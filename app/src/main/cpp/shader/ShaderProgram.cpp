#include "ShaderProgram.h"
#include <android/log.h>
#include <vector>

#define TAG "ShaderProgram"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

GLuint compileShader(GLenum type, std::string_view src) {
    GLuint shader = glCreateShader(type);
    const char *srcPtr = src.data();
    auto srcLen = static_cast<GLint>(src.size());
    glShaderSource(shader, 1, &srcPtr, &srcLen);
    glCompileShader(shader);

    GLint status = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        GLint logLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLen);
        std::vector<char> log(logLen);
        glGetShaderInfoLog(shader, logLen, nullptr, log.data());
        LOGE("Shader compile error: %s", log.data());
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint linkProgram(GLuint vert, GLuint frag) {
    GLuint program = glCreateProgram();
    glAttachShader(program, vert);
    glAttachShader(program, frag);
    glLinkProgram(program);

    GLint status = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status == GL_FALSE) {
        GLint logLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLen);
        std::vector<char> log(logLen);
        glGetProgramInfoLog(program, logLen, nullptr, log.data());
        LOGE("Program link error: %s", log.data());
        glDeleteProgram(program);
        program = 0;
    }
    // Program retains shader objects after linking; deleting here frees the compiler artefacts.
    glDeleteShader(vert);
    glDeleteShader(frag);
    return program;
}
