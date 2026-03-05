cmake_minimum_required(VERSION 3.20)
project(snow_cpp VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(SNOW_ENABLE_LLVM "Enable LLVM lowering integration" ON)
if(NOT SNOW_ENABLE_LLVM)
  message(FATAL_ERROR "SNOW_ENABLE_LLVM=OFF is not supported in Snow v1.0. Enable LLVM backend.")
endif()

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
include(DetectLLVM)

add_library(snow_compiler STATIC
  src/common/diagnostic.cpp
  src/common/manifest.cpp
  src/common/mangling.cpp
  src/common/target.cpp
  src/frontend/lexer.cpp
  src/frontend/parser.cpp
  src/sema/sema.cpp
  src/ownership/ownership.cpp
  src/sir/sir_builder.cpp
  src/sir/validator.cpp
  src/passes/pass_manager.cpp
  src/passes/inline_pass.cpp
  src/codegen/lowering.cpp
  src/driver/driver.cpp
  src/runtime/runtime_stub.cpp
)

target_include_directories(snow_compiler
  PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_definitions(snow_compiler PUBLIC SNOW_ENABLE_LLVM=1)
target_include_directories(snow_compiler SYSTEM PRIVATE ${SNOW_LLVM_INCLUDE_DIRS})
if(SNOW_LLVM_DEFINITIONS)
  separate_arguments(SNOW_LLVM_DEFINITIONS_LIST NATIVE_COMMAND "${SNOW_LLVM_DEFINITIONS}")
  target_compile_definitions(snow_compiler PRIVATE ${SNOW_LLVM_DEFINITIONS_LIST})
endif()
if(SNOW_LLVM_LIBS)
  target_link_libraries(snow_compiler PRIVATE ${SNOW_LLVM_LIBS})
endif()

add_executable(snowc src/cli/main.cpp)
target_link_libraries(snowc PRIVATE snow_compiler)

add_executable(cloud2toml tools/cloud2toml/main.cpp)

include(CTest)
if(BUILD_TESTING)
  add_subdirectory(tests)
endif()
