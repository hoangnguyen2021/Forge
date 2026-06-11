#pragma once

#include <GLES3/gl3.h>

namespace forge {

/*
 * Common interface for a render-graph pass that reads one 2D texture and writes
 * into the currently bound framebuffer. The caller (RenderEngine) binds the
 * target framebuffer and sets the viewport before invoking draw(); the pass only
 * samples its input and emits fragments. Keeping the target out of the pass is
 * what lets the engine chain passes uniformly — bind FBO A, run effect, bind
 * FBO B, run the next effect on A's output — and present to screen with the same
 * call shape, ping-ponging through a pool of framebuffers it owns.
 *
 * The camera/source pass (PassthroughRenderer) is deliberately not a RenderPass:
 * its input is an external OES texture plus a per-frame SurfaceTexture transform,
 * not a 2D texture, so it sits at the head of the graph rather than in the
 * uniform chain. Everything downstream of the first offscreen target conforms to
 * this interface.
 */
class RenderPass {
public:
    virtual ~RenderPass() = default;

    RenderPass(const RenderPass&)            = delete;
    RenderPass& operator=(const RenderPass&) = delete;

    // Sample inputTexture (a GL_TEXTURE_2D) into the currently bound framebuffer.
    virtual void draw(GLuint inputTexture) const = 0;

protected:
    RenderPass() = default;
};

}  // namespace forge