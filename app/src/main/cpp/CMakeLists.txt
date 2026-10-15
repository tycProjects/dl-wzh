cmake_minimum_required(VERSION 3.22.1)
project(native_memory)

add_library(native_memory SHARED native_memory.cpp)

find_library(log-lib log)
target_link_libraries(native_memory ${log-lib})
