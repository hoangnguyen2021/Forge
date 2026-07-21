#include "Segmenter.h"

#include <android/asset_manager.h>

#include <algorithm>
#include <cmath>
#include <cstring>
#include <string>
#include <utility>
#include <vector>

#include "absl/types/span.h"
#include "litert/cc/litert_buffer_ref.h"
#include "litert/cc/litert_common.h"
#include "litert/cc/litert_compiled_model.h"
#include "litert/cc/litert_environment.h"
#include "litert/cc/litert_tensor_buffer.h"

#define LOG_TAG "Segmenter"
#include "../Log.h"

namespace forge {

namespace {

// Model geometry (selfie_multiclass_256x256): RGB float in, 6-class float out, HWC.
constexpr int kPixels          = Segmenter::kSize * Segmenter::kSize;  // 65536
constexpr int kClasses         = 6;  // output channels per pixel
constexpr int kBackgroundClass = 0;  // class 0 = background; 1..5 are person parts

// Pull an Expected's error message into a std::string for logging (Message() may be
// a std::string or absl::string_view; both expose data()/size()).
template <typename ExpectedT>
std::string errorOf(const ExpectedT& e) {
    auto msg = e.Error().Message();
    return std::string(msg.data(), msg.size());
}

}  // namespace

Segmenter::Segmenter() = default;

Segmenter::~Segmenter() {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        stop_ = true;
    }
    cv_.notify_all();
    if (worker_.joinable()) {
        worker_.join();  // waits out at most one in-flight inference
    }
}

bool Segmenter::init(AAssetManager* assets, const char* modelAsset) {
    // Read the model bytes here (cheap file I/O). The LiteRT objects themselves are
    // built on the worker thread so all model use is single-threaded (see workerLoop).
    AAsset* asset = AAssetManager_open(assets, modelAsset, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        LOGE("model asset not found: %s", modelAsset);
        return false;
    }
    const off_t size  = AAsset_getLength(asset);
    const void* bytes = AAsset_getBuffer(asset);
    if (bytes == nullptr || size <= 0) {
        LOGE("model asset unreadable: %s", modelAsset);
        AAsset_close(asset);
        return false;
    }
    modelBytes_.assign(static_cast<const uint8_t*>(bytes),
                       static_cast<const uint8_t*>(bytes) + size);
    AAsset_close(asset);

    input_.assign(static_cast<size_t>(kPixels) * 4, 0);
    mask_.assign(kPixels, 255);

    worker_ = std::thread(&Segmenter::workerLoop, this);
    return true;
}

bool Segmenter::wantsFrame() {
    std::lock_guard<std::mutex> lock(mutex_);
    return ready_ && !hasInput_;
}

void Segmenter::submit(const uint8_t* rgba) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (!ready_) return;
        std::memcpy(input_.data(), rgba, static_cast<size_t>(kPixels) * 4);
        hasInput_ = true;
    }
    cv_.notify_one();
}

bool Segmenter::fetchMask(uint8_t* out) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!hasMask_) return false;
    std::memcpy(out, mask_.data(), kPixels);
    hasMask_ = false;
    return true;
}

