#include "RenderEngine.h"

#include "../effects/GaussianBlur.h"
#include "../gl/CheckGl.h"
#include "../ml/Segmenter.h"
#include "../passes/CompositePass.h"
#include "../passes/EffectPass.h"
#include "../passes/PresentPass.h"
#include "../trace/Trace.h"

#include <GLES2/gl2ext.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <initializer_list>
#include <string_view>
#include <utility>
#include <vector>

#define LOG_TAG "RenderEngine"
#include "../Log.h"

namespace forge {

// Creates the EGL context bound to the given Android window. Must run on the
// thread that will later issue draw calls — EGL contexts are thread-local and
// every GL call is implicitly dispatched to the context current on that thread.
bool RenderEngine::surfaceCreated(ANativeWindow* window) {
    egl_ = std::make_unique<EglContext>();
    if (!egl_->init(window)) {
        LOGE("EGL init failed");
        // Reset rather than leave a partially-initialized EglContext alive —
        // its destructor would otherwise call destroy() on uninitialized handles.
        egl_.reset();
        return false;
    }
    return true;
}

// Creates the GL texture the camera writes its frames into and returns its id so
// the Kotlin side can wrap it in a SurfaceTexture. This is only the camera *input*;
// the render graph that samples it is built separately in initPipeline(). Returns
// the id, or 0 (GL's reserved invalid-texture sentinel) if the context isn't current.
//
// Canonical home for GL textures and their sampling parameters — other passes and
// FrameBuffer reuse these ideas and point back here.
GLuint RenderEngine::createOesTexture() {
    // A GL "texture" is just image memory on the GPU that a shader can read; this one
    // holds the live camera frame. glGenTextures reserves an id, but the object isn't
    // actually allocated until the first glBindTexture below.
    glGenTextures(1, &oesTextureId_);

    // Binding makes this the texture that the following texture calls configure. The
    // target GL_TEXTURE_EXTERNAL_OES is Android-specific: it accepts the camera's
    // native buffer directly and converts YUV->RGB when sampled, which a normal
    // sampler2D cannot do (the camera delivers YUV; shaders want RGB).
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureId_);
    // Filtering controls how the texture is sampled when it doesn't map 1:1 to screen
    // pixels. GL_LINEAR blends the nearest texels — MIN when the image is shrunk, MAG
    // when it's stretched — which is smoother than the blocky GL_NEAREST. OES textures
    // can't have mipmaps, so the GL_LINEAR_MIPMAP_* variants aren't an option here.
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // Wrapping controls what's sampled when a UV coordinate falls outside [0,1]
    // (S = horizontal axis, T = vertical). GL_CLAMP_TO_EDGE pins it to the nearest
    // edge texel, so float rounding past 1.0 reads the border rather than wrapping
    // around and bleeding in the opposite edge of the frame.
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    // Unbind so later, unrelated texture calls don't accidentally reconfigure this one.
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

    CHECK_GL("RenderEngine::createOesTexture");
    return oesTextureId_;
}

// Creates the segmentation mask texture the composite samples (.r): a kSize x kSize
// single-channel R8 texture, initialized to all-foreground (255). That cold-start value
// means the composite keeps the frame fully sharp (no blur) before the first real mask
// arrives, or if segmentation is never enabled. The Segmenter overwrites it via
// glTexSubImage2D in drawFrame as each new mask lands. GL_LINEAR upsamples the low-res
// mask smoothly to full frame resolution.
GLuint RenderEngine::createMaskTexture() {
    constexpr int kSize = Segmenter::kSize;
    std::vector<uint8_t> pixels(static_cast<size_t>(kSize) * kSize, 255);  // all foreground

    glGenTextures(1, &maskTextureId_);
    glBindTexture(GL_TEXTURE_2D, maskTextureId_);
    // R8: one 8-bit channel is all a coverage mask needs, matching the model's single
    // foreground probability. (kSize-byte rows are 4-aligned, so the default
    // GL_UNPACK_ALIGNMENT is fine.)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, kSize, kSize, 0, GL_RED, GL_UNSIGNED_BYTE, pixels.data());
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);

    CHECK_GL("RenderEngine::createMaskTexture");
    return maskTextureId_;
}

