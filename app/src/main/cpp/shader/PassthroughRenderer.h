#pragma once

#include <GLES3/gl3.h>

namespace forge {

class FullScreenQuad;

/*
 * The head pass of the render graph: samples the camera's OES texture, applies the
 * per-frame SurfaceTexture transform (sensor orientation + HAL crop) and a
 * cover-style crop, and renders the upright, surface-filling frame into the first
 * offscreen target. See RenderEngine for the full pipeline.
 *
 * It is not a RenderPass: its input is an external OES texture plus a transform
 * matrix, not a plain 2D texture, so it sits at the head of the chain rather than
 * in the uniform effect list.
 */
class PassthroughRenderer {
public:
    PassthroughRenderer() = default;
    ~PassthroughRenderer() { destroy(); }

    PassthroughRenderer(const PassthroughRenderer&)            = delete;
    PassthroughRenderer& operator=(const PassthroughRenderer&) = delete;

    // quad is owned by the caller (RenderEngine) and shared across passes; it
    // must outlive this renderer and stay valid for every draw() call.
    bool init(GLuint oesTextureId, const FullScreenQuad* quad);

    void setViewport(int cameraPortraitW, int cameraPortraitH, int surfaceW, int surfaceH);

    void draw(const float* texMatrix4x4) const;

    void destroy();

private:
    const FullScreenQuad* quad_ = nullptr;
    GLuint program_             = 0;
    GLuint oesTextureId_        = 0;
    GLint uTexMatrix_           = -1;
    GLint uTexture_             = -1;
    GLint uCropScale_           = -1;
    GLint uCropOffset_          = -1;
    float cropScaleX_           = 1.0f;
    float cropScaleY_           = 1.0f;
    float cropOffsetX_          = 0.0f;
    float cropOffsetY_          = 0.0f;
};

}  // namespace forge
