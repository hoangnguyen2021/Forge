cmake_minimum_required(VERSION 3.22.1)
project(forge_engine LANGUAGES CXX)

# C++20 is required by the vendored LiteRT C++ SDK (third_party/litert). It is a
# near-superset of C++17, so the existing engine code is unaffected.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(android-lib android)
find_library(log-lib log)
find_library(EGL-lib EGL)
find_library(GLESv3-lib GLESv3)

# Vendored LiteRT C++ SDK (prebuilt, v2.1.5). Builds the litert_cc_api static lib and
# fetches abseil + flatbuffers via FetchContent on the first configure. EXCLUDE_FROM_ALL
# keeps its test executables out of our build — only what forge_engine links is built.
add_subdirectory(third_party/litert ${CMAKE_BINARY_DIR}/litert EXCLUDE_FROM_ALL)

add_library(forge_engine SHARED
    forge_jni.cpp
    egl/EglContext.cpp
    engine/RenderEngine.cpp
    resources/FrameBuffer.cpp
    resources/FullScreenQuad.cpp
    gl/ShaderProgram.cpp
    gl/GpuTimer.cpp
    passes/CameraPass.cpp
    passes/PresentPass.cpp
    passes/EffectPass.cpp
    passes/CompositePass.cpp
    ml/LiteRtSmokeTest.cpp
)

target_compile_features(forge_engine PRIVATE cxx_std_20)
target_compile_options(forge_engine PRIVATE -Wall -Wextra)

target_link_libraries(forge_engine
    litert_cc_api
    ${android-lib}
    ${log-lib}
    ${EGL-lib}
    ${GLESv3-lib}
)