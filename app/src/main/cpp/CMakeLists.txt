cmake_minimum_required(VERSION 3.22.1)
project("zewmod_engine")

add_library(
    zewmod_engine
    SHARED
    zewmod_engine.c
)

find_library(log-lib log)

target_link_libraries(
    zewmod_engine
    ${log-lib}
)
