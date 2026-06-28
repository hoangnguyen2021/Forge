#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "absl/types/span.h"
#include "litert/cc/litert_buffer_ref.h"
#include "litert/cc/litert_common.h"
#include "litert/cc/litert_compiled_model.h"
#include "litert/cc/litert_environment.h"
#include "litert/cc/litert_tensor_buffer.h"

#define LOG_TAG "LiteRtSmoke"
#include "../Log.h"

/*
 * Throwaway smoke test for the LiteRT C++ integration (Milestone 2a). It builds a
 * CPU CompiledModel straight from the in-memory model bytes, runs one inference on a
 * dummy input, and reports the output tensor shape. The point is only to prove the
 * vendored runtime actually executes end to end — no camera, no GL. Delete this file
 * (and its Kotlin caller + CMake entry) once the real segmenter lands.
 */

namespace {

// Pull an Expected's error message into a std::string, whether Message() returns a
// std::string or an absl::string_view (both expose data()/size()).
template <typename ExpectedT>
std::string errorOf(const ExpectedT& e) {
    auto msg = e.Error().Message();
    return std::string(msg.data(), msg.size());
}

std::string runSmokeTest(const uint8_t* modelData, size_t modelSize) {
    auto env = litert::Environment::Create({});
    if (!env) return "Environment::Create failed: " + errorOf(env);

    // The model lives in the APK; feed its bytes directly rather than via a file path.
    litert::BufferRef<uint8_t> modelBuf(modelData, modelSize);
    auto compiled = litert::CompiledModel::Create(*env, modelBuf, litert::HwAccelerators::kCpu);
    if (!compiled) return "CompiledModel::Create failed: " + errorOf(compiled);

    auto inputs = compiled->CreateInputBuffers(0);
    if (!inputs) return "CreateInputBuffers failed: " + errorOf(inputs);

    // Derive the input element count from its tensor type and fill with a constant —
    // just enough to give Run() well-formed data.
    auto inType = (*inputs)[0].TensorType();
    if (!inType) return "input TensorType failed: " + errorOf(inType);
    const auto& inLayout = inType->Layout();
    size_t inElems = 1;
    for (size_t d = 0; d < inLayout.Rank(); ++d) inElems *= inLayout.Dimensions()[d];
    std::vector<float> dummy(inElems, 0.5f);
    auto wrote = (*inputs)[0].Write<float>(absl::MakeConstSpan(dummy));
    if (!wrote) return "input Write failed: " + errorOf(wrote);

    auto outputs = compiled->CreateOutputBuffers(0);
    if (!outputs) return "CreateOutputBuffers failed: " + errorOf(outputs);

    // static_cast: the literal 0 is ambiguous between the size_t (index) and
    // string_view (signature key) Run() overloads.
    auto status = compiled->Run(static_cast<size_t>(0), *inputs, *outputs);
    if (!status) return "Run failed: " + errorOf(status);

    // Report the output shape (expect [1, 256, 256, 6] for selfie_multiclass).
    auto outType = (*outputs)[0].TensorType();
    if (!outType) return "output TensorType failed: " + errorOf(outType);
    const auto& outLayout = outType->Layout();
    std::string shape = "[";
    for (size_t d = 0; d < outLayout.Rank(); ++d) {
        if (d) shape += ", ";
        shape += std::to_string(outLayout.Dimensions()[d]);
    }
    shape += "]";
    return "OK: inference ran on CPU, output shape = " + shape;
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_app_honguyen_forge_ml_LiteRtSmokeTest_nativeRun(JNIEnv* env, jobject, jbyteArray modelBytes) {
    const jsize len = env->GetArrayLength(modelBytes);
    jbyte* bytes = env->GetByteArrayElements(modelBytes, nullptr);
    std::string result =
        runSmokeTest(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(modelBytes, bytes, JNI_ABORT);  // read-only; discard the copy
    LOGI("%s", result.c_str());
    return env->NewStringUTF(result.c_str());
}
