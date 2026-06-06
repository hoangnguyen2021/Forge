#include "PassthroughRenderer.h"
#include "ShaderProgram.h"
#include "../CheckGl.h"
#include "../resource/FullScreenQuad.h"
#include <GLES2/gl2ext.h>
#include <algorithm>

#define LOG_TAG "PassthroughRenderer"
#include "../Log.h"

// Vertex shader: runs once per corner of the quad.
// Its only job is to pass the screen position straight through and
// compute the texture coordinate that the fragment shader will sample.
static constexpr std::string_view kVertSrc = R"GLSL(
    #version 300 es

    // --- Attributes: per-vertex inputs from CPU memory, different value for each of the 4 corners.
    // layout(location = N) pins each to a slot number, must match the slot number passed to
    // glVertexAttribPointer in draw().
    // NDC screen coordinate of this corner: where to draw on screen (-1..1)
    layout(location = 0) in vec2 aPosition;
    // UV coordinate into the camera image: where to sample the camera image (0..1)
    layout(location = 1) in vec2 aTexCoord;

    // --- Uniforms: constant across all 4 vertices.
    uniform mat4 uTexMatrix;   // encodes whatever rotation/flip the camera hardware applied to the buffer
    uniform vec2 uCropScale;   // shrinks UVs so only the center region of the camera image is used
    uniform vec2 uCropOffset;  // shifts the shrunk UV window to re-center it

    // --- Varying: output to the fragment shader.
    // The GPU interpolates this across every pixel inside the triangle using barycentric coordinates —
    // each pixel gets a weight (α, β, γ) for each corner that sums to 1.0, and its vTexCoord is
    // α*A + β*B + γ*C. A pixel at corner A gets exactly A's value; a pixel dead center gets the
    // average. This happens automatically in the rasterizer, with no extra code needed.
    out vec2 vTexCoord;

    void main() {
        // required output: where this vertex lands on screen.
        // aPosition is already in NDC so no transformation is needed — pass it straight through.
        // vec4(aPosition, 0.0, 1.0) expands vec2 → vec4: z=0 (no depth), w=1 (required for a point).
        gl_Position = vec4(aPosition, 0.0, 1.0);

        // Step 1 — crop: shrink the 0..1 UV range down to the center sub-region of the camera image.
        // uCropScale/uCropOffset must be applied before uTexMatrix because the crop
        // lives in the pre-rotation UV space that SurfaceTexture's transform expects.
        vec2 uv = aTexCoord * uCropScale + uCropOffset;

        // Step 2 — rotate/flip: apply SurfaceTexture's matrix to handle device orientation.
        // uv must be promoted to vec4 (w=1) for the mat4 multiply, then we extract .xy back out.
        vTexCoord = (uTexMatrix * vec4(uv, 0.0, 1.0)).xy;
    }
)GLSL";

// Fragment shader: runs once per pixel covered by the quad.
// Receives the interpolated vTexCoord from the vertex shader and samples the camera texture.
// The result is written to fragColor, which the GPU writes to the framebuffer for that pixel.
static constexpr std::string_view kFragSrc = R"GLSL(
    #version 300 es

    // GL_OES_EGL_image_external_essl3 must be explicitly enabled to use samplerExternalOES.
    #extension GL_OES_EGL_image_external_essl3 : require

    // Declares the default float precision for this shader.
    // mediump (medium precision) is the standard choice for fragment shaders on mobile —
    // high enough for color math, low enough to run fast on mobile GPUs.
    precision mediump float;

    // --- Varying input: interpolated value passed in from the vertex shader, one per pixel.
    in vec2 vTexCoord;  // UV coordinate computed per-corner in kVertSrc, interpolated across the triangle

    // --- Uniform: the camera texture, constant for the entire draw call.
    // samplerExternalOES is a special sampler for OES textures (fed by Android's SurfaceTexture).
    // Unlike a regular sampler2D, it lets the driver handle YUV→RGB conversion internally —
    // camera hardware produces YUV buffers, but shaders expect RGB, so the driver bridges the gap.
    uniform samplerExternalOES uTexture;

    // --- Output: the final RGBA color written to the framebuffer for this pixel.
    out vec4 fragColor;

    void main() {
        // Sample the camera texture at the UV coordinate this pixel received from the vertex shader.
        // texture() returns a vec4 (RGBA). Since the camera image is opaque, alpha will always be 1.
        fragColor = texture(uTexture, vTexCoord);
    }
)GLSL";

bool PassthroughRenderer::init(GLuint oesTextureId, const FullScreenQuad* quad) {
    // Save the OES texture ID created by SurfaceTexture on the Java side.
    // We don't own this texture — SurfaceTexture manages its lifetime — we just sample from it each frame.
    oesTexId_ = oesTextureId;

    // Cache the shared quad. It is owned by RenderEngine and reused by every pass;
    // we only sample it in draw() and never free it here.
    quad_ = quad;

    // Step 1: compile each GLSL source string into a GPU shader object.
    // compileShader sends the source to the GPU driver, which compiles it to GPU machine code —
    // just like clang compiling a .cpp file. Returns 0 on failure (compile error logged inside).
    GLuint vert = compileShader(GL_VERTEX_SHADER, kVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }

    // Step 2: link the two compiled shader objects into one GPU program.
    // Linking resolves the interface between vertex outputs (vTexCoord) and fragment inputs,
    // validates that types match, and produces a single executable the GPU can run.
    // The analogy: compile .cpp → .o files, then link the .o files into a binary.
    // After linking, vert and frag are intermediate artifacts (like .o files) and are deleted inside linkProgram.
    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }

    // Step 3: look up the integer slot for each uniform variable in the linked program.
    // Uniforms are declared by name in GLSL, but the GPU assigns each an integer slot at link time.
    // glGetUniformLocation queries that slot — caching it here avoids a name lookup every frame in draw().
    uTexMatrix_ = glGetUniformLocation(program_, "uTexMatrix");
    uTexture_   = glGetUniformLocation(program_, "uTexture");
    uCropScale_ = glGetUniformLocation(program_, "uCropScale");
    uCropOffset_= glGetUniformLocation(program_, "uCropOffset");

    CHECK_GL("PassthroughRenderer::init");
    LOGI("initialized");
    return true;
}

