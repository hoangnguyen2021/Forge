#include "GpuTimer.h"

#include <EGL/egl.h>
#include <GLES2/gl2ext.h>

#include <cstring>

#define LOG_TAG "GpuTimer"
#include "../Log.h"

namespace forge {

namespace {
// EXT_disjoint_timer_query is loaded at runtime via eglGetProcAddress — Android's GLES
// library doesn't export these entry points directly. Held as file-static pointers,
// shared across any GpuTimer instances on this thread.
PFNGLGENQUERIESEXTPROC          pGenQueries     = nullptr;
PFNGLDELETEQUERIESEXTPROC       pDeleteQueries  = nullptr;
PFNGLBEGINQUERYEXTPROC          pBeginQuery     = nullptr;
PFNGLENDQUERYEXTPROC            pEndQuery       = nullptr;
PFNGLGETQUERYOBJECTUIVEXTPROC   pGetQueryObjUiv = nullptr;
PFNGLGETQUERYOBJECTUI64VEXTPROC pGetQueryObjU64 = nullptr;

template <typename T>
void load(T& fn, const char* name) {
    fn = reinterpret_cast<T>(eglGetProcAddress(name));
}

// Whether the driver advertises the extension. A non-null eglGetProcAddress isn't proof on
// its own (some drivers return stubs), so we gate on the extension string too.
bool extensionListed() {
    const auto* exts = reinterpret_cast<const char*>(glGetString(GL_EXTENSIONS));
    return exts != nullptr && std::strstr(exts, "GL_EXT_disjoint_timer_query") != nullptr;
}
}  // namespace

bool GpuTimer::init() {
    if (!extensionListed()) {
        LOGI("EXT_disjoint_timer_query unavailable; GPU timing disabled");
        return false;
    }
    load(pGenQueries, "glGenQueriesEXT");
    load(pDeleteQueries, "glDeleteQueriesEXT");
    load(pBeginQuery, "glBeginQueryEXT");
    load(pEndQuery, "glEndQueryEXT");
    load(pGetQueryObjUiv, "glGetQueryObjectuivEXT");
    load(pGetQueryObjU64, "glGetQueryObjectui64vEXT");
    if (!pGenQueries || !pDeleteQueries || !pBeginQuery || !pEndQuery || !pGetQueryObjUiv ||
        !pGetQueryObjU64) {
        LOGE("EXT_disjoint_timer_query entry points missing; GPU timing disabled");
        return false;
    }

    // One query object per (ring slot, zone), so every zone can be timed every frame while
    // older slots are still being read back.
    for (auto& slot : queries_) {
        pGenQueries(kZones, slot);
    }
    available_ = true;
    LOGI("GPU timing enabled (EXT_disjoint_timer_query)");
    return true;
}

void GpuTimer::destroy() {
    if (!available_) {
        return;
    }
    for (auto& slot : queries_) {
        pDeleteQueries(kZones, slot);
    }
    available_ = false;
}

void GpuTimer::beginFrame() {
    if (!available_) {
        return;
    }

    // A disjoint event (thermal throttle, context switch) makes every in-flight timing
    // unreliable. Reading the flag with glGetIntegerv also clears it.
    GLint disjoint = 0;
    glGetIntegerv(GL_GPU_DISJOINT_EXT, &disjoint);

    currentSlot_ = static_cast<int>(frame_ % kRing);

    // The slot we're about to reuse holds queries issued kRing frames ago — old enough that
    // the GPU has finished them. Harvest those results before overwriting the slot.
    if (frame_ >= kRing) {
        for (int z = 0; z < kZones; ++z) {
            if (!issued_[currentSlot_][z]) {
                continue;
            }
            issued_[currentSlot_][z] = false;
            if (disjoint) {
                continue;  // spoiled timing; drop it rather than report garbage
            }
            GLuint ready = 0;
            pGetQueryObjUiv(queries_[currentSlot_][z], GL_QUERY_RESULT_AVAILABLE_EXT, &ready);
            if (!ready) {
                continue;  // GPU still behind; skip this sample
            }
            GLuint64 ns = 0;
            pGetQueryObjU64(queries_[currentSlot_][z], GL_QUERY_RESULT_EXT, &ns);
            lastMs_[z] = static_cast<double>(ns) / 1.0e6;
        }
    }
    ++frame_;
}

void GpuTimer::begin(Zone zone) {
    // Refuse to open a second query while one is active — TIME_ELAPSED cannot nest.
    if (!available_ || active_ != Zone::Count) {
        return;
    }
    const int z = static_cast<int>(zone);
    pBeginQuery(GL_TIME_ELAPSED_EXT, queries_[currentSlot_][z]);
    issued_[currentSlot_][z] = true;
    active_                  = zone;
}

void GpuTimer::end(Zone zone) {
    if (!available_ || active_ != zone) {
        return;
    }
    pEndQuery(GL_TIME_ELAPSED_EXT);
    active_ = Zone::Count;
}

double GpuTimer::lastMs(Zone zone) const {
    return lastMs_[static_cast<int>(zone)];
}

}  // namespace forge
