cmake_minimum_required(VERSION 3.22.1)
project(forge_engine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(android-lib android)
find_library(log-lib log)

add_library(forge_engine SHARED
    forge_jni.cpp
)

target_link_libraries(forge_engine
    ${android-lib}
    ${log-lib}
)