#pragma once

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>

namespace forge {

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
