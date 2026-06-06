#pragma once

#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>

#include "../egl/EglContext.h"
#include "../gl/FullScreenQuad.h"
#include "../shader/PassthroughRenderer.h"

// Owns the per-surface GL state (EGL context + renderer) and exposes the
// operations the JNI layer needs. One RenderEngine per camera preview surface;
// future consumers (encoder input surface, inference target) will share the
// same EGL context but render into their own outputs.
//
// Lifecycle precondition: the destructor assumes the caller has already
// invoked surfaceDestroyed() on the GL thread. Without that, the implicit
// unique_ptr cleanup would issue EGL/GL calls on whatever thread `delete`
// lands on, with no current context.
class RenderEngine {
public:
    bool surfaceCreated(ANativeWindow* window);

    GLuint createOesTexture();

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void drawFrame(const float* texMatrix4x4);

    void surfaceDestroyed();

private:
    std::unique_ptr<EglContext> egl_;
    // Shared full-screen geometry, created in createOesTexture and handed to every
    // pass. Held here (not inside a pass) so a single VBO is reused across passes
    // and freed once, on the GL thread, in surfaceDestroyed.
    std::unique_ptr<FullScreenQuad> quad_;
    std::unique_ptr<PassthroughRenderer> renderer_;
};