// Builds the render graph that turns camera frames into screen pixels: the shared
// full-screen quad, the camera head pass, the blur chain, the composite pass, the
// sharp target, the two blur ping-pong targets, and the placeholder mask. Must run after
// createOesTexture() — the camera pass samples that texture — and while the EGL
// context is current. Each stage is built into a local first and only committed to
// members once every step has succeeded; a partial failure returns false and the
// locals free their GL objects via RAII on this (the GL) thread, leaving the engine
// untouched. The OES texture is left alone — the caller owns it via its SurfaceTexture.
bool RenderEngine::initPipeline() {
    // Shared full-screen quad; every pass reuses this one VBO.
    auto quad = std::make_unique<FullScreenQuad>();
    if (!quad->init()) {
        LOGE("FullScreenQuad init failed");
        return false;
    }

    // Head pass: samples the OES camera texture, applies texMatrix4x4 (sensor
    // orientation + HAL crop) and cover-crop, renders into the first ping-pong target.
    auto camera = std::make_unique<CameraPass>();
    if (!camera->init(oesTextureId_, quad.get())) {
        LOGE("CameraPass init failed");
        return false;
    }

    // Effect chain: a separable Gaussian blur — a horizontal 1D pass followed by a
    // vertical one (see kBlurHFragSrc). Each EffectPass is effect-agnostic; its
    // fragment shader defines the look, so wiring a new effect is just another entry
    // here — the draw loop ping-pongs them through the two targets automatically.
    std::vector<std::unique_ptr<RenderPass>> effects;
    for (std::string_view fragSrc : {kBlurHFragSrc, kBlurVFragSrc}) {
        auto effect = std::make_unique<EffectPass>();
        if (!effect->init(quad.get(), fragSrc)) {
            LOGE("EffectPass init failed");
            return false;
        }
        effects.push_back(std::move(effect));
    }

    // Terminal pass: composites the sharp frame, its blurred copy, and the mask
    // (mix(blurred, sharp, mask)) straight to the window.
    auto composite = std::make_unique<CompositePass>();
    if (!composite->init(quad.get())) {
        LOGE("CompositePass init failed");
        return false;
    }

    // Three offscreen targets, sized later in setViewport: sharp_ holds the untouched
    // camera frame (kept alive so the composite can read it), and the two ping-pong
    // targets carry the blur.
    auto sharp = std::make_unique<FrameBuffer>();
    auto ping  = std::make_unique<FrameBuffer>();
    auto pong  = std::make_unique<FrameBuffer>();

    // The mask texture the composite samples; starts all-foreground and is overwritten by
    // the segmenter when enabled. Writes maskTextureId_ directly — safe because no
    // fallible step follows it.
    createMaskTexture();

    // Commit: every step succeeded, so move the locals into members in one shot. The
    // passes cached quad.get(), whose pointee address is unchanged by the unique_ptr move.
    quad_        = std::move(quad);
    camera_      = std::move(camera);
    effects_     = std::move(effects);
    composite_   = std::move(composite);
    sharp_       = std::move(sharp);
    pingPong_[0] = std::move(ping);
    pingPong_[1] = std::move(pong);

    // Optional: enable per-pass GPU timing if the device supports the extension. A failure
    // here just leaves timing disabled — it must not fail the pipeline.
    gpuTimer_.init();

    CHECK_GL("RenderEngine::initPipeline");
    return true;
}

