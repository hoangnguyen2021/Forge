#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <memory>
#include <string>
#include "egl/EglContext.h"
#include "shader/PassthroughRenderer.h"

#define TAG "ForgeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<EglContext>          gEglContext;
static std::unique_ptr<PassthroughRenderer> gRenderer;

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

// Must be called after nativeSurfaceCreated — requires EGL context to be current.
// Returns the GL texture ID for Kotlin to wrap in a SurfaceTexture, or -1 on failure.
JNIEXPORT jint JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeCreateOesTexture(JNIEnv*, jobject) {
    GLuint texId = 0;
    glGenTextures(1, &texId);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

    gRenderer = std::make_unique<PassthroughRenderer>();
    if (!gRenderer->init(texId)) {
        LOGE("PassthroughRenderer init failed");
        gRenderer.reset();
        glDeleteTextures(1, &texId);
        return -1;
    }

    LOGI("OES texture created: id=%u", texId);
    return static_cast<jint>(texId);
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeDrawFrame(JNIEnv* env, jobject, jfloatArray texMatrix) {
    if (!gEglContext || !gRenderer) return;
    jfloat* mat = env->GetFloatArrayElements(texMatrix, nullptr);
    glClear(GL_COLOR_BUFFER_BIT);
    gRenderer->draw(mat);
    env->ReleaseFloatArrayElements(texMatrix, mat, JNI_ABORT);
    gEglContext->swapBuffers();
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSetViewport(
        JNIEnv*, jobject, jint camW, jint camH, jint surfW, jint surfH) {
    if (gRenderer) gRenderer->setViewport(camW, camH, surfW, surfH);
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSurfaceDestroyed(JNIEnv*, jobject) {
    gRenderer.reset();
    gEglContext.reset();
    LOGI("EGL context destroyed");
}

} // extern "C"
