set(SNOW_LLVM_FOUND OFF)
set(SNOW_LLVM_INCLUDE_DIRS "")
set(SNOW_LLVM_DEFINITIONS "")
set(SNOW_LLVM_LIBS "")

if(NOT SNOW_ENABLE_LLVM)
  message(FATAL_ERROR "Snow requires LLVM backend. Configure with -DSNOW_ENABLE_LLVM=ON.")
endif()

set(_SNOW_LLVM_HINT_ROOTS "")
if(DEFINED ENV{SNOW_LLVM_SDK_ROOT} AND NOT "$ENV{SNOW_LLVM_SDK_ROOT}" STREQUAL "")
  list(APPEND _SNOW_LLVM_HINT_ROOTS "$ENV{SNOW_LLVM_SDK_ROOT}")
endif()
if(DEFINED ENV{LLVM_ROOT} AND NOT "$ENV{LLVM_ROOT}" STREQUAL "")
  list(APPEND _SNOW_LLVM_HINT_ROOTS "$ENV{LLVM_ROOT}")
endif()

if(WIN32)
  if(DEFINED ENV{USERPROFILE} AND NOT "$ENV{USERPROFILE}" STREQUAL "")
    list(APPEND _SNOW_LLVM_HINT_ROOTS
      "$ENV{USERPROFILE}/.snow/toolchains/llvm-21.1.8/clang+llvm-21.1.8-x86_64-pc-windows-msvc"
    )
  endif()
  list(APPEND _SNOW_LLVM_HINT_ROOTS
    "C:/Program Files/LLVM"
  )
else()
  list(APPEND _SNOW_LLVM_HINT_ROOTS
    "/opt/homebrew/opt/llvm"
    "/usr/local/opt/llvm"
    "/usr/lib/llvm-21"
    "/usr/local"
  )
endif()

if(NOT DEFINED LLVM_DIR OR LLVM_DIR STREQUAL "" OR LLVM_DIR STREQUAL "LLVM_DIR-NOTFOUND")
  foreach(_snow_llvm_root IN LISTS _SNOW_LLVM_HINT_ROOTS)
    if(EXISTS "${_snow_llvm_root}/lib/cmake/llvm/LLVMConfig.cmake")
      set(LLVM_DIR "${_snow_llvm_root}/lib/cmake/llvm" CACHE PATH "LLVM CMake config directory for Snow" FORCE)
      message(STATUS "Using LLVM_DIR hint: ${LLVM_DIR}")
      break()
    endif()
  endforeach()
endif()

find_package(LLVM 21.1.8 EXACT CONFIG REQUIRED)
if(NOT LLVM_VERSION_MAJOR EQUAL 21)
  message(FATAL_ERROR "Snow requires LLVM major version 21. Found ${LLVM_PACKAGE_VERSION}.")
endif()

message(STATUS "LLVM found: ${LLVM_PACKAGE_VERSION}")
set(SNOW_LLVM_FOUND ON)
set(SNOW_LLVM_INCLUDE_DIRS ${LLVM_INCLUDE_DIRS})
set(SNOW_LLVM_DEFINITIONS ${LLVM_DEFINITIONS})

if(WIN32 AND TARGET LLVMDebugInfoPDB)
  file(GLOB _SNOW_DIAGUIDS_CANDIDATES
    "C:/Program Files/Microsoft Visual Studio/*/*/DIA SDK/lib/amd64/diaguids.lib"
    "D:/Program Files/Microsoft Visual Studio/*/*/DIA SDK/lib/amd64/diaguids.lib"
    "C:/Program Files (x86)/Microsoft Visual Studio/*/*/DIA SDK/lib/amd64/diaguids.lib"
  )
  list(LENGTH _SNOW_DIAGUIDS_CANDIDATES _SNOW_DIAGUIDS_COUNT)
  if(_SNOW_DIAGUIDS_COUNT GREATER 0)
    list(GET _SNOW_DIAGUIDS_CANDIDATES 0 _SNOW_DIAGUIDS_LIB)
    file(TO_CMAKE_PATH "${_SNOW_DIAGUIDS_LIB}" _SNOW_DIAGUIDS_LIB)
    get_target_property(_SNOW_LLVM_PDB_LINK_LIBS LLVMDebugInfoPDB INTERFACE_LINK_LIBRARIES)
    if(_SNOW_LLVM_PDB_LINK_LIBS)
      string(REPLACE
        "C:/Program Files (x86)/Microsoft Visual Studio/2019/Professional/DIA SDK/lib/amd64/diaguids.lib"
        "${_SNOW_DIAGUIDS_LIB}"
        _SNOW_LLVM_PDB_LINK_LIBS
        "${_SNOW_LLVM_PDB_LINK_LIBS}"
      )
      set_target_properties(LLVMDebugInfoPDB PROPERTIES INTERFACE_LINK_LIBRARIES "${_SNOW_LLVM_PDB_LINK_LIBS}")
      message(STATUS "LLVM DIA override: ${_SNOW_DIAGUIDS_LIB}")
    endif()
  else()
    message(WARNING "diaguids.lib not found; LLVM link may fail. Install Visual Studio DIA SDK.")
  endif()
endif()

llvm_map_components_to_libnames(SNOW_LLVM_LIBS
  Core
  Support
  AsmParser
  IRReader
  Target
  MC
  CodeGen
  AsmPrinter
  X86
  AArch64
)
