# Variables:
#  EXE: executable path
#  ARGS: pipe-separated args (example: compile|--emit-llvm|file.snow)
#  EXPECT_EXIT: expected exit code
#  EXPECT_REGEX: pipe-separated regex patterns that must appear in output
#  REJECT_REGEX: pipe-separated regex patterns that must NOT appear in output
#  EXPECT_FILE: pipe-separated file paths that must exist
#  EXPECT_FILE_MAGIC: pipe-separated "path=>hexprefix" entries (example: out.exe=>4d5a)

if(NOT DEFINED EXE)
  message(FATAL_ERROR "EXE is required")
endif()

if(NOT DEFINED EXPECT_EXIT)
  set(EXPECT_EXIT 0)
endif()

function(normalize_manifest_regex OUT_VAR IN_VALUE)
  set(_value "${IN_VALUE}")
  # Manifest entries are authored like CMake string literals, so collapse
  # escaped backslashes before using regex matching.
  string(REPLACE "\\\\" "\\" _value "${_value}")
  set(${OUT_VAR} "${_value}" PARENT_SCOPE)
endfunction()

set(ARG_LIST "")
if(DEFINED ARGS AND NOT "${ARGS}" STREQUAL "")
  string(REPLACE "|" ";" ARG_LIST "${ARGS}")
endif()

execute_process(
  COMMAND "${EXE}" ${ARG_LIST}
  RESULT_VARIABLE ACTUAL_EXIT
  OUTPUT_VARIABLE STDOUT_TEXT
  ERROR_VARIABLE STDERR_TEXT
)

set(COMBINED_TEXT "${STDOUT_TEXT}\n${STDERR_TEXT}")

if(NOT "${ACTUAL_EXIT}" STREQUAL "${EXPECT_EXIT}")
  message(FATAL_ERROR
    "Unexpected exit code. expected=${EXPECT_EXIT} actual=${ACTUAL_EXIT}\n"
    "--- stdout ---\n${STDOUT_TEXT}\n"
    "--- stderr ---\n${STDERR_TEXT}\n")
endif()

if(DEFINED EXPECT_REGEX AND NOT "${EXPECT_REGEX}" STREQUAL "")
  normalize_manifest_regex(_expect_regex "${EXPECT_REGEX}")
  string(REPLACE "|" ";" EXPECT_LIST "${_expect_regex}")
  foreach(pattern IN LISTS EXPECT_LIST)
    if(pattern STREQUAL "")
      continue()
    endif()
    string(REGEX MATCH "${pattern}" FOUND "${COMBINED_TEXT}")
    if(NOT FOUND)
      message(FATAL_ERROR
        "Expected regex not found: ${pattern}\n"
        "--- output ---\n${COMBINED_TEXT}\n")
    endif()
  endforeach()
endif()

if(DEFINED REJECT_REGEX AND NOT "${REJECT_REGEX}" STREQUAL "")
  normalize_manifest_regex(_reject_regex "${REJECT_REGEX}")
  string(REPLACE "|" ";" REJECT_LIST "${_reject_regex}")
  foreach(pattern IN LISTS REJECT_LIST)
    if(pattern STREQUAL "")
      continue()
    endif()
    string(REGEX MATCH "${pattern}" FOUND "${COMBINED_TEXT}")
    if(FOUND)
      message(FATAL_ERROR
        "Rejected regex was found: ${pattern}\n"
        "--- output ---\n${COMBINED_TEXT}\n")
    endif()
  endforeach()
endif()

if(DEFINED EXPECT_FILE AND NOT "${EXPECT_FILE}" STREQUAL "")
  string(REPLACE "|" ";" FILE_LIST "${EXPECT_FILE}")
  foreach(path IN LISTS FILE_LIST)
    if(path STREQUAL "")
      continue()
    endif()
    if(NOT EXISTS "${path}")
      message(FATAL_ERROR "Expected file does not exist: ${path}")
    endif()
  endforeach()
endif()

if(DEFINED EXPECT_FILE_MAGIC AND NOT "${EXPECT_FILE_MAGIC}" STREQUAL "")
  string(REPLACE "|" ";" MAGIC_LIST "${EXPECT_FILE_MAGIC}")
  foreach(entry IN LISTS MAGIC_LIST)
    if(entry STREQUAL "")
      continue()
    endif()
    string(REGEX MATCH "^(.*)=>([0-9A-Fa-f]+)$" MATCHED "${entry}")
    if(NOT MATCHED)
      message(FATAL_ERROR "Invalid EXPECT_FILE_MAGIC entry: ${entry}")
    endif()
    set(path "${CMAKE_MATCH_1}")
    set(expected_hex "${CMAKE_MATCH_2}")
    if(NOT EXISTS "${path}")
      message(FATAL_ERROR "Expected magic file does not exist: ${path}")
    endif()
    file(READ "${path}" actual_hex HEX OFFSET 0 LIMIT 8)
    string(TOLOWER "${actual_hex}" actual_hex)
    string(TOLOWER "${expected_hex}" expected_hex)
    string(LENGTH "${expected_hex}" expected_len)
    string(SUBSTRING "${actual_hex}" 0 ${expected_len} actual_prefix)
    if(NOT actual_prefix STREQUAL expected_hex)
      message(FATAL_ERROR
        "Unexpected file magic for ${path}. expected prefix=${expected_hex} actual=${actual_hex}")
    endif()
  endforeach()
endif()
