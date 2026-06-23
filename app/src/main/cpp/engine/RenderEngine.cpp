#include "RenderEngine.h"

#include "../effects/GaussianBlur.h"
#include "../gl/CheckGl.h"
#include "../passes/EffectPass.h"
#include "../passes/PresentPass.h"
#include "../trace/Trace.h"

#include <GLES2/gl2ext.h>
#include <initializer_list>
#include <string_view>
#include <utility>

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

// Builds the render graph that turns camera frames into screen pixels: the shared
// full-screen quad, the camera head pass, the effect chain, the present pass, and
// the two ping-pong targets the chain hands off through. Must run after
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

    // Final pass: blits the chain's output to the window with straight UVs.
    auto present = std::make_unique<PresentPass>();
    if (!present->init(quad.get())) {
        LOGE("PresentPass init failed");
        return false;
    }

    // Two offscreen targets the chain ping-pongs between, sized later in setViewport.
    auto ping = std::make_unique<FrameBuffer>();
    auto pong = std::make_unique<FrameBuffer>();

    // Commit: every step succeeded, so move the locals into members in one shot. The
    // passes cached quad.get(), whose pointee address is unchanged by the unique_ptr move.
    quad_        = std::move(quad);
    camera_      = std::move(camera);
    effects_     = std::move(effects);
    present_     = std::move(present);
    pingPong_[0] = std::move(ping);
    pingPong_[1] = std::move(pong);

    CHECK_GL("RenderEngine::initPipeline");
    return true;
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
    // Size both ping-pong targets to the surface so every stage renders at display
    // resolution. ensureSize is a no-op unless the size changed.
    for (auto& fbo : pingPong_) {
        if (fbo) {
            fbo->ensureSize(surfW, surfH);
        }
    }
    // Let each effect react to the new resolution (e.g. recompute its texel size for
    // neighbour sampling — a wrong texel size would offset neighbour reads and smear
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
// Graph: camera OES --[camera]--> pingPong[0] --[effect..]--> pingPong[..] --[present]--> screen.
// The effect chain ping-pongs between the two targets; present blits whichever holds
// the final result. With no effects, present blits the camera output directly.
void RenderEngine::drawFrame(const float* texMatrix4x4) {
    // Defensive guard: a queued onFrameAvailable can fire after surfaceDestroyed
    // has cleared state, or before setViewport has sized the ping-pong targets.
    if (!egl_ || !camera_ || !present_ || !pingPong_[0] || !pingPong_[0]->ready() ||
        !pingPong_[1] || !pingPong_[1]->ready()) {
        return;
    }

    // Whole-frame slice; the stage slices below nest under it in a Perfetto capture.
    FORGE_TRACE("RenderEngine::drawFrame");

    // Head pass: camera -> pingPong_[0]. The camera pass applies crop and orientation
    // while rendering into the FBO; bind() also sets the viewport to the FBO size.
    {
        FORGE_TRACE("CameraPass");
        pingPong_[0]->bind();
        // Clear so any pixel not covered by the camera quad reads black rather than
        // last-frame garbage (defensive — cover-mode crop fills the target, but a
        // transient mismatch frame on resize can leave gaps).
        glClear(GL_COLOR_BUFFER_BIT);
        camera_->draw(texMatrix4x4);
    }

    // Effect chain: each pass reads the previous target and writes the other. No clear
    // needed — an effect writes every pixel. After the loop, src indexes the target
    // holding the final result.
    int src = 0;
    int dst = 1;
    {
        FORGE_TRACE("EffectChain");
        for (const auto& effect : effects_) {
            pingPong_[dst]->bind();
            effect->draw(pingPong_[src]->textureId());
            std::swap(src, dst);
        }
    }

    // Present: final target -> screen. Bind the default framebuffer (0) and restore
    // the surface viewport, since the FBO binds changed it.
    {
        FORGE_TRACE("PresentPass");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, surfaceW_, surfaceH_);
        present_->draw(pingPong_[src]->textureId());
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
    present_.reset();
    effects_.clear();
    camera_.reset();
    pingPong_[0].reset();
    pingPong_[1].reset();
    quad_.reset();
    glDeleteTextures(1, &oesTextureId_);
    oesTextureId_ = 0;
    egl_.reset();
}

}  // namespace forge