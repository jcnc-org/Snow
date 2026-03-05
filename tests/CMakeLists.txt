set(ASSERT_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/assert_cli.cmake)
set(DOC_ASSERT_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/assert_docs.cmake)
set(DATA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/data)
set(CASES_DIR ${CMAKE_CURRENT_SOURCE_DIR}/data/cases)
set(CLI_MANIFEST ${CMAKE_CURRENT_SOURCE_DIR}/data/cli_cases.tsv)
set(ARTIFACT_DIR ${CMAKE_CURRENT_BINARY_DIR}/artifacts)
set(PROJECT_OK_DIR ${CMAKE_CURRENT_BINARY_DIR}/project_ok)
set(PROJECT_CYCLE_DIR ${CMAKE_CURRENT_BINARY_DIR}/project_cycle)
set(PROJECT_MANIFEST_DIR ${CMAKE_CURRENT_BINARY_DIR}/project_manifest)

file(MAKE_DIRECTORY ${ARTIFACT_DIR})
file(REMOVE_RECURSE ${PROJECT_OK_DIR})
file(REMOVE_RECURSE ${PROJECT_CYCLE_DIR})
file(REMOVE_RECURSE ${PROJECT_MANIFEST_DIR})
file(COPY ${DATA_DIR}/project_ok DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
file(COPY ${DATA_DIR}/project_cycle DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
file(COPY ${DATA_DIR}/project_manifest DESTINATION ${CMAKE_CURRENT_BINARY_DIR})

if(WIN32)
  set(EXEC_MAGIC_HEX 4d5a)
elseif(APPLE)
  set(EXEC_MAGIC_HEX cffaedfe)
else()
  set(EXEC_MAGIC_HEX 7f454c46)
endif()

add_executable(snow-unit-tests unit/snow_unit_tests.cpp)
target_link_libraries(snow-unit-tests PRIVATE snow_compiler)
add_test(
  NAME snow_unit_tests
  COMMAND $<TARGET_FILE:snow-unit-tests>
)

add_executable(snow-pass-tests unit/pass_pipeline_tests.cpp)
target_link_libraries(snow-pass-tests PRIVATE snow_compiler)
add_test(
  NAME snow_pass_tests
  COMMAND $<TARGET_FILE:snow-pass-tests>
)

add_executable(snow-sema-tests unit/snow_sema_tests.cpp)
target_link_libraries(snow-sema-tests PRIVATE snow_compiler)
add_test(
  NAME snow_sema_tests
  COMMAND $<TARGET_FILE:snow-sema-tests>
)

add_executable(snow-common-tests unit/snow_common_tests.cpp)
target_link_libraries(snow-common-tests PRIVATE snow_compiler)
add_test(
  NAME snow_common_tests
  COMMAND $<TARGET_FILE:snow-common-tests>
)

add_test(
  NAME snow_pass_docs_exist
  COMMAND ${CMAKE_COMMAND}
    -DDOC_PATH=${CMAKE_SOURCE_DIR}/docs/Snow-Pass-Invariants-v1.md
    -DREQUIRED_PATTERNS="Canonicalize|ConstantFold|CfgSimplify|CopyPropagation|DeadCodeElimination|Inline|input invariants|output invariants|failure modes"
    -P ${DOC_ASSERT_SCRIPT}
)

if(WIN32)
  set(SNOW_POWERSHELL_EXE powershell)
else()
  find_program(SNOW_POWERSHELL_EXE NAMES pwsh powershell)
  if(NOT SNOW_POWERSHELL_EXE)
    message(FATAL_ERROR "PowerShell executable not found (expected pwsh or powershell)")
  endif()
endif()

add_test(
  NAME snow_syntax_alignment
  COMMAND ${SNOW_POWERSHELL_EXE}
    -ExecutionPolicy Bypass
    -File ${CMAKE_SOURCE_DIR}/tools/check_syntax_alignment.ps1
    -Root ${CMAKE_SOURCE_DIR}
)

add_test(
  NAME snow_diagnostic_alignment
  COMMAND ${SNOW_POWERSHELL_EXE}
    -ExecutionPolicy Bypass
    -File ${CMAKE_SOURCE_DIR}/tools/check_diagnostic_alignment.ps1
    -Root ${CMAKE_SOURCE_DIR}
)

function(add_cli_assert_test NAME EXE EXIT_CODE ARGS EXPECT REJECT EXPECT_FILE)
  add_test(
    NAME ${NAME}
    COMMAND ${CMAKE_COMMAND}
      -DEXE=$<TARGET_FILE:${EXE}>
      -DEXPECT_EXIT=${EXIT_CODE}
      -DARGS=${ARGS}
      -DEXPECT_REGEX=${EXPECT}
      -DREJECT_REGEX=${REJECT}
      -DEXPECT_FILE=${EXPECT_FILE}
      -P ${ASSERT_SCRIPT}
  )
endfunction()

function(add_cli_assert_magic_test NAME EXE EXIT_CODE ARGS EXPECT REJECT EXPECT_FILE EXPECT_FILE_MAGIC)
  add_test(
    NAME ${NAME}
    COMMAND ${CMAKE_COMMAND}
      -DEXE=$<TARGET_FILE:${EXE}>
      -DEXPECT_EXIT=${EXIT_CODE}
      -DARGS=${ARGS}
      -DEXPECT_REGEX=${EXPECT}
      -DREJECT_REGEX=${REJECT}
      -DEXPECT_FILE=${EXPECT_FILE}
      -DEXPECT_FILE_MAGIC=${EXPECT_FILE_MAGIC}
      -P ${ASSERT_SCRIPT}
  )
endfunction()

function(strip_outer_quotes OUT_VAR IN_VALUE)
  set(_value "${IN_VALUE}")
  string(REGEX REPLACE "^\"(.*)\"$" "\\1" _value "${_value}")
  set(${OUT_VAR} "${_value}" PARENT_SCOPE)
endfunction()

function(expand_manifest_vars OUT_VAR IN_VALUE)
  set(_value "${IN_VALUE}")
  string(CONFIGURE "${_value}" _value)
  set(${OUT_VAR} "${_value}" PARENT_SCOPE)
endfunction()

if(NOT EXISTS ${CLI_MANIFEST})
  message(FATAL_ERROR "CLI manifest missing: ${CLI_MANIFEST}")
endif()

file(STRINGS ${CLI_MANIFEST} CLI_CASE_LINES)
list(REMOVE_AT CLI_CASE_LINES 0) # header

foreach(_line IN LISTS CLI_CASE_LINES)
  if(_line STREQUAL "")
    continue()
  endif()

  string(REPLACE "\t" ";" _cols "${_line}")
  list(LENGTH _cols _col_count)
  if(_col_count LESS 11)
    message(FATAL_ERROR "Malformed CLI case row (expected 11 columns): ${_line}")
  endif()

  list(GET _cols 0 _type)
  list(GET _cols 1 _name)
  list(GET _cols 2 _exe)
  list(GET _cols 3 _exit)
  list(GET _cols 4 _raw_args)
  list(GET _cols 5 _raw_expect)
  list(GET _cols 6 _raw_reject)
  list(GET _cols 7 _raw_expect_file)
  list(GET _cols 8 _raw_expect_magic)
  list(GET _cols 9 _env)
  list(GET _cols 10 _depends)

  strip_outer_quotes(_args "${_raw_args}")
  strip_outer_quotes(_expect "${_raw_expect}")
  strip_outer_quotes(_reject "${_raw_reject}")
  strip_outer_quotes(_expect_file "${_raw_expect_file}")
  strip_outer_quotes(_expect_magic "${_raw_expect_magic}")

  expand_manifest_vars(_args "${_args}")
  expand_manifest_vars(_expect "${_expect}")
  expand_manifest_vars(_reject "${_reject}")
  expand_manifest_vars(_expect_file "${_expect_file}")
  expand_manifest_vars(_expect_magic "${_expect_magic}")
  expand_manifest_vars(_env "${_env}")
  expand_manifest_vars(_depends "${_depends}")

  if(_type STREQUAL "magic")
    add_cli_assert_magic_test(
      ${_name}
      ${_exe}
      ${_exit}
      "${_args}"
      "${_expect}"
      "${_reject}"
      "${_expect_file}"
      "${_expect_magic}"
    )
  elseif(_type STREQUAL "assert")
    add_cli_assert_test(
      ${_name}
      ${_exe}
      ${_exit}
      "${_args}"
      "${_expect}"
      "${_reject}"
      "${_expect_file}"
    )
  else()
    message(FATAL_ERROR "Unknown CLI case type '${_type}' for ${_name}")
  endif()

  if(NOT _env STREQUAL "")
    set_tests_properties(${_name} PROPERTIES ENVIRONMENT "${_env}")
  endif()
  if(NOT _depends STREQUAL "")
    set_tests_properties(${_name} PROPERTIES DEPENDS "${_depends}")
  endif()
endforeach()

add_test(
  NAME snowc_build_project_ok_run_binary
  COMMAND ${CMAKE_COMMAND}
    -DEXE=${PROJECT_OK_DIR}/snow-build/main.exe
    -DEXPECT_EXIT=1
    -P ${ASSERT_SCRIPT}
)
set_tests_properties(snowc_build_project_ok_run_binary PROPERTIES DEPENDS snowc_build_project_ok)
