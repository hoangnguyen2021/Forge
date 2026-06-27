#pragma once

#include <string_view>

namespace forge {

// Full-screen passthrough vertex stage shared by every 2D pass (EffectPass,
// PresentPass, CompositePass). The quad corners are already in NDC and the UVs are
// straight 0..1 — CameraPass baked in any sensor orientation and crop upstream — so
// this only forwards the position and texture coordinate to the fragment shader.
inline constexpr std::string_view kPassthroughVertSrc = R"GLSL(
    #version 300 es
    layout(location = 0) in vec2 aPosition;  // a quad corner in NDC, -1..1
    layout(location = 1) in vec2 aTexCoord;  // that corner's UV into the input texture, 0..1
    out vec2 vTexCoord;                      // UV forwarded to the fragment shader
    void main() {
        gl_Position = vec4(aPosition, 0.0, 1.0);
        vTexCoord = aTexCoord;
    }
)GLSL";

}  // namespace forge
