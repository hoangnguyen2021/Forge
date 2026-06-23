#pragma once

#include <android/trace.h>

namespace forge {

/*
 * Lightweight scoped tracing for the render pipeline. Each TraceScope emits an ATrace
 * section for its lifetime, which appears as a slice on this thread's track in a
 * Perfetto (or systrace) capture — the foundation for measuring per-stage cost.
 *
 * This measures CPU time: the wall-clock spent *issuing* the work on this thread, not
 * the GPU time to execute it. GL draw calls only enqueue commands, so a pass's slice is
 * its command-submission cost; the GPU runs the work asynchronously, and the wait for it
 * usually surfaces as a stall inside swapBuffers. Real per-pass GPU cost needs timer
 * queries — a later phase. Prefer the FORGE_TRACE macro over constructing this directly.
 *
 * ATrace_beginSection/endSection are no-ops with a cheap atomic check when no tracer is
 * attached, so these are safe to leave in release builds.
 */
class TraceScope {
public:
    explicit TraceScope(const char* name) { ATrace_beginSection(name); }
    ~TraceScope() { ATrace_endSection(); }

    TraceScope(const TraceScope&)            = delete;
    TraceScope& operator=(const TraceScope&) = delete;
};

}  // namespace forge

// Trace the enclosing scope under `name` (a string literal): drop one at the top of a
// block and the slice closes when the block exits. The __LINE__ suffix lets several
// live in the same function without their generated variable names colliding.
#define FORGE_TRACE_CONCAT_(a, b) a##b
#define FORGE_TRACE_CONCAT(a, b) FORGE_TRACE_CONCAT_(a, b)
#define FORGE_TRACE(name) ::forge::TraceScope FORGE_TRACE_CONCAT(forgeTrace_, __LINE__)(name)
