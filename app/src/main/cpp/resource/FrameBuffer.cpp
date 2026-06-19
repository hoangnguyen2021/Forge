#include "FrameBuffer.h"

#include "../CheckGl.h"

#define LOG_TAG "FrameBuffer"
#include "../Log.h"

namespace forge {

bool FrameBuffer::ensureSize(int w, int h) {
    // Nothing to do if we already have a framebuffer of exactly this size.
    if (fbo_ != 0 && w == width_ && h == height_) {
        return true;
    }
    // First allocation or a size change (e.g. entering split-screen): drop any
    // existing objects and rebuild. Re-creating is simpler than re-specifying storage
    // and happens rarely — never per frame.
    destroy();

    // The color texture that passes render into and later passes sample from. Created
    // like any GL texture; see RenderEngine::createOesTexture for the filter/wrap
    // parameters below — identical reasoning, just a plain 2D target instead of OES.
    glGenTextures(1, &texture_);
    glBindTexture(GL_TEXTURE_2D, texture_);
    // glTexImage2D allocates the storage. Internal format GL_RGBA8 = how the GPU
    // stores each texel (8 bits/channel); GL_RGBA + GL_UNSIGNED_BYTE describe the
    // upload format. nullptr = allocate but leave the contents undefined; a pass fills it.
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);

    // Create the FBO (the off-screen render target, see the header) and wire our
    // texture in as its color output: fragment-shader writes to layout(location = 0)
    // now land in texture_ instead of the screen. The last arg is mip level 0.
    glGenFramebuffers(1, &fbo_);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture_, 0);

    // An FBO can be "incomplete" (an unsupported size/format combination); rendering
    // to an incomplete FBO is undefined, so verify before trusting it.
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
    // Make this FBO the active draw target, so following draws render into its texture.
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
