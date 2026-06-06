#pragma once

#include <GLES3/gl3.h>

namespace forge {

// A full-screen quad shared by every render pass (camera, blur, present).
//
// All passes draw the same two-triangle strip covering clip space; only their
// shader programs differ. Centralizing the geometry here means the VBO is
// uploaded once and reused, instead of each pass allocating its own copy.
//
// The vertex attribute layout is fixed: every pass's vertex shader pins
//   layout(location = 0) in vec2 aPosition;   // NDC corner (-1..1)
//   layout(location = 1) in vec2 aTexCoord;   // UV into the source (0..1)
// so draw() can describe and enable both attributes without knowing which
// program is currently bound.
//
// Lifecycle: init() requires a current GL context; the destructor frees the
// VBO, so the owner must ensure the context is still current at destruction.
class FullScreenQuad {
public:
    FullScreenQuad() = default;
    ~FullScreenQuad() { destroy(); }

    // Owns a GL buffer — non-copyable. Instances live in unique_ptr, so the
    // pointer moves and the object itself never needs to.
    FullScreenQuad(const FullScreenQuad&)            = delete;
    FullScreenQuad& operator=(const FullScreenQuad&) = delete;

    // Upload the quad geometry to a VBO. Returns false if the buffer could not
    // be created. Must run on the GL thread with a current context.
    bool init();

    // Bind the VBO, enable both attributes, and issue the draw call. Assumes a
    // shader program and any uniforms/textures are already bound by the caller.
    void draw() const;

    void destroy();

private:
    GLuint vbo_ = 0;
};

}  // namespace forge