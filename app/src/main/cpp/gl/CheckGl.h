#pragma once

#include <GLES3/gl3.h>
#include <android/log.h>

// CHECK_GL("label") drains the GL error queue and logs any pending errors.
//
// Each glGetError() forces a sync with the GPU command queue, so this expands
// to a no-op in release builds. In debug builds the cost is paid only on call
// sites where errors would otherwise be invisible — typically once per stage
// boundary (after a shader build, after a draw, after a framebuffer setup).
#ifndef NDEBUG
#define CHECK_GL(label) ::forge::checkGlError(label, __FILE__, __LINE__)
#else
#define CHECK_GL(label) ((void)0)
#endif

namespace forge {
inline void checkGlError(const char* label, const char* file, int line) {
    // GL can have multiple errors queued — drain them all so a later check
    // isn't polluted by an earlier one.
    GLenum err;
    while ((err = glGetError()) != GL_NO_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, "GL", "GL error 0x%x at %s (%s:%d)", err, label,
                            file, line);
    }
}
}  // namespace forge