cmake_minimum_required(VERSION 3.14)
project(freeplane_grpc_cpp VERSION 0.1.1 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Generate protobuf and gRPC stubs
set(PROTO_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../src/main/proto")
set(PROTO_FILE "${PROTO_DIR}/freeplane.proto")
set(GENERATED_DIR "${CMAKE_CURRENT_SOURCE_DIR}/freeplane")

if(NOT EXISTS "${GENERATED_DIR}/freeplane.pb.h")
    message(STATUS "Generating C++ gRPC stubs from ${PROTO_FILE}")
    execute_process(
        COMMAND ${CMAKE_COMMAND} -E make_directory ${GENERATED_DIR}
    )
    execute_process(
        COMMAND protoc
            -I${PROTO_DIR}
            --cpp_out ${GENERATED_DIR}
            --grpc_out=plugins=grpc:${GENERATED_DIR}
            ${PROTO_FILE}
        RESULT_VARIABLE PROTO_RESULT
    )
    if(NOT PROTO_RESULT EQUAL 0)
        message(FATAL_ERROR "Failed to generate protobuf/gRPC stubs")
    endif()
endif()

# Library sources
set(LIB_SOURCES
    src/client.cpp
    src/node.cpp
    src/mindmap.cpp
    freeplane/freeplane.pb.cc
    freeplane/freeplane.grpc.pb.cc
)

# Header files
set(LIB_HEADERS
    src/error.h
    src/client.h
    src/node.h
    src/mindmap.h
    freeplane/freeplane.pb.h
    freeplane/freeplane.grpc.pb.h
)

# Create the library
add_library(freeplane_grpc ${LIB_SOURCES} ${LIB_HEADERS})

target_include_directories(freeplane_grpc
    PUBLIC
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>
        $<INSTALL_INTERFACE:include>
)

target_link_libraries(freeplane_grpc
    PUBLIC
        gRPC::grpc
        gRPC::grpc++
        protobuf::libprotobuf
)

# Use FetchContent for GTest to avoid Ubuntu libgtest-dev source-only issue
include(FetchContent)
FetchContent_Declare(
    GTest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG v1.14.0
    GIT_SHALLOW TRUE
)
set(GTEST_FORCE_SHARED_CRT ON CACHE BOOL "" FORCE)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
set(BUILD_GMOCK ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(GTest)

# Unit tests
option(BUILD_TESTS "Build unit tests" ON)
if(BUILD_TESTS)
    enable_testing()
    
    add_executable(test_freeplane_grpc
        tests/test_client.cpp
        tests/test_node.cpp
        tests/test_mindmap.cpp
        src/client.cpp
        src/node.cpp
        src/mindmap.cpp
        freeplane/freeplane.pb.cc
        freeplane/freeplane.grpc.pb.cc
    )
    
    target_include_directories(test_freeplane_grpc
        PRIVATE
            ${CMAKE_CURRENT_SOURCE_DIR}
            ${gtest_SOURCE_DIR}/googlemock/include
            ${gtest_SOURCE_DIR}/googletest/include
    )
    
    target_link_libraries(test_freeplane_grpc
        GTest::gtest_main
        gRPC::grpc
        gRPC::grpc++
        protobuf::libprotobuf
    )
    
    # Manual test discovery (replaces gtest_discover_tests which is unavailable with FetchContent)
    add_test(NAME test_freeplane_grpc COMMAND test_freeplane_grpc)
endif()

# Integration tests (require a running Freeplane server)
option(BUILD_INTEGRATION_TESTS "Build integration tests" ON)
if(BUILD_INTEGRATION_TESTS)
    enable_testing()
    
    add_executable(test_integration
        tests/integration/test_integration.cpp
        src/client.cpp
        src/node.cpp
        src/mindmap.cpp
        freeplane/freeplane.pb.cc
        freeplane/freeplane.grpc.pb.cc
    )
    
    target_include_directories(test_integration
        PRIVATE
            ${CMAKE_CURRENT_SOURCE_DIR}
            ${gtest_SOURCE_DIR}/googlemock/include
            ${gtest_SOURCE_DIR}/googletest/include
    )
    
    target_link_libraries(test_integration
        GTest::gtest_main
        GTest::gmock
        gRPC::grpc
        gRPC::grpc++
        protobuf::libprotobuf
    )
    
    # Manual test discovery (replaces gtest_discover_tests which is unavailable with FetchContent)
    add_test(NAME test_integration COMMAND test_integration)
endif()

# Install rules
install(TARGETS freeplane_grpc
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

install(DIRECTORY src/ DESTINATION include FILES_MATCHING PATTERN "*.h")
install(DIRECTORY freeplane/ DESTINATION include FILES_MATCHING PATTERN "*.h")
