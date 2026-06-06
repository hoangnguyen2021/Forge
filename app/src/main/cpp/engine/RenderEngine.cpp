#include "RenderEngine.h"

#include <GLES2/gl2ext.h>

#include "../CheckGl.h"

#define LOG_TAG "RenderEngine"
#include "../Log.h"

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

// Allocates the GL texture the camera will write frames into, plus the renderer
// that samples from it. Returns the texture id so Kotlin can wrap it in a
// SurfaceTexture, or 0 (GL's reserved invalid-texture sentinel) on failure.
GLuint RenderEngine::createOesTexture() {
    GLuint texId = 0;
    // glGenTextures reserves an id in the driver. The texture object itself
    // is not allocated until the first glBindTexture call below.
    glGenTextures(1, &texId);

    // GL_TEXTURE_EXTERNAL_OES is a special texture target that accepts camera
    // and video buffers directly. The driver handles YUV→RGB conversion when
    // sampled — regular sampler2D textures can't do this.
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
    // Linear filtering for smooth scaling. OES textures don't support mipmaps,
    // so MIN_FILTER must be GL_LINEAR (not any GL_LINEAR_MIPMAP_* variant).
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // CLAMP_TO_EDGE prevents wrap-around artifacts at frame borders; the
    // camera frame's UVs map exactly to 0..1, so no repeat is desired.
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    // Unbind so subsequent unrelated GL calls don't accidentally mutate this texture.
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

    // The renderer caches texId internally and samples it on every draw call.
    renderer_ = std::make_unique<PassthroughRenderer>();
    if (!renderer_->init(texId)) {
        LOGE("PassthroughRenderer init failed");
        renderer_.reset();
        // Don't orphan the GL texture if the renderer that was supposed to
        // own it can't be constructed.
        glDeleteTextures(1, &texId);
        return 0;
    }
    CHECK_GL("RenderEngine::createOesTexture");
    return texId;
}

// Passes camera and surface dimensions to the renderer so it can compute the
// cover-style crop. Null-guarded because setViewport may be called before
// createOesTexture has succeeded, or after surfaceDestroyed has cleared state.
void RenderEngine::setViewport(int camW, int camH, int surfW, int surfH) {
    if (renderer_) {
        renderer_->setViewport(camW, camH, surfW, surfH);
    }
}

// Renders one camera frame to the EGL window surface. Invoked on the GL thread
// after SurfaceTexture's onFrameAvailable callback. texMatrix4x4 is the per-frame
// transform from SurfaceTexture.getTransformMatrix() that compensates for sensor
// orientation and HAL crop — it must be re-read every frame.
void RenderEngine::drawFrame(const float* texMatrix4x4) {
    // Defensive guard: a queued onFrameAvailable can fire after surfaceDestroyed
    // has cleared egl_/renderer_; we ignore those late frames silently.
    if (!egl_ || !renderer_) {
        return;
    }
    // Clear the back buffer so any pixel not covered by the camera quad reads
    // black rather than last-frame garbage (mostly defensive — cover-mode crop
    // fills the screen, but a transient mismatch frame on resize can leave gaps).
    glClear(GL_COLOR_BUFFER_BIT);
    renderer_->draw(texMatrix4x4);
    CHECK_GL("RenderEngine::drawFrame");
    // Promote the just-rendered back buffer to the front buffer so the user sees it.
    egl_->swapBuffers();
}

// Releases per-surface GL state in reverse order of acquisition. Must run on
// the GL thread while the EGL context is still current — the renderer's
// destructor frees GPU objects (program, VBO) that require a current context.
// Tearing down EGL first would orphan those objects in the driver.
void RenderEngine::surfaceDestroyed() {
    renderer_.reset();
    egl_.reset();
}