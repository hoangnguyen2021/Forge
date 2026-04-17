#pragma once

#include <GLES3/gl3.h>

class PassthroughRenderer {
public:
    ~PassthroughRenderer() { destroy(); }

    bool init(GLuint oesTextureId);

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void draw(const float* texMatrix4x4) const;

    void destroy();

private:
    GLuint program_ = 0;
    GLuint vbo_ = 0;
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
