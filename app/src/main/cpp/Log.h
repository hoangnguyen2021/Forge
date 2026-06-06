#pragma once

#include <android/log.h>

// Per-file logging helpers. Each .cpp defines its own tag before including its
// other headers, then uses LOGI/LOGE:
//
//     #define LOG_TAG "MyComponent"
//     #include "Log.h"
//     ...
//     LOGI("started");
//
// Centralizing this avoids re-declaring the same three-line macro block in every
// translation unit. LOG_TAG must be defined before this header is included.
#ifndef LOG_TAG
#error "Define LOG_TAG before including Log.h"
#endif

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)