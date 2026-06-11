#include "RenderEngine.h"

#include "../CheckGl.h"

#include <GLES2/gl2ext.h>

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
// full-screen quad, the camera (pass 1) and present (pass 2) shader passes, and
// the offscreen target that hands off between them. Must run after
// createOesTexture() — pass 1 samples that texture — and while the EGL context is
// current. On any failure it rolls back the partial state and returns false; the
// OES texture is left untouched because the caller already owns it via its
// SurfaceTexture.
bool RenderEngine::initPipeline() {
    // Upload the shared full-screen quad once; every pass reuses this VBO.
    quad_ = std::make_unique<FullScreenQuad>();
    if (!quad_->init()) {
        LOGE("FullScreenQuad init failed");
        quad_.reset();
        return false;
    }

    // Pass 1 shader: Samples the OES camera texture, applies texMatrix4x4 (sensor orientation +
    // HAL crop) and cover-crop, writes into the bound FBO.
    renderer_ = std::make_unique<PassthroughRenderer>();
    if (!renderer_->init(oesTexId_, quad_.get())) {
        LOGE("PassthroughRenderer init failed");
        renderer_.reset();
        quad_.reset();
        return false;
    }

    // Pass 2 shader: Blits sceneFbo_'s texture to the window surface with straight UVs.
    // Built as the concrete type so we can call init(), then stored through the
    // RenderPass interface — drawFrame only needs the polymorphic draw().
    auto present = std::make_unique<PresentPass>();
    if (!present->init(quad_.get())) {
        LOGE("PresentPass init failed");
        renderer_.reset();
        quad_.reset();
        return false;
    }
    present_ = std::move(present);

    // Offscreen target the camera renders into. It is the handoff between the two passes.
    // renderer_ renders into it; present_ samples from it. The insertion point for any future
    // effect pass.
    sceneFbo_ = std::make_unique<FrameBuffer>();

    CHECK_GL("RenderEngine::initPipeline");
    return true;
}

// Passes camera and surface dimensions to the renderer so it can compute the
// cover-style crop. Null-guarded because setViewport may be called before
// createOesTexture has succeeded, or after surfaceDestroyed has cleared state.
void RenderEngine::setViewport(int camW, int camH, int surfW, int surfH) {
    surfaceW_ = surfW;
    surfaceH_ = surfH;
    // Forward dimensions so the camera pass can recompute the cover-crop matrix —
    // the ratio of camera vs surface may change on rotation or surface resize.
    if (renderer_) {
        renderer_->setViewport(camW, camH, surfW, surfH);
    }
    // Size the offscreen target to match the screen so the scene is rendered at
    // display resolution. ensureSize is a no-op unless the size changed.
    if (sceneFbo_) {
        sceneFbo_->ensureSize(surfW, surfH);
    }
}

// Renders one camera frame through the render graph. Invoked on the GL thread
// after SurfaceTexture's onFrameAvailable callback. texMatrix4x4 is the per-frame
// transform from SurfaceTexture.getTransformMatrix() that compensates for sensor
// orientation and HAL crop — it must be re-read every frame.
//
// Graph: camera OES --[camera pass]--> sceneFbo --[present pass]--> screen.
// The intermediate FBO is the seam where future effect/ML passes will slot in.
void RenderEngine::drawFrame(const float* texMatrix4x4) {
    // Defensive guard: a queued onFrameAvailable can fire after surfaceDestroyed
    // has cleared state, or before setViewport has sized the offscreen target.
    if (!egl_ || !renderer_ || !present_ || !sceneFbo_ || !sceneFbo_->ready()) {
        return;
    }

    // Pass 1: camera -> offscreen scene texture. The camera pass applies crop and
    // orientation while rendering into the FBO. bind() also sets the viewport to
    // the FBO size.
    sceneFbo_->bind();
    // Clear so any pixel not covered by the camera quad reads black rather than
    // last-frame garbage (defensive — cover-mode crop fills the target, but a
    // transient mismatch frame on resize can leave gaps).
    glClear(GL_COLOR_BUFFER_BIT);
    renderer_->draw(texMatrix4x4);

    // Pass 2: scene texture -> screen. Bind the default framebuffer (0) and
    // restore the surface viewport, since the FBO bind changed it.
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, surfaceW_, surfaceH_);
    present_->draw(sceneFbo_->textureId());

    CHECK_GL("RenderEngine::drawFrame");
    // Promote the just-rendered back buffer to the front buffer so the user sees it.
    egl_->swapBuffers();
}

// Releases per-surface GL state in reverse order of acquisition. Must run on
// the GL thread while the EGL context is still current — the renderer's
// destructor frees GPU objects (program, VBO) that require a current context.
// Tearing down EGL first would orphan those objects in the driver.
void RenderEngine::surfaceDestroyed() {
    present_.reset();
    sceneFbo_.reset();
    renderer_.reset();
    quad_.reset();
    glDeleteTextures(1, &oesTexId_);
    oesTexId_ = 0;
    egl_.reset();
}

}  // namespace forge