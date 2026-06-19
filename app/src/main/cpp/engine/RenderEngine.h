#pragma once

#include "../egl/EglContext.h"
#include "../resource/FrameBuffer.h"
#include "../resource/FullScreenQuad.h"
#include "../shader/PassthroughRenderer.h"
#include "../shader/RenderPass.h"

#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>
#include <vector>

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
    RenderEngine() = default;

    RenderEngine(const RenderEngine&)            = delete;
    RenderEngine& operator=(const RenderEngine&) = delete;

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
    // Head pass: samples the OES camera texture (plus the per-frame SurfaceTexture
    // matrix) and renders the cropped, oriented frame into pingPong_[0]. Kept as the
    // concrete type, outside the effects_ chain, because its input differs from a
    // RenderPass (external OES + matrix, not a 2D texture).
    std::unique_ptr<PassthroughRenderer> camera_;
    // Ordered effect chain, each behind the RenderPass interface. Every pass samples
    // the previous stage's output and writes the next ping-pong target, so extending
    // the chain is a single push_back in initPipeline — no new members or wiring.
    std::vector<std::unique_ptr<RenderPass>> effects_;
    // Final pass: blits whichever ping-pong target holds the final result to the
    // window. Behind RenderPass like the effects — just the last link in the chain.
    std::unique_ptr<RenderPass> present_;
    // The two offscreen targets the chain ping-pongs between: the camera renders into
    // [0], each effect reads one and writes the other, and present_ samples the last
    // one written. Two suffice for any chain length; both are surface-sized in
    // setViewport.
    std::unique_ptr<FrameBuffer> pingPong_[2];
    // Surface (screen) dimensions, needed to reset the viewport for the present
    // pass after an offscreen pass changed it.
    int surfaceW_ = 0;
    int surfaceH_ = 0;
};

}  // namespace forge