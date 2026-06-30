cmake_minimum_required(VERSION 3.31.0)
project("automod_native" C CXX ASM)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 23)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Architecture optimizations for Cortex-X3/A715 (aarch64)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.5-a+fp16+rcpc+dotprod+flagm+mte -mtune=cortex-x3 -O3 -flto=thin -fomit-frame-pointer -fvisibility=hidden -fno-strict-aliasing")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8.5-a+fp16+rcpc+dotprod+flagm+mte -mtune=cortex-x3 -O3 -flto=thin -fomit-frame-pointer -fvisibility=hidden -fno-strict-aliasing")

add_library(automod_native SHARED
    native_bridge.cpp
    memory_scanner.cpp
    hook_engine.cpp
    dex_patcher.cpp
    elf_parser.cpp
    syscall_intercept.cpp
    metacognitive_engine.cpp
)

target_include_directories(automod_native PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/../r2frida_bridge/src/main/cpp
    ${CMAKE_SOURCE_DIR}/../r2ghidra_bridge/src/main/cpp
)

target_link_libraries(automod_native
    log
    dl
    android
    frida-gadget
    radare2
)

# Frida gadget static linking
add_library(frida-gadget STATIC IMPORTED)
set_target_properties(frida-gadget PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/arm64-v8a/libfrida-gadget.a
)
