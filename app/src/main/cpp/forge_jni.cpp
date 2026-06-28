#include "engine/RenderEngine.h"

#include <android/native_window_jni.h>
#include <jni.h>

#define LOG_TAG "Forge"
#include "Log.h"

using forge::RenderEngine;

/*
 * JNI bridge between the Kotlin RenderEngine wrapper and the C++ RenderEngine. Each
 * native* function here backs one `external fun` on the Kotlin side. The engine is
 * held as an opaque jlong handle (a raw RenderEngine* round-tripped through a long),
 * created by nativeCreate and freed by nativeDestroy. Every call must arrive on the
 * GL thread — see RenderEngine for the lifecycle and call ordering.
 */

// Round-trip a jlong back into the RenderEngine pointer. The Kotlin side treats the
// handle as opaque; only this file dereferences it.
static RenderEngine *asRenderEngine(jlong handle) {
    return reinterpret_cast<RenderEngine *>(handle);
}

extern "C" {

JNIEXPORT jstring JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeVersion(JNIEnv *env,
                                                                                    jclass) {
    LOGI("native layer initialized");
    return env->NewStringUTF("Forge Engine v0.2 | C++20 | OpenGL ES 3.1");
}

JNIEXPORT jlong JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeCreate(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new RenderEngine());
}

JNIEXPORT void JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeDestroy(JNIEnv *, jclass,
                                                                                 jlong handle) {
    delete asRenderEngine(handle);
}

JNIEXPORT void JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeSurfaceCreated(
    JNIEnv *env, jclass, jlong handle, jobject surface) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    asRenderEngine(handle)->surfaceCreated(window);
    ANativeWindow_release(window);  // EGL holds its own ref; safe to drop ours now.
}

// Must be called after nativeSurfaceCreated — requires EGL context to be current.
// Returns the GL texture ID for Kotlin to wrap in a SurfaceTexture, or 0 on failure
// (0 is GL's reserved invalid-texture sentinel — glGenTextures never produces it).
JNIEXPORT jint JNICALL
Java_app_honguyen_forge_engine_RenderEngine_nativeCreateOesTexture(JNIEnv *, jclass, jlong handle) {
    return static_cast<jint>(asRenderEngine(handle)->createOesTexture());
}

// Must be called after nativeCreateOesTexture — builds the render graph that
// samples the camera texture. Returns JNI_TRUE on success, JNI_FALSE on failure.
JNIEXPORT jboolean JNICALL
Java_app_honguyen_forge_engine_RenderEngine_nativeInitPipeline(JNIEnv *, jclass, jlong handle) {
    return asRenderEngine(handle)->initPipeline() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeSetViewport(
    JNIEnv *, jclass, jlong handle, jint camW, jint camH, jint surfW, jint surfH) {
    asRenderEngine(handle)->setViewport(camW, camH, surfW, surfH);
}

JNIEXPORT void JNICALL Java_app_honguyen_forge_engine_RenderEngine_nativeDrawFrame(
    JNIEnv *env, jclass, jlong handle, jfloatArray texMatrix) {
    jfloat *mat = env->GetFloatArrayElements(texMatrix, nullptr);
    asRenderEngine(handle)->drawFrame(mat);
    env->ReleaseFloatArrayElements(texMatrix, mat, JNI_ABORT);  // read-only; skip copy-back.
}

JNIEXPORT void JNICALL
Java_app_honguyen_forge_engine_RenderEngine_nativeSurfaceDestroyed(JNIEnv *, jclass, jlong handle) {
    asRenderEngine(handle)->surfaceDestroyed();
}

}  // extern "C"