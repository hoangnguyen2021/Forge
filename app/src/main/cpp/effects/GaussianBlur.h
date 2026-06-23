#pragma once

#include <string_view>

namespace forge {

/*
 * Fragment shaders for a separable Gaussian blur, ready to hand to EffectPass::init.
 * These are effect data, not engine logic — RenderEngine includes this and wires the
 * two passes into the chain, but knows nothing about how the blur works.
 *
 * Separable blur splits a 2D Gaussian into a horizontal then a vertical 1D pass. The
 * result is identical to a full 2D kernel, but costs 2*N texture taps per pixel instead
 * of N*N — the standard separable-convolution optimization, and why the chain runs two
 * passes. Each pass reads 9 taps (centre + four neighbours per side) along its axis,
 * weighted by a fixed kernel (kWeight) that sums to 1 so the image keeps its brightness.
 * uTexelSize turns a "one pixel" step into UV space; the H shader walks along x, the V
 * along y. Both reuse EffectPass's shared passthrough vertex stage.
 */

// Horizontal half of the separable Gaussian: steps along x (uTexelSize.x).
inline constexpr std::string_view kBlurHFragSrc = R"GLSL(
    #version 300 es
    precision mediump float;     // medium float precision, the usual mobile default for color math
    in vec2 vTexCoord;           // interpolated UV from the vertex shader, 0..1
    uniform sampler2D uTexture;  // the input image to blur (previous pass's output)
    uniform vec2 uTexelSize;     // size of one texel in UV space (1/width, 1/height)
    out vec4 fragColor;          // the blurred color written for this pixel

    // Gaussian weights for the centre tap and the four neighbours on each side. The
    // kernel is symmetric, so one array serves both sides; the weights sum to 1.0
    // (centre + 2 * the other four) to preserve overall brightness.
    const float kWeight[5] = float[](0.2270270270, 0.1945945946, 0.1216216216,
                                     0.0540540541, 0.0162162162);

    void main() {
        // Centre tap first, then step outward along x, adding the mirrored pair of
        // neighbours at each distance with the same weight.
        vec3 acc = texture(uTexture, vTexCoord).rgb * kWeight[0];
        for (int i = 1; i < 5; i++) {
            vec2 off = vec2(float(i) * uTexelSize.x, 0.0);
            acc += texture(uTexture, vTexCoord + off).rgb * kWeight[i];
            acc += texture(uTexture, vTexCoord - off).rgb * kWeight[i];
        }
        fragColor = vec4(acc, 1.0);
    }
)GLSL";

// Vertical half of the separable Gaussian: steps along y (uTexelSize.y), blurring the
// already horizontally-blurred image into the final 2D result.
inline constexpr std::string_view kBlurVFragSrc = R"GLSL(
    #version 300 es
    precision mediump float;     // medium float precision, the usual mobile default for color math
    in vec2 vTexCoord;           // interpolated UV from the vertex shader, 0..1
    uniform sampler2D uTexture;  // the horizontally-blurred image (previous pass's output)
    uniform vec2 uTexelSize;     // size of one texel in UV space (1/width, 1/height)
    out vec4 fragColor;          // the blurred color written for this pixel

    const float kWeight[5] = float[](0.2270270270, 0.1945945946, 0.1216216216,
                                     0.0540540541, 0.0162162162);

    void main() {
        vec3 acc = texture(uTexture, vTexCoord).rgb * kWeight[0];
        for (int i = 1; i < 5; i++) {
            vec2 off = vec2(0.0, float(i) * uTexelSize.y);
            acc += texture(uTexture, vTexCoord + off).rgb * kWeight[i];
            acc += texture(uTexture, vTexCoord - off).rgb * kWeight[i];
        }
        fragColor = vec4(acc, 1.0);
    }
)GLSL";

}  // namespace forge
