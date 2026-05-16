#include "ShaderProgram.h"
#include <android/log.h>
#include <vector>

#define TAG "ShaderProgram"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Shaders are small programs written in GLSL that run on the GPU, not the CPU.
// Every draw call needs two: a vertex shader (runs once per vertex — computes screen position)
// and a fragment shader (runs once per pixel — decides the output color).
// GLSL source is just a string at C++ compile time; the GPU driver compiles it at runtime.
GLuint compileShader(GLenum type, std::string_view src) {
    // Reserve a shader slot in the driver. 'type' is GL_VERTEX_SHADER or GL_FRAGMENT_SHADER.
    GLuint shader = glCreateShader(type);
    const char *srcPtr = src.data();
    auto srcLen = static_cast<GLint>(src.size());
    // Hand the GLSL source string to the driver (the GPU doesn't read your memory directly).
    glShaderSource(shader, 1, &srcPtr, &srcLen);
    // Ask the driver to compile GLSL → GPU machine code, just like clang compiling a .cpp file.
    glCompileShader(shader);

    // GL never throws — you must explicitly ask whether the last operation succeeded.
    GLint status = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        // Fetch the compiler error message (syntax errors, type mismatches, etc.) for logging.
        GLint logLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLen);
        std::vector<char> log(logLen);
        glGetShaderInfoLog(shader, logLen, nullptr, log.data());
        LOGE("Shader compile error: %s", log.data());
        glDeleteShader(shader);
        return 0;  // caller checks for 0 to detect failure
    }
    return shader;
}

// Linking combines the compiled vertex + fragment shaders into one GPU executable (a "program").
// The analogy to C++ is exact: compile each .cpp → .o, then link the .o files into a binary.
// Once linked, you activate the program with glUseProgram() before issuing draw calls.
GLuint linkProgram(GLuint vert, GLuint frag) {
    // Create the program container that will hold both shaders after linking.
    GLuint program = glCreateProgram();
    // Attach the two compiled shader objects — like passing .o files to the linker.
    glAttachShader(program, vert);
    glAttachShader(program, frag);
    // Link: resolves the interface between vertex outputs and fragment inputs, validates types.
    glLinkProgram(program);

    GLint status = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status == GL_FALSE) {
        // Linker errors appear here, e.g. a varying output in the vertex shader that the
        // fragment shader doesn't declare, or mismatched types across the boundary.
        GLint logLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLen);
        std::vector<char> log(logLen);
        glGetProgramInfoLog(program, logLen, nullptr, log.data());
        LOGE("Program link error: %s", log.data());
        glDeleteProgram(program);
        program = 0;
    }
    // The linked program has its own copy of the compiled code; the shader objects are now
    // just intermediate artefacts (like .o files after linking) and can be freed.
    glDeleteShader(vert);
    glDeleteShader(frag);
    return program;
}
