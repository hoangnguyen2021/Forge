#pragma once

#include "RenderPass.h"

#include <GLES3/gl3.h>
#include <string_view>

namespace forge {

class FullScreenQuad;

/*
 * A configurable single-input effect pass: samples one 2D texture (the previous
 * stage's output) and writes the shaded result into the currently bound
 * framebuffer. Unlike PresentPass, the fragment shader is supplied at init() time,
 * so one class serves any per-pixel effect — grade, blur, edge detect — without a
 * new subclass per look.
 *
 * Every effect shares the same contract: a full-screen passthrough vertex stage,
 * the input texture on unit 0 (uTexture), and uTexelSize — the size of one texel
 * in UV space — so neighbor-sampling effects like convolutions can step to
 * adjacent pixels. Effects that don't read neighbors simply omit uTexelSize and
 * the per-frame upload becomes a no-op.
 */
class EffectPass : public RenderPass {
public:
    EffectPass() = default;
    ~EffectPass() override { destroy(); }

    // quad is owned by RenderEngine and shared across passes; it must outlive this
    // pass. fragmentSrc is the GLSL fragment shader that defines the effect.
    bool init(const FullScreenQuad* quad, std::string_view fragmentSrc);

    // Sample inputTexture (a GL_TEXTURE_2D) into the currently bound framebuffer.
    void draw(GLuint inputTexture) const override;

    // Resolution of the texture this pass samples, used to derive uTexelSize.
    // Overrides RenderPass so the engine drives it uniformly through the chain.
    void onViewport(int width, int height) override;

    void destroy();

private:
    const FullScreenQuad* quad_ = nullptr;
    GLuint program_             = 0;
    GLint uTexture_             = -1;
    GLint uTexelSize_           = -1;
    // One texel measured in UV space (1/width, 1/height), uploaded as uTexelSize.
    float texelW_               = 0.0f;
    float texelH_               = 0.0f;
};

}  // namespace forge