void Segmenter::workerLoop() {
    // --- one-time setup, on this thread (LiteRT is single-thread-affine) ---
    auto env = litert::Environment::Create({});
    if (!env) {
        LOGE("Environment::Create failed: %s", errorOf(env).c_str());
        return;  // leaves the segmenter inert; blur falls back to the cold-start mask
    }
    litert::BufferRef<uint8_t> modelBuf(modelBytes_.data(), modelBytes_.size());
    auto compiled = litert::CompiledModel::Create(*env, modelBuf, litert::HwAccelerators::kCpu);
    if (!compiled) {
        LOGE("CompiledModel::Create failed: %s", errorOf(compiled).c_str());
        return;
    }
    // Move the wrappers into members so they (and the handles they own) outlive the
    // loop; modelBytes_ stays alive as the model's backing buffer.
    env_   = std::make_unique<litert::Environment>(std::move(*env));
    model_ = std::make_unique<litert::CompiledModel>(std::move(*compiled));

    auto inBufs  = model_->CreateInputBuffers();
    auto outBufs = model_->CreateOutputBuffers();
    if (!inBufs || !outBufs) {
        LOGE("tensor buffer allocation failed");
        return;
    }

    // Reusable worker-local buffers (avoid per-frame allocation).
    std::vector<uint8_t> frame(static_cast<size_t>(kPixels) * 4);  // swapped out of input_
    std::vector<float> inputF(static_cast<size_t>(kPixels) * 3);   // normalized RGB
    std::vector<float> output(static_cast<size_t>(kPixels) * kClasses);
    std::vector<uint8_t> localMask(kPixels);

    {
        std::lock_guard<std::mutex> lock(mutex_);
        ready_ = true;
    }
    LOGI("segmenter ready");

    for (;;) {
        {
            std::unique_lock<std::mutex> lock(mutex_);
            cv_.wait(lock, [this] { return hasInput_ || stop_; });
            if (stop_) return;
            std::swap(input_, frame);  // O(1): take the freshest frame, free input_ for the next
            hasInput_ = false;
        }

        // --- preprocess + inference, NO lock held (16-30ms must not block submit/fetch) ---
        // RGBA8 -> RGB float in [0,1], dropping alpha. NOTE: `frame` comes from
        // glReadPixels (origin bottom-left), so it is vertically flipped vs a top-down
        // image. Segmentation is orientation-tolerant and the mask realigns on upload
        // (glTexSubImage2D shares the bottom-up convention), so no flip is needed here.
        // If accuracy suffers, feed upright by flipping the source row here and the
        // mask row below. [0,1] vs [-1,1] normalization is the other on-device knob.
        for (int i = 0; i < kPixels; ++i) {
            inputF[i * 3 + 0] = static_cast<float>(frame[i * 4 + 0]) * (1.0f / 255.0f);
            inputF[i * 3 + 1] = static_cast<float>(frame[i * 4 + 1]) * (1.0f / 255.0f);
            inputF[i * 3 + 2] = static_cast<float>(frame[i * 4 + 2]) * (1.0f / 255.0f);
        }

        if (auto w = (*inBufs)[0].Write<float>(absl::MakeConstSpan(inputF)); !w) {
            LOGE("input write failed: %s", errorOf(w).c_str());
            continue;
        }
        if (auto r = model_->Run(static_cast<size_t>(0), *inBufs, *outBufs); !r) {
            LOGE("Run failed: %s", errorOf(r).c_str());
            continue;
        }
        if (auto r = (*outBufs)[0].Read(absl::MakeSpan(output)); !r) {
            LOGE("output read failed: %s", errorOf(r).c_str());
            continue;
        }

        // Reduce the 6 per-pixel class scores to one foreground probability via softmax:
        // foreground = 1 - P(background). Soft values give smooth blur edges.
        for (int p = 0; p < kPixels; ++p) {
            const float* s = &output[static_cast<size_t>(p) * kClasses];
            float maxLogit = s[0];
            for (int c = 1; c < kClasses; ++c) maxLogit = std::max(maxLogit, s[c]);
            float sum = 0.0f;
            for (int c = 0; c < kClasses; ++c) sum += std::exp(s[c] - maxLogit);
            const float pBg = std::exp(s[kBackgroundClass] - maxLogit) / sum;
            const float fg  = 1.0f - pBg;
            localMask[p] = static_cast<uint8_t>(fg * 255.0f + 0.5f);
        }

        {
            std::lock_guard<std::mutex> lock(mutex_);
            std::swap(mask_, localMask);  // publish; localMask reused next iteration
            hasMask_ = true;
        }
    }
}

}  // namespace forge
