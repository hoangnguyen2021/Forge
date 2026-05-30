#include "Engine.h"

#include <GLES2/gl2ext.h>
#include <android/log.h>

#include "../CheckGl.h"

#define TAG "Engine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

bool Engine::surfaceCreated(ANativeWindow* window) {
    egl_ = std::make_unique<EglContext>();
    if (!egl_->init(window)) {
        LOGE("EGL init failed");
        egl_.reset();
        return false;
    }
    return true;
}

GLuint Engine::createOesTexture() {
    GLuint texId = 0;
    glGenTextures(1, &texId);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

    renderer_ = std::make_unique<PassthroughRenderer>();
    if (!renderer_->init(texId)) {
        LOGE("PassthroughRenderer init failed");
        renderer_.reset();
        glDeleteTextures(1, &texId);
        return 0;
    }
    CHECK_GL("Engine::createOesTexture");
    return texId;
}

void Engine::setViewport(int camW, int camH, int surfW, int surfH) {
    if (renderer_) {
        renderer_->setViewport(camW, camH, surfW, surfH);
    }
}

void Engine::drawFrame(const float* texMatrix4x4) {
    if (!egl_ || !renderer_) {
        return;
    }
    glClear(GL_COLOR_BUFFER_BIT);
    renderer_->draw(texMatrix4x4);
    CHECK_GL("Engine::drawFrame");
    egl_->swapBuffers();
}

void Engine::surfaceDestroyed() {
    renderer_.reset();
    egl_.reset();
}