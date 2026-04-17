#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <GLES3/gl3.h>
#include <memory>
#include <string>
#include "egl/EglContext.h"

#define TAG "ForgeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<EglContext> gEglContext;

extern "C" {

JNIEXPORT jstring JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeVersion(JNIEnv* env, jobject) {
    LOGI("native layer initialized");
    return env->NewStringUTF("Forge Engine v0.1 | C++17 | OpenGL ES 3.1");
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSurfaceCreated(JNIEnv* env, jobject, jobject surface) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    gEglContext = std::make_unique<EglContext>();
    if (!gEglContext->init(window)) {
        LOGE("EGL init failed");
        gEglContext.reset();
    }
    ANativeWindow_release(window);
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeDrawFrame(JNIEnv*, jobject) {
    if (!gEglContext) return;
    glClearColor(0.05f, 0.05f, 0.12f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    gEglContext->swapBuffers();
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSurfaceDestroyed(JNIEnv*, jobject) {
    gEglContext.reset();
    LOGI("EGL context destroyed");
}

} // extern "C"
