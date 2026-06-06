#pragma once

#include <GLES3/gl3.h>

class FullScreenQuad;

class PassthroughRenderer {
public:
    ~PassthroughRenderer() { destroy(); }

    // quad is owned by the caller (RenderEngine) and shared across passes; it
    // must outlive this renderer and stay valid for every draw() call.
    bool init(GLuint oesTextureId, const FullScreenQuad* quad);

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void draw(const float* texMatrix4x4) const;

    void destroy();

private:
    const FullScreenQuad* quad_ = nullptr;
    GLuint program_ = 0;
    GLuint oesTexId_ = 0;
    GLint uTexMatrix_ = -1;
    GLint uTexture_ = -1;
    GLint uCropScale_ = -1;
    GLint uCropOffset_ = -1;
    float cropScaleX_ = 1.0f;
    float cropScaleY_ = 1.0f;
    float cropOffsetX_ = 0.0f;
    float cropOffsetY_ = 0.0f;
};
