#pragma once

#include "../egl/EglContext.h"
#include "../gl/GpuTimer.h"
#include "../resources/FrameBuffer.h"
#include "../resources/FullScreenQuad.h"
#include "../passes/CameraPass.h"
#include "../passes/RenderPass.h"

#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>
#include <vector>

namespace forge {

/*
 * RenderEngine owns all per-surface GL state and drives the render graph that
 * turns camera frames into screen pixels. One instance per camera preview
 * surface. The JNI layer (forge_jni.cpp) calls in; everything runs on a single
 * GL thread owned by the Kotlin side (CameraSurfaceCallback).
 *
 * Canonical pipeline map (other files point here instead of redrawing it):
 *
 *   camera OES texture          one camera frame, delivered via SurfaceTexture
 *        |   camera_ (CameraPass): orientation + cover-crop
 *        v
 *   pingPong_[0] <--+
 *        |          |  effects_ (RenderPass chain): each effect reads one
 *        |          |  target and writes the other, alternating ("ping-pong")
 *        v          |
 *   pingPong_[1] >--+
 *        |   present_ (PresentPass): blit the final target to the window
 *        v
 *   screen (default framebuffer)
 *
 * Two offscreen targets (FrameBuffer) suffice for any number of effects, since
 * each effect only needs its immediate input. With no effects, present blits the
 * camera output directly.
 *
 * Lifecycle (every call on the GL thread, in this order):
 *   surfaceCreated   - create the EGL context for the window
 *   createOesTexture - allocate the texture the camera writes into
 *   initPipeline     - build the quad, the passes, and the ping-pong targets
 *   setViewport      - (re)size the targets and recompute the crop; may repeat
 *   drawFrame        - render one frame; called once per camera frame
 *   surfaceDestroyed - release everything, in reverse order of acquisition
 *
 * Lifecycle precondition: the destructor assumes surfaceDestroyed() already ran
 * on the GL thread. Without that, unique_ptr cleanup would issue EGL/GL calls on
 * whatever thread `delete` lands on, with no current context.
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
    // The camera's GL texture id (see createOesTexture for what "OES" means).
    GLuint oesTextureId_ = 0;
    // Shared full-screen geometry, created in initPipeline and handed to every
    // pass. Held here (not inside a pass) so a single VBO is reused across passes
    // and freed once, on the GL thread, in surfaceDestroyed.
    std::unique_ptr<FullScreenQuad> quad_;
    // Head pass: samples the OES camera texture (plus the per-frame SurfaceTexture
    // matrix) and renders the cropped, oriented frame into pingPong_[0]. Kept as the
    // concrete type, outside the effects_ chain, because its input differs from a
    // RenderPass (external OES + matrix, not a 2D texture).
    std::unique_ptr<CameraPass> camera_;
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
    // Per-pass GPU timing. Disabled (no-op) if the device lacks the timer extension.
    // gpuLogFrame_ throttles how often the harvested timings are logged.
    GpuTimer gpuTimer_;
    int gpuLogFrame_ = 0;
};

}  // namespace forge