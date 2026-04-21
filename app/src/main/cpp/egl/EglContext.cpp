#include "EglContext.h"
#include <android/log.h>

#define TAG "EglContext"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

bool EglContext::init(ANativeWindow *window) {
    // Open a connection to the GPU driver; EGL_DEFAULT_DISPLAY means "whatever GPU this device has."
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }
    // Boot up the driver connection.
    if (eglInitialize(display_, nullptr, nullptr) == EGL_FALSE) {
        LOGE("eglInitialize failed: 0x%x", eglGetError());
        return false;
    }

    // Ask EGL for a pixel format that supports ES3, RGBA 8-bit, and window rendering.
    const EGLint configAttribs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_NONE
    };
    EGLConfig config;
    EGLint numConfigs = 0;
    if (eglChooseConfig(display_, configAttribs, &config, 1, &numConfigs) == EGL_FALSE ||
        numConfigs == 0) {
        LOGE("eglChooseConfig failed: 0x%x", eglGetError());
        return false;
    }

    // Create the OpenGL ES 3 state machine — holds shaders, textures, and bound buffers.
    const EGLint contextAttribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL_NONE
    };
    context_ = eglCreateContext(display_, config, EGL_NO_CONTEXT, contextAttribs);
    if (context_ == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed: 0x%x", eglGetError());
        return false;
    }

    // Wrap the ANativeWindow into an EGL surface — the actual drawable target on screen.
    // EGL addref's the window internally; caller may release their ref after this.
    surface_ = eglCreateWindowSurface(display_, config, window, nullptr);
    if (surface_ == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed: 0x%x", eglGetError());
        return false;
    }

    // Bind context + surface to this thread; all subsequent GL calls go to this GPU context.
    if (eglMakeCurrent(display_, surface_, surface_, context_) == EGL_FALSE) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        return false;
    }

    LOGI("EGL initialized — OpenGL ES %s", glGetString(GL_VERSION));
    return true;
}

void EglContext::swapBuffers() {
    // Flip the back buffer to screen — called at the end of each render iteration.
    eglSwapBuffers(display_, surface_);
}

void EglContext::destroy() {
    // Tear down in reverse order to avoid dangling GPU references.
    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);
        }
        if (surface_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, surface_);
        }
        eglTerminate(display_);
    }
    display_ = EGL_NO_DISPLAY;
    context_ = EGL_NO_CONTEXT;
    surface_ = EGL_NO_SURFACE;
}
