#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include "engine/Engine.h"

#define TAG "ForgeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

// Round-trip a jlong through the Engine pointer. The Kotlin side treats the
// handle as opaque; only this file dereferences it.
static Engine* asEngine(jlong handle) {
    return reinterpret_cast<Engine*>(handle);
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeVersion(JNIEnv *env, jclass) {
    LOGI("native layer initialized");
    return env->NewStringUTF("Forge Engine v0.2 | C++17 | OpenGL ES 3.1");
}

JNIEXPORT jlong JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeCreate(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new Engine());
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeDestroy(JNIEnv *, jclass, jlong handle) {
    delete asEngine(handle);
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSurfaceCreated(JNIEnv *env, jclass,
                                                                jlong handle, jobject surface) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    asEngine(handle)->surfaceCreated(window);
    ANativeWindow_release(window);  // EGL holds its own ref; safe to drop ours now.
}

// Must be called after nativeSurfaceCreated — requires EGL context to be current.
// Returns the GL texture ID for Kotlin to wrap in a SurfaceTexture, or 0 on failure
// (0 is GL's reserved invalid-texture sentinel — glGenTextures never produces it).
JNIEXPORT jint JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeCreateOesTexture(JNIEnv *, jclass, jlong handle) {
    return static_cast<jint>(asEngine(handle)->createOesTexture());
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSetViewport(
        JNIEnv *, jclass, jlong handle,
        jint camW, jint camH, jint surfW, jint surfH) {
    asEngine(handle)->setViewport(camW, camH, surfW, surfH);
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeDrawFrame(JNIEnv *env, jclass,
                                                           jlong handle, jfloatArray texMatrix) {
    jfloat *mat = env->GetFloatArrayElements(texMatrix, nullptr);
    asEngine(handle)->drawFrame(mat);
    env->ReleaseFloatArrayElements(texMatrix, mat, JNI_ABORT);  // read-only; skip copy-back.
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeSurfaceDestroyed(JNIEnv *, jclass, jlong handle) {
    asEngine(handle)->surfaceDestroyed();
}

} // extern "C"