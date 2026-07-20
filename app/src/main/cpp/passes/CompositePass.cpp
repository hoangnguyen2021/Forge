#include "CompositePass.h"

#include "PassthroughVert.h"
#include "../gl/CheckGl.h"
#include "../gl/ShaderProgram.h"
#include "../resources/FullScreenQuad.h"

#include <string_view>

#define LOG_TAG "CompositePass"
#include "../Log.h"

namespace forge {

// Fragment shader: the merge step of the background blur. Three plain sampler2D
// inputs (not OES — all three are RGBA8/R8 textures from earlier passes) and a
// per-pixel mix between the sharp and blurred frames driven by the mask.
static constexpr std::string_view kFragSrc = R"GLSL(#version 300 es
    precision mediump float;     // medium float precision, the usual mobile default for color math
    in vec2 vTexCoord;           // full-res UV from the passthrough vertex stage, 0..1
    uniform sampler2D uSharp;    // the crisp camera frame
    uniform sampler2D uBlurred;  // a fully blurred copy of the same frame
    uniform sampler2D uMask;     // segmentation mask: 1 = foreground, 0 = background (low-res, upsampled here)
    out vec4 fragColor;          // the composited color written to the screen
    void main() {
        // .r: the mask is a single-channel R8 texture (one coverage value per texel).
        // Sampling it with full-res UVs lets GL_LINEAR upsample it to frame size for free.
        float m = texture(uMask, vTexCoord).r;
        vec3 sharp   = texture(uSharp, vTexCoord).rgb;
        vec3 blurred = texture(uBlurred, vTexCoord).rgb;
        // Keep the foreground crisp, let the background fall through to the blur.
        fragColor = vec4(mix(blurred, sharp, m), 1.0);
    }
)GLSL";

bool CompositePass::init(const FullScreenQuad* quad) {
    quad_ = quad;

    GLuint vert = compileShader(GL_VERTEX_SHADER, kPassthroughVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }
    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }
    uSharp_   = glGetUniformLocation(program_, "uSharp");
    uBlurred_ = glGetUniformLocation(program_, "uBlurred");
    uMask_    = glGetUniformLocation(program_, "uMask");

    CHECK_GL("CompositePass::init");
    LOGI("initialized");
    return true;
}

void CompositePass::draw(GLuint sharp, GLuint blurred, GLuint mask) const {
    glUseProgram(program_);

    // One input per texture unit. The same 3-step bind as the single-input passes
    // (see CameraPass::draw), repeated per unit, with each sampler uniform pointed at
    // its unit index so the shader reads the right texture.
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, sharp);
    glUniform1i(uSharp_, 0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, blurred);
    glUniform1i(uBlurred_, 1);

    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, mask);
    glUniform1i(uMask_, 2);

    quad_->draw();

    // Leave unit 0 active so the next frame's single-unit binds (camera, effects)
    // behave as they expect rather than configuring whatever unit we left current.
    glActiveTexture(GL_TEXTURE0);

    CHECK_GL("CompositePass::draw");
}

void CompositePass::destroy() {
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}

}  // namespace forge
