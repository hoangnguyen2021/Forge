#include "EglContext.h"

#define LOG_TAG "EglContext"
#include "../Log.h"

bool EglContext::init(ANativeWindow *window) {
    // EGL is the glue layer between Android's window system and OpenGL ES.
    // Think of it as the "setup" API you call before you can issue any draw commands.
    // Step 1: get a handle to the GPU driver. EGL_DEFAULT_DISPLAY means "whatever GPU this device has."
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }
    // Step 2: actually initialize the driver connection (like opening a socket before sending data).
    if (eglInitialize(display_, nullptr, nullptr) == EGL_FALSE) {
        LOGE("eglInitialize failed: 0x%x", eglGetError());
        return false;
    }

    // Step 3: choose a pixel format ("config"). We describe what we need and EGL picks the best match.
    // EGL_OPENGL_ES3_BIT — we want OpenGL ES 3 (not ES 2 or desktop GL).
    // EGL_WINDOW_BIT     — the surface will be an actual on-screen window, not an off-screen buffer.
    // RED/GREEN/BLUE/ALPHA 8 — standard 32-bit RGBA color; 8 bits per channel = 256 values each.
    const EGLint configAttribs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_NONE  // sentinel: tells EGL the attribute list is done
    };
    EGLConfig config;
    EGLint numConfigs = 0;
    if (eglChooseConfig(display_, configAttribs, &config, 1, &numConfigs) == EGL_FALSE ||
        numConfigs == 0) {
        LOGE("eglChooseConfig failed: 0x%x", eglGetError());
        return false;
    }

    // Step 4: create a context — the GL "session" that owns all GPU state (shaders, textures,
    // bound buffers, blend modes, etc.). Every GL call implicitly reads/writes this state.
    // EGL_NO_CONTEXT means we're not sharing resources with another context.
    const EGLint contextAttribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 3,  // request OpenGL ES 3
            EGL_NONE
    };
    context_ = eglCreateContext(display_, config, EGL_NO_CONTEXT, contextAttribs);
    if (context_ == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed: 0x%x", eglGetError());
        return false;
    }

    // Step 5: create a surface — the drawable canvas backed by the Android window.
    // Frames you render go here; EGL holds its own reference to the window internally.
    surface_ = eglCreateWindowSurface(display_, config, window, nullptr);
    if (surface_ == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed: 0x%x", eglGetError());
        return false;
    }

    // Step 6: make context + surface "current" on this thread.
    // OpenGL ES is thread-local: every GL call is implicitly sent to whichever context is
    // current on the calling thread. Nothing works until this binding is in place.
    if (eglMakeCurrent(display_, surface_, surface_, context_) == EGL_FALSE) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        return false;
    }

    LOGI("EGL initialized — OpenGL ES %s", glGetString(GL_VERSION));
    return true;
}

void EglContext::swapBuffers() {
    // GL uses double-buffering: you draw into a hidden "back buffer" while the screen shows the
    // "front buffer". Swap makes the finished back buffer visible and gives you a fresh one to draw
    // into next frame. This prevents the user from seeing a half-drawn frame (tearing).
    eglSwapBuffers(display_, surface_);
}

void EglContext::destroy() {
    // Teardown mirrors init in reverse order so nothing is freed while something else still
    // holds a reference to it.
    if (display_ != EGL_NO_DISPLAY) {
        // Unbind first: detaching the context from the thread before destroying it prevents the
        // driver from seeing GL calls on a context that no longer exists.
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);  // free GPU-side shader/texture state
        }
        if (surface_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, surface_);  // release the drawable canvas
        }
        eglTerminate(display_);  // close the driver connection opened by eglInitialize
    }
    // Reset handles to sentinel values so any accidental use-after-free is caught early.
    display_ = EGL_NO_DISPLAY;
    context_ = EGL_NO_CONTEXT;
    surface_ = EGL_NO_SURFACE;
}
