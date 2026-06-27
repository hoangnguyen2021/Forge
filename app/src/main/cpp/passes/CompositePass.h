#pragma once

#include <GLES3/gl3.h>

namespace forge {

class FullScreenQuad;

/*
 * Compositing pass for background blur: the fork-and-merge tail of the render graph.
 * Unlike a RenderPass it samples THREE inputs at once — the sharp camera frame, a
 * fully blurred copy of it, and a segmentation mask — and writes
 * mix(blurred, sharp, mask) into the currently bound framebuffer. mask == 1 keeps the
 * sharp foreground (the person); mask == 0 falls through to the blurred background.
 *
 * It is deliberately not a RenderPass: that interface's draw(GLuint) takes a single
 * input, and there is no polymorphic chain of multi-input passes for the engine to
 * drive. Like CameraPass it is held as a concrete type and called explicitly by
 * RenderEngine. If a second multi-input pass ever lands, extract a shared interface
 * from the two then — not speculatively now.
 *
 * The mask arrives lower-res than the frame (a segmentation model runs small); the
 * composite samples it with the frame's full-res UVs and lets GL_LINEAR upsample it
 * for free, so no explicit upscale pass is needed.
 */
class CompositePass {
public:
    CompositePass() = default;
    ~CompositePass() { destroy(); }

    CompositePass(const CompositePass&)            = delete;
    CompositePass& operator=(const CompositePass&) = delete;

    // quad is owned by RenderEngine and shared across passes; it must outlive this
    // pass and stay valid for every draw() call.
    bool init(const FullScreenQuad* quad);

    // Sample sharp, blurred, and mask (all GL_TEXTURE_2D) and write the composited
    // result into the currently bound framebuffer. The engine binds the target and
    // sets the viewport first, exactly as for the single-input passes.
    void draw(GLuint sharp, GLuint blurred, GLuint mask) const;

    void destroy();

private:
    const FullScreenQuad* quad_ = nullptr;
    GLuint program_             = 0;
    GLint uSharp_               = -1;
    GLint uBlurred_             = -1;
    GLint uMask_                = -1;
};

}  // namespace forge
