#include "FullScreenQuad.h"

#include "../CheckGl.h"

#include <array>
#include <cstddef>

namespace forge {

// Each vertex carries a screen position and a texture coordinate. This layout
// must exactly match what draw() describes to the GPU via glVertexAttribPointer.
struct QuadVertex {
    float x, y;  // NDC position: (-1,-1) is bottom-left, (1,1) is top-right of the screen
    float u, v;  // texture coordinate: (0,0) is bottom-left of the source, (1,1) is top-right
};

// A full-screen quad made of two triangles drawn as a strip: BL -> BR -> TL -> TR.
// A triangle strip reuses the last two vertices for each new triangle, so 4
// vertices produce 2 triangles. NDC positions fill clip space so the source
// covers every pixel on screen.
//
// UVs follow GL's bottom-left origin: BL maps to (0,0). This keeps both the
// camera frame (via SurfaceTexture's transform matrix, applied in the camera
// pass) and FBO color textures (which are bottom-left origin) upright, so no
// vertical flip is needed between passes.
static constexpr std::array<QuadVertex, 4> kQuad = {
    {
        {-1.0f, -1.0f, 0.0f, 0.0f},  // bottom-left
        {1.0f, -1.0f, 1.0f, 0.0f},   // bottom-right
        {-1.0f, 1.0f, 0.0f, 1.0f},   // top-left
        {1.0f, 1.0f, 1.0f, 1.0f},    // top-right
    },
};

bool FullScreenQuad::init() {
    // Upload the quad vertex data to a VBO (Vertex Buffer Object) in GPU memory.
    // Uploading once here means draw() can reuse it every frame without copying
    // kQuad from CPU memory each time.
    glGenBuffers(1, &vbo_);  // allocate a GPU buffer, store its ID in vbo_
    if (vbo_ == 0) {
        return false;
    }
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);  // make vbo_ the active buffer so the next call targets it
    // GL_STATIC_DRAW hints that data is written once and read many times, so the
    // driver can place it in the fastest GPU memory tier.
    glBufferData(GL_ARRAY_BUFFER, sizeof(kQuad), kQuad.data(), GL_STATIC_DRAW);
    // Compile-time check: QuadVertex must be exactly 4 floats with no padding,
    // otherwise glVertexAttribPointer would compute wrong byte offsets and
    // corrupt the geometry silently.
    static_assert(sizeof(QuadVertex) == 4 * sizeof(float), "QuadVertex layout mismatch");
    // Unbind after upload
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    CHECK_GL("FullScreenQuad::init");
    return true;
}

void FullScreenQuad::draw() const {
    // --- Vertex layout description ---
    // Tell the GPU how to unpack bytes from the VBO into vertex shader attributes.
    // glVertexAttribPointer args: (slot, num_floats, type, normalized, stride, byte_offset)
    //   slot        — must match layout(location = N) in the vertex shader
    //   num_floats  — how many floats to read per vertex for this attribute
    //   stride      — bytes between the start of one vertex and the next (= sizeof(QuadVertex))
    //   byte_offset — where in each vertex this attribute starts
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glEnableVertexAttribArray(0);  // activate slot 0 (off by default)
    // Slot 0 = aPosition: starts at byte 0 (field x), reads 2 floats (x, y)
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, x)));
    glEnableVertexAttribArray(1);  // activate slot 1 (off by default)
    // Slot 1 = aTexCoord: starts at byte 8 (field u, after x and y), reads 2 floats (u, v)
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, u)));

    // --- Draw call ---
    // GPU reads 4 vertices from the VBO, runs the vertex shader once per corner,
    // rasterizes 2 triangles covering the full target, then runs the fragment
    // shader once per covered pixel.
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // --- Cleanup ---
    // Disable the attribute slots and unbinding the buffer.
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void FullScreenQuad::destroy() {
    // The 0-check guards against double-free if destroy() is called more than
    // once. Resetting to 0 marks the handle invalid — 0 is GL's null/sentinel.
    if (vbo_ != 0) {
        glDeleteBuffers(1, &vbo_);
        vbo_ = 0;
    }
}

}  // namespace forge