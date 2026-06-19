cmake_minimum_required(VERSION 3.10)
project(splitter)

set(CMAKE_CXX_STANDARD 17)

add_executable(splitter splitter.cpp)
target_link_libraries(splitter ws2_32 user32 setupapi cfgmgr32)