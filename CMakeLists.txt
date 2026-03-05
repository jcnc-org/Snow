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

set(SNOW_PUBLIC_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/include")

function(snow_apply_common_settings target_name)
  target_include_directories(${target_name}
    PUBLIC
      ${SNOW_PUBLIC_INCLUDE_DIR}
  )
endfunction()

add_library(snow_common STATIC
  src/common/diagnostic.cpp
  src/common/manifest.cpp
  src/common/mangling.cpp
  src/common/target.cpp
)
snow_apply_common_settings(snow_common)

add_library(snow_frontend STATIC
  src/frontend/lexer.cpp
  src/frontend/parser.cpp
  src/frontend/parser_common.cpp
  src/frontend/parser_expr.cpp
  src/frontend/parser_stmt.cpp
  src/frontend/parser_module.cpp
  src/frontend/parser_dump.cpp
)
snow_apply_common_settings(snow_frontend)
target_link_libraries(snow_frontend PUBLIC snow_common)

add_library(snow_sema STATIC
  src/sema/sema.cpp
)
snow_apply_common_settings(snow_sema)
target_link_libraries(snow_sema PUBLIC snow_frontend snow_common)

add_library(snow_ownership STATIC
  src/ownership/ownership.cpp
)
snow_apply_common_settings(snow_ownership)
target_link_libraries(snow_ownership PUBLIC snow_sema snow_common)

add_library(snow_sir STATIC
  src/sir/sir_builder.cpp
  src/sir/sir_dump.cpp
  src/sir/validator.cpp
)
snow_apply_common_settings(snow_sir)
target_link_libraries(snow_sir PUBLIC snow_ownership snow_sema snow_common)

add_library(snow_passes STATIC
  src/passes/pass_manager.cpp
  src/passes/pass_utils.cpp
  src/passes/constant_fold_pass.cpp
  src/passes/cfg_simplify_pass.cpp
  src/passes/copy_propagation_pass.cpp
  src/passes/dead_code_elimination_pass.cpp
  src/passes/inline_pass.cpp
)
snow_apply_common_settings(snow_passes)
target_link_libraries(snow_passes PUBLIC snow_sir snow_common)

add_library(snow_codegen_llvm STATIC
  src/codegen/lowering.cpp
  src/codegen/llvm/textual_lowering.cpp
  src/codegen/llvm/object_emitter.cpp
)
snow_apply_common_settings(snow_codegen_llvm)
target_compile_definitions(snow_codegen_llvm PUBLIC SNOW_ENABLE_LLVM=1)
target_include_directories(snow_codegen_llvm SYSTEM PRIVATE ${SNOW_LLVM_INCLUDE_DIRS})
if(SNOW_LLVM_DEFINITIONS)
  separate_arguments(SNOW_LLVM_DEFINITIONS_LIST NATIVE_COMMAND "${SNOW_LLVM_DEFINITIONS}")
  target_compile_definitions(snow_codegen_llvm PRIVATE ${SNOW_LLVM_DEFINITIONS_LIST})
endif()
target_link_libraries(snow_codegen_llvm PUBLIC snow_passes snow_sir snow_common)
if(SNOW_LLVM_LIBS)
  target_link_libraries(snow_codegen_llvm PRIVATE ${SNOW_LLVM_LIBS})
endif()

add_library(snow_runtime STATIC
  src/runtime/runtime_stub.cpp
)
snow_apply_common_settings(snow_runtime)
target_link_libraries(snow_runtime PUBLIC snow_common)

add_library(snow_driver STATIC
  src/driver/driver.cpp
  src/driver/driver_build.cpp
)
snow_apply_common_settings(snow_driver)
target_link_libraries(snow_driver
  PUBLIC
    snow_codegen_llvm
    snow_passes
    snow_sir
    snow_ownership
    snow_sema
    snow_frontend
    snow_runtime
    snow_common
)

add_library(snow_compiler INTERFACE)
target_link_libraries(snow_compiler
  INTERFACE
    snow_driver
)

add_executable(snowc src/cli/main.cpp)
target_link_libraries(snowc PRIVATE snow_driver)

add_executable(cloud2toml tools/cloud2toml/main.cpp)

include(CTest)
if(BUILD_TESTING)
  add_subdirectory(tests)
endif()
