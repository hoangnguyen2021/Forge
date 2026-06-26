#pragma once

#include <string_view>

namespace forge {

/*
 * Fragment shaders for a separable Gaussian blur, ready to hand to EffectPass::init.
 * These are effect data, not engine logic — RenderEngine includes this and wires the
 * two passes into the chain, but knows nothing about how the blur works.
 *
 * Separable blur splits a 2D Gaussian into a horizontal then a vertical 1D pass. The
 * result is identical to a full 2D kernel, but costs 2*N taps per pixel instead of N*N
 * — the standard separable-convolution optimization, and why the chain runs two passes.
 *
 * On top of that, this uses linear (bilinear) sampling to halve the taps. The underlying
 * kernel is still the 9-tap Gaussian (centre + four neighbours per side), but instead of
 * reading each neighbour texel separately, we exploit the GPU's hardware linear filter:
 * a single texture() read at a fractional offset *between* two adjacent texels returns
 * their hardware-blended average for free. By placing that sample at the weighted centre
 * of a texel pair, one read reproduces what used to be two reads + a multiply-add. So the
 * four neighbours per side collapse into two reads, and each pass drops from 9 taps to 5
 * (centre + two per side). kOffset holds the fractional sample distances (in texels) and
 * kWeight the combined pair weights; both are precomputed from the 9-tap kernel, and the
 * weights sum to 1.0 (centre + 2 * the other two) so the image keeps its brightness.
 *
 * Precondition: the input texture must be sampled GL_LINEAR, or the "between two texels"
 * read snaps to one texel and the blur is wrong. The ping-pong targets are GL_LINEAR
 * (see FrameBuffer::ensureSize). uTexelSize turns a one-texel step into UV space; the H
 * shader walks along x, the V along y. Both reuse EffectPass's shared passthrough vertex
 * stage.
 */

// Horizontal half of the separable Gaussian: steps along x (uTexelSize.x).
inline constexpr std::string_view kBlurHFragSrc = R"GLSL(
    #version 300 es
    precision mediump float;     // medium float precision, the usual mobile default for color math
    in vec2 vTexCoord;           // interpolated UV from the vertex shader, 0..1
    uniform sampler2D uTexture;  // the input image to blur (previous pass's output)
    uniform vec2 uTexelSize;     // size of one texel in UV space (1/width, 1/height)
    out vec4 fragColor;          // the blurred color written for this pixel

    // Linear-sampling taps derived from the 9-tap Gaussian. kOffset[0] is the centre (a
    // plain discrete tap); kOffset[1]/[2] each sit between a pair of the original
    // neighbours, so one linear read covers both. kWeight holds the combined pair weights
    // and sums to 1.0 (centre + 2 * the other two) to preserve brightness.
    const float kOffset[3] = float[](0.0, 1.3846153846, 3.2307692308);
    const float kWeight[3] = float[](0.2270270270, 0.3162162162, 0.0702702703);

    void main() {
        // Centre tap first, then step outward along x, adding the mirrored pair of
        // linear-blended neighbours at each fractional offset with the same weight.
        vec3 acc = texture(uTexture, vTexCoord).rgb * kWeight[0];
        for (int i = 1; i < 3; i++) {
            vec2 off = vec2(kOffset[i] * uTexelSize.x, 0.0);
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

    const float kOffset[3] = float[](0.0, 1.3846153846, 3.2307692308);
    const float kWeight[3] = float[](0.2270270270, 0.3162162162, 0.0702702703);

    void main() {
        vec3 acc = texture(uTexture, vTexCoord).rgb * kWeight[0];
        for (int i = 1; i < 3; i++) {
            vec2 off = vec2(0.0, kOffset[i] * uTexelSize.y);
            acc += texture(uTexture, vTexCoord + off).rgb * kWeight[i];
            acc += texture(uTexture, vTexCoord - off).rgb * kWeight[i];
        }
        fragColor = vec4(acc, 1.0);
    }
)GLSL";

}  // namespace forge
