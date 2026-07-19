#include "EffectPass.h"

#include "PassthroughVert.h"
#include "../gl/CheckGl.h"
#include "../gl/ShaderProgram.h"
#include "../resources/FullScreenQuad.h"

#include <string_view>

#define LOG_TAG "EffectPass"
#include "../Log.h"

namespace forge {

bool EffectPass::init(const FullScreenQuad* quad, std::string_view fragmentSrc) {
    quad_ = quad;

    GLuint vert = compileShader(GL_VERTEX_SHADER, kPassthroughVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }
    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }
    // uTexelSize is optional: an effect that doesn't sample neighbors won't
    // declare it, leaving the location at -1 so the per-frame glUniform2f below is
    // a silent no-op rather than an error.
    uTexture_   = glGetUniformLocation(program_, "uTexture");
    uTexelSize_ = glGetUniformLocation(program_, "uTexelSize");

    CHECK_GL("EffectPass::init");
    LOGI("initialized");
    return true;
}

void EffectPass::onViewport(int width, int height) {
    // One texel measured in UV space: stepping vTexCoord by this amount lands
    // exactly on the next pixel, which is what a convolution kernel needs to read
    // its neighbors. Guard against a zero size before the surface is known.
    texelW_ = width > 0 ? 1.0f / static_cast<float>(width) : 0.0f;
    texelH_ = height > 0 ? 1.0f / static_cast<float>(height) : 0.0f;
}

void EffectPass::draw(GLuint inputTexture) const {
    glUseProgram(program_);

    // Bind the input (the previous stage's output) to texture unit 0 and point the
    // sampler at it — the standard 3-step bind (see CameraPass::draw). This
    // input is a plain 2D texture, not OES, so the target is GL_TEXTURE_2D.
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, inputTexture);
    glUniform1i(uTexture_, 0);

    // Upload the texel size so neighbor-sampling effects can offset their reads.
    glUniform2f(uTexelSize_, texelW_, texelH_);

    quad_->draw();

    CHECK_GL("EffectPass::draw");
}

void EffectPass::destroy() {
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}

}  // namespace forge
