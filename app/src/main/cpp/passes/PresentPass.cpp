#include "PresentPass.h"

#include "../gl/CheckGl.h"
#include "../gl/ShaderProgram.h"
#include "../resources/FullScreenQuad.h"

#include <string_view>

#define LOG_TAG "PresentPass"
#include "../Log.h"

namespace forge {

// Vertex shader: a plain full-screen passthrough. Unlike the camera pass there
// is no crop or orientation matrix — the scene texture is already upright and
// cropped — so the texture coordinate is forwarded to the fragment shader as-is.
static constexpr std::string_view kVertSrc = R"GLSL(
    #version 300 es
    layout(location = 0) in vec2 aPosition;  // a quad corner in NDC, -1..1
    layout(location = 1) in vec2 aTexCoord;  // that corner's UV into the scene texture, 0..1
    out vec2 vTexCoord;                      // UV forwarded to the fragment shader
    void main() {
        gl_Position = vec4(aPosition, 0.0, 1.0);
        vTexCoord = aTexCoord;
    }
)GLSL";

// Fragment shader: sample the scene texture. A regular sampler2D this time (not
// samplerExternalOES) — the source is an ordinary RGBA8 texture produced by an
// earlier pass, not a camera buffer needing YUV conversion.
static constexpr std::string_view kFragSrc = R"GLSL(
    #version 300 es
    precision mediump float;     // medium float precision, the usual mobile default for color math
    in vec2 vTexCoord;           // interpolated UV from the vertex shader, 0..1
    uniform sampler2D uTexture;  // the scene texture to present (last pass's output)
    out vec4 fragColor;          // the color written to the screen
    void main() {
        fragColor = texture(uTexture, vTexCoord);
    }
)GLSL";

bool PresentPass::init(const FullScreenQuad* quad) {
    quad_ = quad;

    GLuint vert = compileShader(GL_VERTEX_SHADER, kVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }
    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }
    uTexture_ = glGetUniformLocation(program_, "uTexture");

    CHECK_GL("PresentPass::init");
    LOGI("initialized");
    return true;
}

void PresentPass::draw(GLuint inputTexture) const {
    glUseProgram(program_);

    // Standard 3-step bind of the input texture to unit 0 (see CameraPass::draw).
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, inputTexture);
    glUniform1i(uTexture_, 0);

    quad_->draw();

    CHECK_GL("PresentPass::draw");
}

void PresentPass::destroy() {
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}

}  // namespace forge