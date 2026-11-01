cmake_minimum_required(VERSION 3.22.1)
project("zewmod_native")

add_library(
    zewmod_native
    SHARED
    zewmod_native.c
)

find_library(log-lib log)
find_library(dl-lib dl)

target_link_libraries(
    zewmod_native
    ${log-lib}
    ${dl-lib}
)

target_compile_options(zewmod_native PRIVATE
    -O2
    -fvisibility=hidden
    -fstack-protector-strong
)
