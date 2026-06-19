#include "PassthroughRenderer.h"

#include "../CheckGl.h"
#include "../resource/FullScreenQuad.h"
#include "ShaderProgram.h"

#include <GLES2/gl2ext.h>
#include <algorithm>

#define LOG_TAG "PassthroughRenderer"
#include "../Log.h"

namespace forge {

// Vertex shader: runs once per quad corner. It places the corner on screen and
// computes the UV the fragment shader will sample the camera image at.
static constexpr std::string_view kVertSrc = R"GLSL(
    #version 300 es

    // --- Attributes: per-vertex inputs, one value per corner. The location slots
    // must match what FullScreenQuad::draw feeds them (see FullScreenQuad for the
    // attribute layout and what NDC/UV mean).
    layout(location = 0) in vec2 aPosition;  // NDC corner position, -1..1
    layout(location = 1) in vec2 aTexCoord;  // UV into the camera image, 0..1

    // --- Uniforms: shader inputs constant across all 4 corners of this draw.
    uniform mat4 uTexMatrix;   // SurfaceTexture's transform: sensor orientation + HAL crop
    uniform vec2 uCropScale;   // shrinks UVs so only the center region is sampled (cover-crop)
    uniform vec2 uCropOffset;  // re-centers that shrunk UV window

    // --- Varying: a value written per-corner that the GPU smoothly interpolates
    // across every pixel of the triangle, so each fragment receives a blended UV.
    // The rasterizer does this automatically.
    out vec2 vTexCoord;

    void main() {
        // aPosition is already in NDC, so pass it straight through. z=0, w=1 expand
        // the vec2 into the vec4 that gl_Position requires.
        gl_Position = vec4(aPosition, 0.0, 1.0);

        // Crop first: shrink the 0..1 UV range to the centered sub-region we keep
        // (see setViewport for how the crop is derived). This must run before
        // uTexMatrix, which expects coordinates in the pre-transform UV space.
        vec2 uv = aTexCoord * uCropScale + uCropOffset;

        // Then apply SurfaceTexture's transform. The vec4 promotion is just so the
        // mat4 multiply works; we take .xy back out.
        vTexCoord = (uTexMatrix * vec4(uv, 0.0, 1.0)).xy;
    }
)GLSL";

// Fragment shader: runs once per pixel, samples the camera texture at the
// interpolated UV, and writes the color to the framebuffer.
static constexpr std::string_view kFragSrc = R"GLSL(
    #version 300 es

    // Required to use samplerExternalOES (the sampler type for an OES texture).
    #extension GL_OES_EGL_image_external_essl3 : require

    // mediump = medium float precision, the usual mobile default for color math.
    precision mediump float;

    in vec2 vTexCoord;

    // The camera's OES texture (see RenderEngine::createOesTexture). samplerExternalOES
    // is the OES counterpart to sampler2D — the driver does YUV->RGB on sample.
    uniform samplerExternalOES uTexture;

    out vec4 fragColor;

    void main() {
        // texture() returns RGBA. The camera image is opaque, so alpha is always 1.
        fragColor = texture(uTexture, vTexCoord);
    }
)GLSL";

bool PassthroughRenderer::init(GLuint oesTextureId, const FullScreenQuad* quad) {
    // We sample these but don't own them: the OES texture and quad are created and
    // freed by RenderEngine. We only keep handles to them.
    oesTexId_ = oesTextureId;
    quad_ = quad;

    // Compile + link the shaders into a GPU program (see ShaderProgram for what
    // compile and link mean). Either source failing aborts init.
    GLuint vert = compileShader(GL_VERTEX_SHADER, kVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }
    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }

    // A "uniform" is a shader input constant across one draw call (unlike per-vertex
    // attributes). The GPU assigns each an integer slot at link time; we cache the
    // slots here so draw() doesn't look them up by name every frame.
    uTexMatrix_  = glGetUniformLocation(program_, "uTexMatrix");
    uTexture_    = glGetUniformLocation(program_, "uTexture");
    uCropScale_  = glGetUniformLocation(program_, "uCropScale");
    uCropOffset_ = glGetUniformLocation(program_, "uCropOffset");

    CHECK_GL("PassthroughRenderer::init");
    LOGI("initialized");
    return true;
}

