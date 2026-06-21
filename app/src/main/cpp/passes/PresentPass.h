#pragma once

#include "RenderPass.h"

#include <GLES3/gl3.h>

namespace forge {

class FullScreenQuad;

/*
 * Final pass of the render graph: samples a 2D texture (the processed scene) and
 * draws it to the currently bound framebuffer — normally the default framebuffer
 * (the screen). Straight 0..1 UVs with no crop or orientation, because the camera
 * pass already baked those in when it rendered into the offscreen target.
 *
 */
class PresentPass : public RenderPass {
public:
    PresentPass() = default;
    ~PresentPass() override { destroy(); }

    // quad is owned by RenderEngine and shared across passes; it must outlive
    // this pass and stay valid for every draw() call.
    bool init(const FullScreenQuad* quad);

    // Draw inputTexture (a GL_TEXTURE_2D) over the currently bound framebuffer.
    void draw(GLuint inputTexture) const override;

    void destroy();

private:
    const FullScreenQuad* quad_ = nullptr;
    GLuint program_             = 0;
    GLint uTexture_             = -1;
};

}  // namespace forge
