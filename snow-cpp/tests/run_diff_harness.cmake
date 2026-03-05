if(NOT DEFINED HARNESS OR "${HARNESS}" STREQUAL "")
  message(FATAL_ERROR "HARNESS is required")
endif()
if(NOT DEFINED SNOWC OR "${SNOWC}" STREQUAL "")
  message(FATAL_ERROR "SNOWC is required")
endif()
if(NOT DEFINED INPUT OR "${INPUT}" STREQUAL "")
  message(FATAL_ERROR "INPUT is required")
endif()

set(JAVA_CMD "")
if(DEFINED ENV{SNOW_JAVA_CMD} AND NOT "$ENV{SNOW_JAVA_CMD}" STREQUAL "")
  set(JAVA_CMD "$ENV{SNOW_JAVA_CMD}")
endif()

if(DEFINED JAVA_CMD_OVERRIDE AND NOT "${JAVA_CMD_OVERRIDE}" STREQUAL "")
  set(JAVA_CMD "${JAVA_CMD_OVERRIDE}")
endif()

if("${JAVA_CMD}" STREQUAL "")
  message(STATUS "SNOW_JAVA_CMD is not set; skipping java differential test")
  return()
endif()

set(COMMAND_ARGS
  --java-cmd "${JAVA_CMD}"
  --snowc "${SNOWC}"
  --fail-on-unclassified
  "${INPUT}"
)

if(DEFINED REPORT AND NOT "${REPORT}" STREQUAL "")
  list(APPEND COMMAND_ARGS --report "${REPORT}")
endif()

execute_process(
  COMMAND "${HARNESS}" ${COMMAND_ARGS}
  RESULT_VARIABLE ACTUAL_EXIT
  OUTPUT_VARIABLE STDOUT_TEXT
  ERROR_VARIABLE STDERR_TEXT
)

if(NOT "${ACTUAL_EXIT}" STREQUAL "0")
  message(FATAL_ERROR
    "diff harness failed. exit=${ACTUAL_EXIT}\n"
    "--- stdout ---\n${STDOUT_TEXT}\n"
    "--- stderr ---\n${STDERR_TEXT}\n")
endif()

