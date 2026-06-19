#include "RenderEngine.h"

#include "../CheckGl.h"
#include "../shader/EffectPass.h"
#include "../shader/PresentPass.h"

#include <GLES2/gl2ext.h>
#include <string_view>
#include <utility>

#define LOG_TAG "RenderEngine"
#include "../Log.h"

namespace forge {

// First effect occupying the slot between the camera pass and present: a 3x3 Sobel
// edge detector. For each pixel it reads the 8 neighbours (stepping by uTexelSize),
// approximates the luminance gradient with the horizontal/vertical Sobel kernels,
// and outputs the gradient magnitude as white-on-black edges. Chosen as the first
// effect because the result is unmistakable — direct proof the pass is wired into
// the chain — and the neighbour-sampling machinery (uTexelSize, the 3x3 tap
// pattern) is exactly what a Gaussian blur and segmentation-mask feathering reuse.
static constexpr std::string_view kSobelFragSrc = R"GLSL(
    #version 300 es
    precision mediump float;
    in vec2 vTexCoord;
    uniform sampler2D uTexture;
    uniform vec2 uTexelSize;   // size of one texel in UV space (1/width, 1/height)
    out vec4 fragColor;

    // Perceptual luminance of the pixel at uv (Rec. 601 weights).
    float luma(vec2 uv) {
        vec3 c = texture(uTexture, uv).rgb;
        return dot(c, vec3(0.299, 0.587, 0.114));
    }

    void main() {
        vec2 t = uTexelSize;
        // Sample the 3x3 luminance neighbourhood around this pixel.
        float tl = luma(vTexCoord + vec2(-t.x,  t.y));
        float tc = luma(vTexCoord + vec2( 0.0,  t.y));
        float tr = luma(vTexCoord + vec2( t.x,  t.y));
        float ml = luma(vTexCoord + vec2(-t.x,  0.0));
        float mr = luma(vTexCoord + vec2( t.x,  0.0));
        float bl = luma(vTexCoord + vec2(-t.x, -t.y));
        float bc = luma(vTexCoord + vec2( 0.0, -t.y));
        float br = luma(vTexCoord + vec2( t.x, -t.y));
        // Sobel gradient: horizontal (gx) and vertical (gy) convolution kernels.
        float gx = -tl - 2.0 * ml - bl + tr + 2.0 * mr + br;
        float gy =  tl + 2.0 * tc + tr - bl - 2.0 * bc - br;
        // Gradient magnitude is the edge strength, drawn as white edges on black.
        float edge = length(vec2(gx, gy));
        fragColor = vec4(vec3(edge), 1.0);
    }
)GLSL";

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

// Creates the OES texture the camera writes frames into and returns its id so the
// caller can wrap it in a SurfaceTexture. Only the camera *input* — the render
// graph that samples it is built separately in initPipeline(). Returns the id, or
// 0 (GL's reserved invalid-texture sentinel) if the context isn't current.
GLuint RenderEngine::createOesTexture() {
    // glGenTextures reserves an id in the driver. The texture object itself
    // is not allocated until the first glBindTexture call below.
    glGenTextures(1, &oesTexId_);

    // GL_TEXTURE_EXTERNAL_OES is a special texture target that accepts camera
    // and video buffers directly. The driver handles YUV→RGB conversion when
    // sampled — regular sampler2D textures can't do this.
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId_);
    // MIN_FILTER: GL_LINEAR blends the 4 nearest texels when the texture is shrunk.
    // MAG_FILTER: GL_LINEAR blends when the texture is stretched, avoiding blockiness.
    // GL_LINEAR: the correct ceiling hereOES textures don't support mipmaps, so any
    // GL_LINEAR_MIPMAP_* variant would silently produce a black texture.
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // WRAP_S/T (S = horizontal axis, T = vertical axis) control what the GPU samples when a UV
    // coordinate falls outside [0,1].
    // CLAMP_TO_EDGE: pins out-of-range UVs to the nearest edge pixel — any floating-point rounding
    // past 1.0 gets the border pixel rather than wrapping to the opposite edge, which would bleed
    // the wrong side of the frame.
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    // Unbind so subsequent unrelated GL calls don't accidentally mutate this texture.
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

    CHECK_GL("RenderEngine::createOesTexture");
    return oesTexId_;
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
    auto camera = std::make_unique<PassthroughRenderer>();
    if (!camera->init(oesTexId_, quad.get())) {
        LOGE("PassthroughRenderer init failed");
        return false;
    }

    // Effect chain. Each entry samples the previous stage's output; the fragment
    // shader defines the look while EffectPass stays effect-agnostic. Append an
    // effect here to extend the chain — nothing else in the engine changes.
    std::vector<std::unique_ptr<RenderPass>> effects;
    auto sobel = std::make_unique<EffectPass>();
    if (!sobel->init(quad.get(), kSobelFragSrc)) {
        LOGE("EffectPass init failed");
        return false;
    }
    effects.push_back(std::move(sobel));

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

    // Head pass: camera -> pingPong_[0]. The camera pass applies crop and orientation
    // while rendering into the FBO; bind() also sets the viewport to the FBO size.
    pingPong_[0]->bind();
    // Clear so any pixel not covered by the camera quad reads black rather than
    // last-frame garbage (defensive — cover-mode crop fills the target, but a
    // transient mismatch frame on resize can leave gaps).
    glClear(GL_COLOR_BUFFER_BIT);
    camera_->draw(texMatrix4x4);

    // Effect chain: each pass reads the previous target and writes the other. No clear
    // needed — an effect writes every pixel. After the loop, src indexes the target
    // holding the final result.
    int src = 0;
    int dst = 1;
    for (const auto& effect : effects_) {
        pingPong_[dst]->bind();
        effect->draw(pingPong_[src]->textureId());
        std::swap(src, dst);
    }

    // Present: final target -> screen. Bind the default framebuffer (0) and restore
    // the surface viewport, since the FBO binds changed it.
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, surfaceW_, surfaceH_);
    present_->draw(pingPong_[src]->textureId());

    CHECK_GL("RenderEngine::drawFrame");
    // Promote the just-rendered back buffer to the front buffer so the user sees it.
    egl_->swapBuffers();
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
    glDeleteTextures(1, &oesTexId_);
    oesTexId_ = 0;
    egl_.reset();
}

}  // namespace forge