// Starts the background-person segmentation pipeline: a fixed-size downscale target and
// pass (so inference runs at model resolution, not full screen) plus a worker thread that
// runs the model from assets. Called on the GL thread after initPipeline. On any failure
// it logs and returns with segmenter_ left null — drawFrame then skips segmentation and
// the composite keeps the cold-start all-foreground mask (frame stays fully sharp).
void RenderEngine::enableSegmentation(AAssetManager* assets) {
    // A passthrough pass that samples sharp_ into the small segInput_ target; GL_LINEAR on
    // the source FBO makes this a bilinear downscale to model resolution.
    auto downscale = std::make_unique<PresentPass>();
    if (!downscale->init(quad_.get())) {
        LOGE("segmentation downscale pass init failed");
        return;
    }
    auto segInput = std::make_unique<FrameBuffer>();
    if (!segInput->ensureSize(Segmenter::kSize, Segmenter::kSize)) {
        LOGE("segmentation input FBO incomplete");
        return;
    }
    // Created last because it starts the worker thread; on the earlier failures above we
    // never spin one up.
    auto segmenter = std::make_unique<Segmenter>();
    if (!segmenter->init(assets, "selfie_multiclass_256x256.tflite")) {
        LOGE("segmenter init failed");
        return;
    }

    constexpr size_t kPixels = static_cast<size_t>(Segmenter::kSize) * Segmenter::kSize;
    readbackRgba_.assign(kPixels * 4, 0);
    maskBuf_.assign(kPixels, 0);

    segDownscale_ = std::move(downscale);
    segInput_     = std::move(segInput);
    segmenter_    = std::move(segmenter);

    CHECK_GL("RenderEngine::enableSegmentation");
}

// Passes camera and surface dimensions down the graph so each stage can resize.
// Null-guarded because setViewport may be called before initPipeline has built the
// graph, or after surfaceDestroyed has cleared it.
void RenderEngine::setViewport(int camW, int camH, int surfW, int surfH) {
    surfaceW_ = surfW;
    surfaceH_ = surfH;
    // Forward dimensions so the camera pass can recompute the cover-crop matrix —
    // the ratio of camera vs surface may change on a surface resize (split-screen,
    // foldable). The preview is portrait-locked, so device rotation never triggers this.
    if (camera_) {
        camera_->setViewport(camW, camH, surfW, surfH);
    }
    // Size the sharp target and both ping-pong targets to the surface so every stage
    // renders at display resolution. ensureSize is a no-op unless the size changed.
    if (sharp_) {
        sharp_->ensureSize(surfW, surfH);
    }
    for (auto& fbo : pingPong_) {
        if (fbo) {
            fbo->ensureSize(surfW, surfH);
        }
    }
    // Let each effect react to the new resolution (e.g. recompute its texel size for
    // neighbor sampling — a wrong texel size would offset neighbor reads and smear
    // the result). Passes that don't care inherit RenderPass's no-op onViewport.
    for (const auto& effect : effects_) {
        effect->onViewport(surfW, surfH);
    }
}

