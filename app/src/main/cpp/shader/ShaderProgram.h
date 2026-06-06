#pragma once

#include <GLES3/gl3.h>
#include <string_view>

namespace forge {

GLuint compileShader(GLenum type, std::string_view src);

GLuint linkProgram(GLuint vert, GLuint frag);

}  // namespace forge
