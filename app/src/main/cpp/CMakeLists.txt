cmake_minimum_required(VERSION 3.22.1)
project(memengine C)

add_executable(memengine mem_engine.c)
target_compile_options(memengine PRIVATE -O2 -Wall)
