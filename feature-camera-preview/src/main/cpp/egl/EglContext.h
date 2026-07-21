#pragma once

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>

namespace forge {

/*
 * Owns the EGL objects that connect OpenGL ES to one Android window: the display
 * (GPU connection), the context (all GL state), and the window surface (the
 * on-screen canvas). EGL is the glue between Android's window system and GL; init()
 * documents each step. A context is thread-local, so this must be created and used
 * on the GL thread. One per RenderEngine.
 */
class EglContext {
public:
    EglContext() = default;
    ~EglContext() { destroy(); }

    EglContext(const EglContext&)            = delete;
    EglContext& operator=(const EglContext&) = delete;

    bool init(ANativeWindow* window);

    void swapBuffers();

    void destroy();

private:
    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLContext context_ = EGL_NO_CONTEXT;
    EGLSurface surface_ = EGL_NO_SURFACE;
};

}  // namespace forge
