#pragma once

#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>

#include "../egl/EglContext.h"
#include "../shader/PassthroughRenderer.h"

// Owns the per-surface GL state (EGL context + renderer) and exposes the
// operations the JNI layer needs. One Engine per camera preview surface;
// future consumers (encoder input surface, inference target) will share the
// same EGL context but render into their own outputs.
class Engine {
public:
    bool surfaceCreated(ANativeWindow* window);

    GLuint createOesTexture();

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void drawFrame(const float* texMatrix4x4);

    void surfaceDestroyed();

private:
    std::unique_ptr<EglContext> egl_;
    std::unique_ptr<PassthroughRenderer> renderer_;
};