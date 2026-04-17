#include <jni.h>
#include <android/log.h>
#include <string>

#define TAG "ForgeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_app_honguyen_forge_engine_ForgeEngine_nativeVersion(JNIEnv *env, jobject /* thiz */) {
    LOGI("native layer initialized");
    return env->NewStringUTF("Forge Engine v0.1 | C++17 | NDK");
}

} // extern "C"
