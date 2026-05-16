#include "PassthroughRenderer.h"
#include "ShaderProgram.h"
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <algorithm>
#include <array>
#include <cstddef>

#define TAG "PassthroughRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Vertex shader: runs once per corner of the quad.
// Its only job is to pass the screen position straight through and
// compute the texture coordinate that the fragment shader will sample.
static constexpr std::string_view kVertSrc = R"GLSL(
#version 300 es

// --- Attributes: per-vertex inputs from CPU memory (uploaded into the VBO at init), different
// value for each of the 4 corners.
// layout(location = N) pins each to a slot number, must match the slot number passed to
// glVertexAttribPointer in draw().
layout(location = 0) in vec2 aPosition;  // screen coordinate of this corner in NDC (-1..1)
layout(location = 1) in vec2 aTexCoord;  // which part of the camera image maps to this corner (UV 0..1)

// --- Uniforms: constant across all 4 vertices, set from the CPU via glUniform* before each draw.
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

// Each vertex of the quad carries two pieces of data: a screen position and a texture coordinate.
// The layout here must exactly match what we describe to the GPU via glVertexAttribPointer.
struct QuadVertex {
    float x, y;  // NDC position: (-1,-1) is bottom-left, (1,1) is top-right of the screen
    float u, v;  // texture coordinate: (0,0) is top-left of the camera image, (1,1) is bottom-right
};

// A full-screen quad made of two triangles drawn as a strip: BL → BR → TL → TR.
// Triangle strip reuses the last two vertices for each new triangle, so 4 vertices → 2 triangles.
// NDC positions fill the entire clip space so the camera image covers every pixel on screen.
static constexpr std::array<QuadVertex, 4> kQuad = {
        {
                {-1.0f, -1.0f, 0.0f, 0.0f},  // bottom-left
                {1.0f, -1.0f, 1.0f, 0.0f},   // bottom-right
                {-1.0f, 1.0f, 0.0f, 1.0f},   // top-left
                {1.0f, 1.0f, 1.0f, 1.0f},    // top-right
        }
};

bool PassthroughRenderer::init(GLuint oesTextureId) {
    // Hold onto the OES texture ID created by SurfaceTexture on the Java side.
    // We don't own this texture — we just sample from it each frame.
    oesTexId_ = oesTextureId;

    // Compile and link the two shaders into a single GPU program (see ShaderProgram.cpp).
    GLuint vert = compileShader(GL_VERTEX_SHADER, kVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }

    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }

    // Look up the integer "slot" for each uniform variable declared in the GLSL source.
    // These slots are stable for the lifetime of the program and are used in draw() to
    // push CPU-side values (matrices, texture unit numbers) into the shader.
    uTexMatrix_ = glGetUniformLocation(program_, "uTexMatrix");
    uTexture_ = glGetUniformLocation(program_, "uTexture");
    uCropScale_ = glGetUniformLocation(program_, "uCropScale");
    uCropOffset_ = glGetUniformLocation(program_, "uCropOffset");

    // Upload the quad geometry to a VBO (Vertex Buffer Object) — a buffer that lives on the GPU.
    // Keeping vertex data on the GPU avoids re-uploading it from CPU memory every frame.
    // GL_STATIC_DRAW is a hint: the data is written once and read many times, so the driver
    // can place it in fast GPU memory.
    glGenBuffers(1, &vbo_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferData(GL_ARRAY_BUFFER, sizeof(kQuad), kQuad.data(), GL_STATIC_DRAW);
    static_assert(sizeof(QuadVertex) == 4 * sizeof(float), "QuadVertex layout mismatch");
    glBindBuffer(GL_ARRAY_BUFFER, 0);  // unbind so later GL calls don't accidentally modify this buffer

    LOGI("initialized");
    return true;
}

// Computes the UV crop that makes the camera image fill the surface without letterboxing.
// Strategy: scale the camera image up until its shorter side matches the surface, then
// crop the overflow on the longer side — identical to CSS "background-size: cover".
// The resulting cropScale/cropOffset are uploaded to the vertex shader each draw call.
void PassthroughRenderer::setViewport(int camW, int camH, int surfW, int surfH) {
    float scaleW = static_cast<float>(surfW) / static_cast<float>(camW);
    float scaleH = static_cast<float>(surfH) / static_cast<float>(camH);
    float renderScale = std::max(scaleW, scaleH);  // fill surface with no letterbox; excess is cropped
    // cropScale < 1.0 means we only use a centered sub-region of the camera's UV space.
    cropScaleX_ = scaleW / renderScale;
    cropScaleY_ = scaleH / renderScale;
    // Shift the sub-region to the center so equal amounts are cropped on both sides.
    cropOffsetX_ = (1.0f - cropScaleX_) * 0.5f;
    cropOffsetY_ = (1.0f - cropScaleY_) * 0.5f;
    LOGI("viewport set: cam=%dx%d surf=%dx%d cropScale=(%.3f,%.3f)",
         camW, camH, surfW, surfH, cropScaleX_, cropScaleY_);
}

// Issues one full-screen draw call that samples the camera texture onto the quad.
// texMatrix4x4 comes from SurfaceTexture.getTransformMatrix() — it encodes any rotation
// or flip the camera hardware applied to the buffer, and must be re-fetched each frame.
void PassthroughRenderer::draw(const float *texMatrix4x4) const {
    // Activate our shader program — all subsequent GL state changes and draw calls use it.
    glUseProgram(program_);

    // Bind the camera texture to texture unit 0, then tell the shader uniform which unit to sample.
    // The GPU has a fixed number of texture units (slots); you bind a texture to a slot, then
    // point the sampler uniform at that slot number.
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId_);
    glUniform1i(uTexture_, 0);  // "uTexture samples from unit 0"

    // Push the per-frame CPU values into the shader uniforms.
    glUniformMatrix4fv(uTexMatrix_, 1, GL_FALSE, texMatrix4x4);
    glUniform2f(uCropScale_, cropScaleX_, cropScaleY_);
    glUniform2f(uCropOffset_, cropOffsetX_, cropOffsetY_);

    // Describe the vertex layout so the GPU knows how to read our VBO.
    // glVertexAttribPointer tells the GPU: "for attribute slot N, read 2 floats per vertex,
    // stride = sizeof(QuadVertex) bytes apart, starting at this byte offset into the buffer."
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, x)));  // position
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, u)));  // texcoord

    // Draw 4 vertices as a triangle strip — two triangles that together cover the full screen.
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // Clean up: disable the attribute arrays and unbind the buffer so other draw calls
    // don't accidentally inherit this state.
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

// Releases GPU resources owned by this renderer. The OES texture is not deleted here
// because it was created externally (by SurfaceTexture) and is not ours to free.
void PassthroughRenderer::destroy() {
    if (vbo_ != 0) {
        glDeleteBuffers(1, &vbo_);
        vbo_ = 0;
    }
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
}
