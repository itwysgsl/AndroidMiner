cmake_minimum_required(VERSION 3.4.1)

# Include sub directory
add_subdirectory(${CMAKE_SOURCE_DIR}/src/main/cpp/libcpuminer)

add_library(
    mininglib
    SHARED
    src/main/cpp/mininglib.cpp)

find_library(
    log-lib
    log)

target_link_libraries(
    mininglib
    ${log-lib}
    cpuminer)

