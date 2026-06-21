#pragma once

#include <GLES3/gl3.h>

namespace forge {

// An offscreen render target: an RGBA8 color texture attached to a framebuffer
// object (FBO). A pass renders into it via bind(); a later pass samples its
// output via textureId(). One FrameBuffer per stage in the render graph (scene,
// blur ping-pong, ...) — this is what lets effects and compositing sit between
// the camera and the screen instead of drawing straight to the window.
//
// Lifecycle: ensureSize() (re)allocates GL storage and requires a current GL
// context; the destructor frees the FBO and texture, so the owner must release
// it on the GL thread while the context is current (RenderEngine does so in
// surfaceDestroyed, before the EGL context is torn down).
class FrameBuffer {
public:
    FrameBuffer() = default;
    ~FrameBuffer() { destroy(); }

    FrameBuffer(const FrameBuffer&)            = delete;
    FrameBuffer& operator=(const FrameBuffer&) = delete;

    // Allocate or resize the target to w x h (RGBA8). No-op if already that size.
    // Returns false if the resulting framebuffer is incomplete. Safe to call on
    // every setViewport — only re-specs storage when the size actually changes.
    bool ensureSize(int w, int h);

    // True once ensureSize has allocated a complete framebuffer.
    bool ready() const { return fbo_ != 0; }

    // Bind as the current draw target and set the viewport to its full size.
    void bind() const;

    // The color texture other passes sample from.
    GLuint textureId() const { return textureId_; }

    void destroy();

private:
    GLuint fbo_     = 0;
    GLuint textureId_ = 0;
    int width_      = 0;
    int height_     = 0;
};

}  // namespace forge