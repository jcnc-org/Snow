# Variables:
#  EXE: executable path
#  ARGS: pipe-separated args (example: compile|--emit-llvm|file.snow)
#  EXPECT_EXIT: expected exit code
#  EXPECT_REGEX: pipe-separated regex patterns that must appear in output
#  REJECT_REGEX: pipe-separated regex patterns that must NOT appear in output
#  EXPECT_FILE: pipe-separated file paths that must exist

if(NOT DEFINED EXE)
  message(FATAL_ERROR "EXE is required")
endif()

if(NOT DEFINED EXPECT_EXIT)
  set(EXPECT_EXIT 0)
endif()

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
  string(REPLACE "|" ";" EXPECT_LIST "${EXPECT_REGEX}")
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
  string(REPLACE "|" ";" REJECT_LIST "${REJECT_REGEX}")
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
