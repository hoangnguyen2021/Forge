#pragma once

#include "../egl/EglContext.h"
#include "../resource/FrameBuffer.h"
#include "../resource/FullScreenQuad.h"
#include "../shader/PassthroughRenderer.h"
#include "../shader/PresentPass.h"
#include "../shader/RenderPass.h"

#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>

namespace forge {

/*
 * Owns the per-surface GL state (EGL context + renderer) and exposes the
 * operations the JNI layer needs. One RenderEngine per camera preview surface;
 * future consumers (encoder input surface, inference target) will share the
 * same EGL context but render into their own outputs.
 *
 * Lifecycle precondition: the destructor assumes the caller has already
 * invoked surfaceDestroyed() on the GL thread. Without that, the implicit
 * unique_ptr cleanup would issue EGL/GL calls on whatever thread `delete`
 * lands on, with no current context.
 */
class RenderEngine {
public:
    bool surfaceCreated(ANativeWindow* window);

    GLuint createOesTexture();

    bool initPipeline();

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void drawFrame(const float* texMatrix4x4);

    void surfaceDestroyed();

private:
    std::unique_ptr<EglContext> egl_;
    // OES texture id
    GLuint oesTexId_ = 0;
    // Shared full-screen geometry, created in initPipeline and handed to every
    // pass. Held here (not inside a pass) so a single VBO is reused across passes
    // and freed once, on the GL thread, in surfaceDestroyed.
    std::unique_ptr<FullScreenQuad> quad_;
    std::unique_ptr<PassthroughRenderer> renderer_;
    // Offscreen target the camera pass renders into; the present pass then
    // samples it to the screen. Sized to the surface in setViewport.
    std::unique_ptr<FrameBuffer> sceneFbo_;
    // Held through the RenderPass interface, not the concrete type: the present
    // pass is just the last link in the chain, and future effect passes will sit
    // in front of it behind the same interface.
    std::unique_ptr<RenderPass> present_;
    // Surface (screen) dimensions, needed to reset the viewport for the present
    // pass after an offscreen pass changed it.
    int surfaceW_ = 0;
    int surfaceH_ = 0;
};

}  // namespace forge