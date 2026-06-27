#pragma once

#include "../egl/EglContext.h"
#include "../gl/GpuTimer.h"
#include "../resources/FrameBuffer.h"
#include "../resources/FullScreenQuad.h"
#include "../passes/CameraPass.h"
#include "../passes/CompositePass.h"
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
 *   camera OES --[camera_]--> sharp_ ---------------------------+  (kept alive)
 *                               |                               |
 *                               |  effects_ (RenderPass chain):  |
 *                               |  blur a COPY of sharp_,         |
 *                               v  ping-ponging the two targets   |
 *                       pingPong_[0] <-> pingPong_[1] = blurred   |
 *                               |                               |
 *   maskTexture_ (placeholder) -+-------------------------------+
 *                               v
 *        composite_ (CompositePass): mix(blurred, sharp, mask) --> screen
 *
 * The graph forks (the sharp frame is preserved while a blurred copy is produced)
 * and merges at the composite, so three offscreen targets are needed: sharp_ plus
 * the two blur ping-pong targets. With no effects the composite mixes sharp_ with
 * itself, so the camera frame passes through unchanged.
 *
 * Lifecycle (every call on the GL thread, in this order):
 *   surfaceCreated   - create the EGL context for the window
 *   createOesTexture - allocate the texture the camera writes into
 *   initPipeline     - build the quad, the passes, the offscreen targets, and the mask
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
    // Uploads the placeholder segmentation mask (see RenderEngine.cpp) and stores its
    // id in maskTextureId_. Internal helper for initPipeline; returns the same id.
    GLuint createMaskTexture();

    std::unique_ptr<EglContext> egl_;
    // The camera's GL texture id (see createOesTexture for what "OES" means).
    GLuint oesTextureId_ = 0;
    // Shared full-screen geometry, created in initPipeline and handed to every
    // pass. Held here (not inside a pass) so a single VBO is reused across passes
    // and freed once, on the GL thread, in surfaceDestroyed.
    std::unique_ptr<FullScreenQuad> quad_;
    // Head pass: samples the OES camera texture (plus the per-frame SurfaceTexture
    // matrix) and renders the cropped, oriented frame into sharp_. Kept as the
    // concrete type, outside the effects_ chain, because its input differs from a
    // RenderPass (external OES + matrix, not a 2D texture).
    std::unique_ptr<CameraPass> camera_;
    // Ordered effect chain, each behind the RenderPass interface. Every pass samples
    // the previous stage's output and writes the next ping-pong target, so extending
    // the chain is a single push_back in initPipeline — no new members or wiring.
    std::vector<std::unique_ptr<RenderPass>> effects_;
    // Terminal pass: composites the sharp frame, its blurred copy, and the segmentation
    // mask (mix(blurred, sharp, mask)) straight to the window. Held as a concrete type,
    // not a RenderPass — its three inputs don't fit draw(GLuint), so like CameraPass it
    // sits outside the uniform single-input chain.
    std::unique_ptr<CompositePass> composite_;
    // The untouched camera frame, kept alive for the whole frame so the composite can
    // read the crisp original after the blur chain has overwritten the ping-pong
    // targets. Surface-sized in setViewport.
    std::unique_ptr<FrameBuffer> sharp_;
    // The two targets the blur chain ping-pongs between: the first blur pass reads
    // sharp_ and writes [0], each subsequent pass reads one and writes the other, so the
    // last one written holds the fully blurred frame. Both surface-sized in setViewport.
    std::unique_ptr<FrameBuffer> pingPong_[2];
    // Placeholder segmentation mask (Step 1): a static low-res R8 radial gradient
    // standing in for the eventual TFLite output. Created in initPipeline, never resized.
    GLuint maskTextureId_ = 0;
    // Surface (screen) dimensions, needed to reset the viewport for the composite
    // pass after an offscreen pass changed it.
    int surfaceW_ = 0;
    int surfaceH_ = 0;
    // Per-pass GPU timing. Disabled (no-op) if the device lacks the timer extension.
    // gpuLogFrame_ throttles how often the harvested timings are logged.
    GpuTimer gpuTimer_;
    int gpuLogFrame_ = 0;
};

}  // namespace forge