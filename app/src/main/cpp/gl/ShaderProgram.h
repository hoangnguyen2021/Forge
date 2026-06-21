#pragma once

#include <GLES3/gl3.h>
#include <string_view>

namespace forge {

/*
 * Helpers shared by every pass for turning GLSL source into a runnable GPU program.
 * The .cpp is the canonical explanation of what "compile" and "link" mean for
 * shaders; passes call these instead of repeating the GL boilerplate.
 */

// Compile one GLSL source string into a shader object. Returns 0 on failure (the
// compiler error is logged).
GLuint compileShader(GLenum type, std::string_view src);

// Link a compiled vertex + fragment shader into one program. Deletes both shader
// objects (no longer needed after linking) and returns 0 on failure.
GLuint linkProgram(GLuint vert, GLuint frag);

}  // namespace forge
