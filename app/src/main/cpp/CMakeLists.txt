cmake_minimum_required(VERSION 3.22.1)
project(forge_engine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(android-lib android)
find_library(log-lib log)
find_library(EGL-lib EGL)
find_library(GLESv3-lib GLESv3)

add_library(forge_engine SHARED
    forge_jni.cpp
    egl/EglContext.cpp
    engine/RenderEngine.cpp
    resource/FrameBuffer.cpp
    resource/FullScreenQuad.cpp
    shader/ShaderProgram.cpp
    shader/CameraPass.cpp
    shader/PresentPass.cpp
    shader/EffectPass.cpp
)

target_compile_features(forge_engine PRIVATE cxx_std_17)
target_compile_options(forge_engine PRIVATE -Wall -Wextra)

target_link_libraries(forge_engine
    ${android-lib}
    ${log-lib}
    ${EGL-lib}
    ${GLESv3-lib}
)