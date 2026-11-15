cmake_minimum_required(VERSION 3.18.1)
project(wzhnode)

add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so)

include_directories(${CMAKE_SOURCE_DIR}/node-include)

add_library(wzhnode SHARED wzh-node.cpp)
target_link_libraries(wzhnode node log)
