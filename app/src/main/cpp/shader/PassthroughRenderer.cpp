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

static constexpr std::string_view kVertSrc = R"GLSL(
#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
uniform mat4 uTexMatrix;
uniform vec2 uCropScale;
uniform vec2 uCropOffset;
out vec2 vTexCoord;
void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vec2 uv = aTexCoord * uCropScale + uCropOffset;
    vTexCoord = (uTexMatrix * vec4(uv, 0.0, 1.0)).xy;
}
)GLSL";

static constexpr std::string_view kFragSrc = R"GLSL(
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
in vec2 vTexCoord;
uniform samplerExternalOES uTexture;
out vec4 fragColor;
void main() {
    fragColor = texture(uTexture, vTexCoord);
}
)GLSL";

struct QuadVertex {
    float x, y;
    float u, v;
};

static constexpr std::array<QuadVertex, 4> kQuad = {
        {
                {-1.0f, -1.0f, 0.0f, 0.0f},
                {1.0f, -1.0f, 1.0f, 0.0f},
                {-1.0f, 1.0f, 0.0f, 1.0f},
                {1.0f, 1.0f, 1.0f, 1.0f},
        }
};

bool PassthroughRenderer::init(GLuint oesTextureId) {
    oesTexId_ = oesTextureId;

    GLuint vert = compileShader(GL_VERTEX_SHADER, kVertSrc);
    GLuint frag = compileShader(GL_FRAGMENT_SHADER, kFragSrc);
    if (vert == 0 || frag == 0) {
        return false;
    }

    program_ = linkProgram(vert, frag);
    if (program_ == 0) {
        return false;
    }

    uTexMatrix_ = glGetUniformLocation(program_, "uTexMatrix");
    uTexture_ = glGetUniformLocation(program_, "uTexture");
    uCropScale_ = glGetUniformLocation(program_, "uCropScale");
    uCropOffset_ = glGetUniformLocation(program_, "uCropOffset");

    glGenBuffers(1, &vbo_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferData(GL_ARRAY_BUFFER, sizeof(kQuad), kQuad.data(), GL_STATIC_DRAW);
    static_assert(sizeof(QuadVertex) == 4 * sizeof(float), "QuadVertex layout mismatch");
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    LOGI("initialized");
    return true;
}

void PassthroughRenderer::setViewport(int camW, int camH, int surfW, int surfH) {
    float scaleW = static_cast<float>(surfW) / static_cast<float>(camW);
    float scaleH = static_cast<float>(surfH) / static_cast<float>(camH);
    float renderScale = std::max(scaleW, scaleH);  // center crop: zoom to fill
    cropScaleX_ = scaleW / renderScale;
    cropScaleY_ = scaleH / renderScale;
    cropOffsetX_ = (1.0f - cropScaleX_) * 0.5f;
    cropOffsetY_ = (1.0f - cropScaleY_) * 0.5f;
    LOGI("viewport set: cam=%dx%d surf=%dx%d cropScale=(%.3f,%.3f)",
         camW, camH, surfW, surfH, cropScaleX_, cropScaleY_);
}

void PassthroughRenderer::draw(const float *texMatrix4x4) const {
    glUseProgram(program_);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId_);
    glUniform1i(uTexture_, 0);
    glUniformMatrix4fv(uTexMatrix_, 1, GL_FALSE, texMatrix4x4);
    glUniform2f(uCropScale_, cropScaleX_, cropScaleY_);
    glUniform2f(uCropOffset_, cropOffsetX_, cropOffsetY_);

    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, x)));
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(QuadVertex),
                          reinterpret_cast<const void *>(offsetof(QuadVertex, u)));

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

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