// Computes the UV crop that makes the camera image fill the surface without letterboxing.
// Strategy: scale the camera image up until its shorter side matches the surface, then
// crop the overflow on the longer side — identical to CSS "background-size: cover".
// Example: camera=1080x1920 (portrait), surface=1080x1080 (square):
//   scaleW=1.0, scaleH=0.5625 → renderScale=1.0 → cropScaleY=0.5625, cropOffsetY=0.219
//   → only the center 56.25% of the camera height is shown, equal amounts cropped top and bottom.
// The resulting cropScale/cropOffset are uploaded as uniforms to the vertex shader each draw call.
void PassthroughRenderer::setViewport(int camW, int camH, int surfW, int surfH) {
    // How much would we need to scale the camera image to fill the surface in each axis independently?
    float scaleW = static_cast<float>(surfW) / static_cast<float>(camW);
    float scaleH = static_cast<float>(surfH) / static_cast<float>(camH);

    // Pick the larger scale — the one that fills the surface completely with no letterbox.
    // The other axis will overflow and get cropped rather than leaving empty bars.
    float renderScale = std::max(scaleW, scaleH);

    // cropScale is the fraction of the camera's UV range (0..1) we actually sample per axis.
    // Dividing by renderScale normalizes so the filling axis gets 1.0 and the cropped axis gets < 1.0.
    // In the vertex shader: uv = aTexCoord * uCropScale shrinks the 0..1 UV range to this fraction.
    cropScaleX_ = scaleW / renderScale;
    cropScaleY_ = scaleH / renderScale;

    // Without an offset, the shrunken UV window would start at 0 and sample only one edge of the image.
    // Adding half the unused portion as an offset re-centers the window so equal amounts are cropped
    // on both sides. e.g. cropScaleY=0.5625 → offset=(1-0.5625)*0.5=0.219 → samples v=0.219..0.781.
    cropOffsetX_ = (1.0f - cropScaleX_) * 0.5f;
    cropOffsetY_ = (1.0f - cropScaleY_) * 0.5f;

    LOGI("viewport set: cam=%dx%d surf=%dx%d cropScale=(%.3f,%.3f)",
         camW, camH, surfW, surfH, cropScaleX_, cropScaleY_);
}

// Issues one full-screen draw call that samples the camera texture onto the quad.
// texMatrix4x4 comes from SurfaceTexture.getTransformMatrix() — it encodes any rotation
// or flip the camera hardware applied to the buffer, and must be re-fetched each frame.
void PassthroughRenderer::draw(const float *texMatrix4x4) const {
    // Make our shader program active. OpenGL is a global state machine — this single call means
    // every subsequent GL operation and draw call uses our vertex + fragment shaders.
    glUseProgram(program_);

    // --- Texture binding (3 steps) ---
    // The GPU has multiple texture units (input ports), numbered 0, 1, 2...
    // You bind a texture to a unit, then point the sampler uniform at that unit number.
    // Step 1: select unit 0 as the active slot for the next bind call.
    glActiveTexture(GL_TEXTURE0);
    // Step 2: plug our OES camera texture into the currently active unit (unit 0).
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId_);
    // Step 3: tell the samplerExternalOES uniform in the fragment shader to read from unit 0.
    // The sampler doesn't hold the texture itself — it holds the unit number to sample from.
    glUniform1i(uTexture_, 0);

    // --- Uniform uploads ---
    // Push per-frame CPU values into the shader using the slot IDs cached in init().
    // texMatrix4x4 changes every frame as SurfaceTexture delivers a new camera buffer.
    // '1' = uploading one matrix, GL_FALSE = not transposed.
    glUniformMatrix4fv(uTexMatrix_, 1, GL_FALSE, texMatrix4x4);
    // cropScale/cropOffset are stable until setViewport() is called again (e.g. on rotation).
    glUniform2f(uCropScale_, cropScaleX_, cropScaleY_);
    glUniform2f(uCropOffset_, cropOffsetX_, cropOffsetY_);

    // --- Geometry + draw call ---
    // The shared full-screen quad binds its VBO, describes the vertex attributes,
    // and issues the GL_TRIANGLE_STRIP draw that covers every pixel of the target.
    quad_->draw();

    CHECK_GL("PassthroughRenderer::draw");
}

// Releases GPU resources allocated by this renderer in init().
// Only frees what we own — the OES texture is not deleted here (SurfaceTexture
// owns it on the Java side) and the quad is not deleted here (RenderEngine owns
// it and shares it across passes). Only the shader program is ours.
void PassthroughRenderer::destroy() {
    // Free the linked shader program — this releases the compiled vertex + fragment shaders
    // that the GPU driver allocated when we called linkProgram() in init().
    // The individual shader objects (vert, frag) were already deleted inside linkProgram()
    // after linking, so only the program handle remains to clean up here.
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}