// Renders one camera frame through the render graph. Invoked on the GL thread
// after SurfaceTexture's onFrameAvailable callback. texMatrix4x4 is the per-frame
// transform from SurfaceTexture.getTransformMatrix() that compensates for sensor
// orientation and HAL crop — it must be re-read every frame.
//
// Graph: camera OES --[camera]--> sharp --[blur..]--> pingPong = blurred, then
// [composite] mixes sharp + blurred + mask --> screen. The blur chain reads sharp (not
// a ping-pong target), so the sharp frame survives for the composite.
void RenderEngine::drawFrame(const float* texMatrix4x4) {
    // Defensive guard: a queued onFrameAvailable can fire after surfaceDestroyed
    // has cleared state, or before setViewport has sized the offscreen targets.
    if (!egl_ || !camera_ || !composite_ || !sharp_ || !sharp_->ready() || !pingPong_[0] ||
        !pingPong_[0]->ready() || !pingPong_[1] || !pingPong_[1]->ready()) {
        return;
    }

    // Whole-frame slice; the stage slices below nest under it in a Perfetto capture.
    FORGE_TRACE("RenderEngine::drawFrame");
    // Harvest GPU timings issued a few frames ago. Each GpuZone below adds the real GPU
    // cost of its pass — invisible to the CPU slices, which only measure command issue.
    gpuTimer_.beginFrame();

    // Head pass: camera -> sharp_. The sharp frame is kept alive for the whole frame so
    // the composite can read the crisp original after the blur chain has run. The camera
    // pass applies crop and orientation while rendering; bind() sets the viewport too.
    {
        FORGE_TRACE("CameraPass");
        GpuZone gpu(gpuTimer_, GpuTimer::Zone::Camera);
        sharp_->bind();
        // Clear so any pixel not covered by the camera quad reads black rather than
        // last-frame garbage (defensive — cover-mode crop fills the target, but a
        // transient mismatch frame on resize can leave gaps).
        glClear(GL_COLOR_BUFFER_BIT);
        camera_->draw(texMatrix4x4);
    }

    // Segmentation (optional): when the worker is idle, downscale the sharp frame to model
    // resolution, read it back, and hand it off; then upload any finished mask into
    // maskTextureId_ for the composite. Inference runs on the worker thread, so the mask
    // lags a few frames — fine for a soft blur. wantsFrame() gates the readback so we only
    // pay the GPU->CPU sync when the worker can actually consume a frame.
    if (segmenter_) {
        FORGE_TRACE("Segmentation");
        if (segmenter_->wantsFrame()) {
            segInput_->bind();  // kSize x kSize target; bind() also sets the viewport
            segDownscale_->draw(sharp_->textureId());  // GL_LINEAR bilinear downscale
            glReadPixels(0, 0, Segmenter::kSize, Segmenter::kSize, GL_RGBA, GL_UNSIGNED_BYTE,
                         readbackRgba_.data());
            segmenter_->submit(readbackRgba_.data());
        }
        if (segmenter_->fetchMask(maskBuf_.data())) {
            glBindTexture(GL_TEXTURE_2D, maskTextureId_);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, Segmenter::kSize, Segmenter::kSize, GL_RED,
                            GL_UNSIGNED_BYTE, maskBuf_.data());
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    // Blur chain: blur a COPY of the sharp frame, ping-ponging through the two targets.
    // The chain's input is sharp_ (not a ping-pong target), so the sharp frame survives;
    // blurInput tracks the last target written = the blurred result. No clear needed —
    // an effect writes every pixel.
    const FrameBuffer* blurInput = sharp_.get();
    int dst = 0;
    {
        FORGE_TRACE("EffectChain");
        GpuZone gpu(gpuTimer_, GpuTimer::Zone::Effects);
        for (const auto& effect : effects_) {
            pingPong_[dst]->bind();
            effect->draw(blurInput->textureId());
            blurInput = pingPong_[dst].get();
            dst ^= 1;
        }
    }

    // Composite -> screen: mix(blurred, sharp, mask). Terminal pass, so bind the default
    // framebuffer (0) and restore the surface viewport, since the FBO binds changed it.
    {
        FORGE_TRACE("CompositePass");
        GpuZone gpu(gpuTimer_, GpuTimer::Zone::Composite);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, surfaceW_, surfaceH_);
        composite_->draw(sharp_->textureId(), blurInput->textureId(), maskTextureId_);
    }

    // Periodically surface the GPU-side cost of each pass (the numbers CPU tracing can't
    // see). Throttled so it doesn't spam Logcat at frame rate.
    if (gpuTimer_.available() && ++gpuLogFrame_ % 120 == 0) {
        LOGI("GPU ms: camera=%.3f effects=%.3f composite=%.3f", gpuTimer_.lastMs(GpuTimer::Zone::Camera),
             gpuTimer_.lastMs(GpuTimer::Zone::Effects), gpuTimer_.lastMs(GpuTimer::Zone::Composite));
    }

    CHECK_GL("RenderEngine::drawFrame");
    // Promote the just-rendered back buffer to the front buffer so the user sees it.
    // This is where the GL thread typically blocks waiting on the GPU and vsync, so
    // its slice usually dominates a CPU-side trace.
    {
        FORGE_TRACE("swapBuffers");
        egl_->swapBuffers();
    }
}

// Releases per-surface GL state in reverse order of acquisition. Must run on
// the GL thread while the EGL context is still current — the passes' destructors
// free GPU objects (programs, VBO, FBOs) that require a current context. Tearing
// down EGL first would orphan those objects in the driver.
void RenderEngine::surfaceDestroyed() {
    gpuTimer_.destroy();
    segmenter_.reset();  // joins the worker thread (no GL); do before GL teardown
    composite_.reset();
    effects_.clear();
    camera_.reset();
    segDownscale_.reset();
    segInput_.reset();
    sharp_.reset();
    pingPong_[0].reset();
    pingPong_[1].reset();
    quad_.reset();
    glDeleteTextures(1, &maskTextureId_);
    maskTextureId_ = 0;
    glDeleteTextures(1, &oesTextureId_);
    oesTextureId_ = 0;
    egl_.reset();
}

}  // namespace forge