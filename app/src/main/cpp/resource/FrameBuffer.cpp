#include "FrameBuffer.h"

#include "../CheckGl.h"

#define LOG_TAG "FrameBuffer"
#include "../Log.h"

namespace forge {

bool FrameBuffer::ensureSize(int w, int h) {
    // No-op if framebuffer already has the exact size.
    if (fbo_ != 0 && w == width_ && h == height_) {
        return true;
    }
    // First allocation or a size change (e.g. entering split-screen): drop any
    // existing objects and rebuild. Recreating is simpler than re-specifying storage
    // and happens rarely — never per frame.
    destroy();

    // Color texture the pass renders into and later passes sample from.
    glGenTextures(1, &texture_);
    // GL_TEXTURE_2D: standard texture we fully control
    glBindTexture(GL_TEXTURE_2D, texture_);
    // GL_RGBA8: the internal format — how the GPU stores each texel (8 bits per channel).
    // GL_RGBA / GL_UNSIGNED_BYTE: describe the upload pixel layout..
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    // MIN_FILTER: GL_LINEAR blends the 4 nearest texels when the texture is shrunk.
    // MAG_FILTER: GL_LINEAR blends when the texture is stretched, avoiding blockiness.
    // GL_LINEAR: Linear filtering so sampling this texture in a later pass scales smoothly.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    // WRAP_S/T (S = horizontal axis, T = vertical axis) control what the GPU samples when a UV
    // coordinate falls outside [0,1].
    // CLAMP_TO_EDGE: pins out-of-range UVs to the nearest edge pixel — any floating-point rounding
    // past 1.0 gets the border pixel rather than wrapping to the opposite edge, which would bleed
    // the wrong side of the frame.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    // Unbind so subsequent unrelated GL calls don't accidentally mutate this texture.
    glBindTexture(GL_TEXTURE_2D, 0);


    // Allocate a framebuffer object handle.
    glGenFramebuffers(1, &fbo_);
    // Make fbo_ the active render target so the attachment call below applies to it.
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
    // COLOR_ATTACHMENT0: wires texture_ as the first (and only) color output — fragment
    // shader writes to layout(location = 0) land in texture_ instead of the screen.
    // The last arg is mip level 0, the only level we allocated.
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
    // Make fbo_ the active render target so the attachment call below applies to it.
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