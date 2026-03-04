set(SNOW_LLVM_FOUND OFF)
set(SNOW_LLVM_INCLUDE_DIRS "")
set(SNOW_LLVM_DEFINITIONS "")
set(SNOW_LLVM_LIBS "")

if(SNOW_ENABLE_LLVM)
  if(NOT DEFINED LLVM_DIR)
    set(_SNOW_LLVM_HINT "C:/Program Files/LLVM/lib/cmake/llvm")
    if(EXISTS "${_SNOW_LLVM_HINT}/LLVMConfig.cmake")
      set(LLVM_DIR "${_SNOW_LLVM_HINT}" CACHE PATH "LLVM CMake config directory for Snow" FORCE)
      message(STATUS "Using LLVM_DIR hint: ${LLVM_DIR}")
    endif()
  endif()

  find_package(LLVM 17 CONFIG QUIET)
  if(LLVM_FOUND)
    message(STATUS "LLVM found: ${LLVM_PACKAGE_VERSION}")
    set(SNOW_LLVM_FOUND ON)
    set(SNOW_LLVM_INCLUDE_DIRS ${LLVM_INCLUDE_DIRS})
    set(SNOW_LLVM_DEFINITIONS ${LLVM_DEFINITIONS})

    if(TARGET LLVM)
      set(SNOW_LLVM_LIBS LLVM)
    else()
      llvm_map_components_to_libnames(SNOW_LLVM_LIBS
        Core
        Support
      )
    endif()
  else()
    message(WARNING "SNOW_ENABLE_LLVM=ON but LLVM >= 17 not found. Falling back to stub lowering.")
  endif()
endif()
