#pragma once

#include <condition_variable>
#include <cstdint>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

struct AAssetManager;

namespace litert {
class Environment;
class CompiledModel;
}  // namespace litert

namespace forge {

/*
 * Runs person segmentation on camera frames, off the GL thread, to drive the
 * background-blur mask. The GL thread hands in downscaled RGBA frames; a worker
 * thread runs LiteRT inference (CPU) and reduces the model's 6-class output to a
 * single foreground-probability mask; the GL thread fetches the result and uploads
 * it. At most one inference is in flight — submit() overwrites the pending frame
 * instead of queueing, so the model always runs on the freshest frame and the mask
 * simply lags a few frames (imperceptible for a soft blur).
 *
 * This class only touches CPU buffers; every GL call stays on the caller's thread.
 * The LiteRT objects are both created AND used on the worker thread (they are
 * single-thread-affine), so init() only reads the model bytes and starts the worker;
 * the model is compiled on the worker's first iteration.
 */
class Segmenter {
public:
    // The model is square at this resolution (selfie_multiclass_256x256): RGBA in,
    // a single-channel foreground mask out. The GL thread downscales to this size.
    static constexpr int kSize = 256;

    // Both defined out-of-line in the .cpp: the unique_ptr<litert::...> members are
    // incomplete here, so the compiler-generated ctor/dtor (which must be able to destroy
    // them) can't be emitted at every include site.
    Segmenter();
    ~Segmenter();

    Segmenter(const Segmenter&)            = delete;
    Segmenter& operator=(const Segmenter&) = delete;

    // Read the model bytes from assets and start the worker thread (which compiles the
    // model). Returns false only if the asset itself can't be read; a later compile
    // failure on the worker just leaves the segmenter inert — wantsFrame()/fetchMask()
    // return false and blur falls back to whatever mask the caller last uploaded.
    bool init(AAssetManager* assets, const char* modelAsset);

    // True when the worker is idle and ready for a new frame. The GL thread checks
    // this before doing the (non-trivial) downscale + readback, so frames the worker
    // can't keep up with are never produced.
    bool wantsFrame();

    // Hand the worker the latest frame: kSize*kSize RGBA8. Non-blocking — copies the
    // bytes, marks a frame pending (replacing any not-yet-started one), and wakes the
    // worker. No-op if init() failed.
    void submit(const uint8_t* rgba);

    // If a newer mask is ready, copy it into out (kSize*kSize bytes, R8) and return
    // true; otherwise leave out untouched and return false. Called on the GL thread.
    bool fetchMask(uint8_t* out);

private:
    void workerLoop();

    bool ready_ = false;  // set by the worker once the model has compiled

    // Created and used only on the worker thread (LiteRT is single-thread-affine).
    // Held behind pointers so this header stays free of the heavy LiteRT/abseil
    // includes. modelBytes_ backs the compiled model and must outlive it.
    std::vector<uint8_t> modelBytes_;
    std::unique_ptr<litert::Environment> env_;
    std::unique_ptr<litert::CompiledModel> model_;

    // Producer/consumer state, all guarded by mutex_.
    std::mutex mutex_;
    std::condition_variable cv_;
    std::thread worker_;
    bool stop_     = false;  // set in the destructor to end the worker loop
    bool hasInput_ = false;  // a submitted frame is waiting for the worker
    bool hasMask_  = false;  // a fresh mask is waiting for the GL thread
    // The worker swaps input_ into a private buffer under the lock (O(1)), then runs
    // inference unlocked — so submit()/fetchMask() never wait on a 16-30ms Run().
    std::vector<uint8_t> input_;  // kSize*kSize*4 RGBA, latest submitted frame
    std::vector<uint8_t> mask_;   // kSize*kSize R8, latest produced foreground mask
};

}  // namespace forge