// Computes the UV crop that makes the camera image fill the surface without
// letterboxing — the same idea as CSS "background-size: cover": scale the image up
// until its shorter side matches the surface, then crop the overflow on the longer
// side. The resulting cropScale/cropOffset are uploaded as uniforms each draw call.
//
// Example: camera 1080x1920 (portrait) into a 1080x1080 (square) surface:
//   scaleW=1.0, scaleH=0.5625 -> renderScale=1.0 -> cropScaleY=0.5625, cropOffsetY=0.219
//   -> only the center 56.25% of the camera height shows, cropped equally top and bottom.
void PassthroughRenderer::setViewport(int camW, int camH, int surfW, int surfH) {
    // Scale needed to fill the surface along each axis independently.
    float scaleW = static_cast<float>(surfW) / static_cast<float>(camW);
    float scaleH = static_cast<float>(surfH) / static_cast<float>(camH);

    // The larger scale fills the surface completely; the other axis overflows and
    // gets cropped, which is what avoids letterbox bars.
    float renderScale = std::max(scaleW, scaleH);

    // cropScale = the fraction of the camera's 0..1 UV range we actually sample on
    // each axis. Normalizing by renderScale gives 1.0 on the filling axis and < 1.0
    // on the cropped one. The vertex shader applies it as uv *= uCropScale.
    cropScaleX_ = scaleW / renderScale;
    cropScaleY_ = scaleH / renderScale;

    // Offset re-centers the shrunk UV window; without it we'd sample from one edge.
    // Half the unused range on each side crops equally. e.g. cropScaleY=0.5625 ->
    // offset=(1-0.5625)*0.5=0.219 -> samples v in 0.219..0.781.
    cropOffsetX_ = (1.0f - cropScaleX_) * 0.5f;
    cropOffsetY_ = (1.0f - cropScaleY_) * 0.5f;

    LOGI("viewport set: cam=%dx%d surf=%dx%d cropScale=(%.3f,%.3f)", camW, camH, surfW, surfH,
         cropScaleX_, cropScaleY_);
}

// Issues one full-screen draw that samples the camera texture onto the quad.
// texMatrix4x4 is SurfaceTexture.getTransformMatrix() and changes every frame, so
// it's re-fetched and re-uploaded each call.
void PassthroughRenderer::draw(const float* texMatrix4x4) const {
    // OpenGL is a global state machine: this makes our program the one every
    // following GL call and the draw use.
    glUseProgram(program_);

    // Binding a texture for sampling is three steps. The GPU has numbered texture
    // units (input slots); you attach a texture to a unit, then point the sampler
    // uniform at that unit number. (The other passes reuse this same pattern.)
    glActiveTexture(GL_TEXTURE0);                          // 1. select unit 0
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId_);     // 2. attach our OES texture to it
    glUniform1i(uTexture_, 0);                             // 3. tell the sampler to read unit 0

    // Upload the per-frame uniforms (slots cached in init). The matrix changes every
    // frame; the crop only changes on setViewport. "1" = one matrix, GL_FALSE = not transposed.
    glUniformMatrix4fv(uTexMatrix_, 1, GL_FALSE, texMatrix4x4);
    glUniform2f(uCropScale_, cropScaleX_, cropScaleY_);
    glUniform2f(uCropOffset_, cropOffsetX_, cropOffsetY_);

    // Draw the shared quad (binds its VBO and issues the triangle-strip draw that
    // covers every pixel of the target).
    quad_->draw();

    CHECK_GL("PassthroughRenderer::draw");
}

// Frees only what this pass owns: the shader program. The OES texture and quad
// belong to RenderEngine and are freed there.
void PassthroughRenderer::destroy() {
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}

}  // namespace forge
