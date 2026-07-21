#pragma once

#include <GLES3/gl3.h>

#include <cstdint>

namespace forge {

/*
 * Per-pass GPU timing via EXT_disjoint_timer_query. CPU tracing (see trace/Trace.h) only
 * measures the cost of *issuing* GL commands; the GPU runs them asynchronously, so the
 * real cost of a pass is invisible there. This wraps each pass in a GL_TIME_ELAPSED query,
 * which the GPU stamps with the actual nanoseconds it spent.
 *
 * Two hardware rules shape the design:
 *  - TIME_ELAPSED queries cannot nest, so passes are timed sequentially (begin A, end A,
 *    begin B, ...) — never an outer query wrapping inner ones.
 *  - A result isn't ready until the GPU finishes, so reading it the same frame would stall
 *    the pipeline. Instead the queries are triple-buffered: frame N issues into one slot,
 *    and we harvest that slot's results kRing frames later, by when the GPU is done.
 *
 * The extension is optional. If unavailable, init() returns false and begin/end become
 * no-ops, so callers never have to branch on support.
 */
class GpuTimer {
public:
    // The passes we time. Count must stay last — it doubles as the array size.
    enum class Zone { Camera, Effects, Composite, Count };

    // Loads the extension entry points and allocates the query objects. Requires a current
    // GL context. Returns false (and leaves the timer disabled) if the GPU lacks the
    // extension; that is not a fatal error.
    bool init();
    void destroy();
    bool available() const { return available_; }

    // Call once at the top of each frame: harvests an earlier frame's results and checks
    // the disjoint flag (a throttle/context-switch invalidates in-flight timings).
    void beginFrame();

    // Time one pass. Calls must balance and must not overlap another zone (no nesting).
    void begin(Zone zone);
    void end(Zone zone);

    // Most recent GPU time harvested for a zone, in milliseconds (0 until the first
    // result lands, kRing frames after the zone is first timed).
    double lastMs(Zone zone) const;

private:
    static constexpr int kRing  = 3;
    static constexpr int kZones = static_cast<int>(Zone::Count);

    bool     available_   = false;
    uint32_t frame_       = 0;
    int      currentSlot_ = 0;
    Zone     active_      = Zone::Count;  // zone whose query is open, or Count if none

    GLuint queries_[kRing][kZones] = {};
    bool   issued_[kRing][kZones]  = {};
    double lastMs_[kZones]         = {};
};

// RAII: times the enclosing scope as `zone`. Drop one right after FORGE_TRACE in a pass
// block so the CPU slice and GPU timing cover the same work.
class GpuZone {
public:
    GpuZone(GpuTimer& timer, GpuTimer::Zone zone) : timer_(timer), zone_(zone) {
        timer_.begin(zone_);
    }
    ~GpuZone() { timer_.end(zone_); }

    GpuZone(const GpuZone&)            = delete;
    GpuZone& operator=(const GpuZone&) = delete;

private:
    GpuTimer&      timer_;
    GpuTimer::Zone zone_;
};

}  // namespace forge
