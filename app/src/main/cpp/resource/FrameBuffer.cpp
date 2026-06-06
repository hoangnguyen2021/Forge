#include "FrameBuffer.h"

#include "../CheckGl.h"

#define LOG_TAG "FrameBuffer"
#include "../Log.h"

namespace forge {

bool FrameBuffer::ensureSize(int w, int h) {
    // Nothing to do if we already have a framebuffer at this exact size.
    if (fbo_ != 0 && w == width_ && h == height_) {
        return true;
    }
    // First allocation or a size change (e.g. rotation): drop any existing
    // objects and rebuild. Recreating is simpler than re-specifying storage and
    // happens rarely — never per frame.
    destroy();

    // Color texture the pass renders into and later passes sample from.
    glGenTextures(1, &texture_);
    glBindTexture(GL_TEXTURE_2D, texture_);
    // RGBA8 storage with no initial pixel data (nullptr) — the GPU allocates the
    // memory and a pass fills it by rendering. The internal format GL_RGBA8 is
    // what matters; GL_RGBA/GL_UNSIGNED_BYTE just describe the (absent) upload.
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    // Linear filtering so sampling this texture in a later pass scales smoothly.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // CLAMP_TO_EDGE so a blur sampling past the edge repeats the border pixel
    // rather than wrapping around to the opposite side.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);

    // Framebuffer object: a render target that draws into the texture above
    // instead of the screen. Attaching the texture to COLOR_ATTACHMENT0 wires
    // "fragment shader output 0 writes here".
    glGenFramebuffers(1, &fbo_);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture_, 0);

    // A framebuffer can be incomplete (an unsupported size/format combination);
    // rendering to an incomplete FBO is undefined, so verify before trusting it.
    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        LOGE("framebuffer incomplete: 0x%x", status);
        destroy();
        return false;
    }

    width_  = w;
    height_ = h;
    CHECK_GL("FrameBuffer::ensureSize");
    return true;
}

void FrameBuffer::bind() const {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
    // Each render target carries its own viewport; cover the whole texture.
    glViewport(0, 0, width_, height_);
}

void FrameBuffer::destroy() {
    if (fbo_ != 0) {
        glDeleteFramebuffers(1, &fbo_);
        fbo_ = 0;
    }
    if (texture_ != 0) {
        glDeleteTextures(1, &texture_);
        texture_ = 0;
    }
    width_  = 0;
    height_ = 0;
}

}  // namespace forge