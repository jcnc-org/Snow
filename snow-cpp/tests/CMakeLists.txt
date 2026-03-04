set(ASSERT_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/assert_cli.cmake)
set(DATA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/data)
set(CASES_DIR ${CMAKE_CURRENT_SOURCE_DIR}/data/cases)
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

add_executable(snow-unit-tests unit/snow_unit_tests.cpp)
target_link_libraries(snow-unit-tests PRIVATE snow_compiler)
add_test(
  NAME snow_unit_tests
  COMMAND $<TARGET_FILE:snow-unit-tests>
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

add_cli_assert_test(
  snowc_version
  snowc
  0
  "version"
  "snowc v1\\.0\\.0-cpp-alpha"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_emit_llvm
  snowc
  0
  "compile|--opt=2|--emit-llvm|-o|${ARTIFACT_DIR}/emit_llvm.exe|${DATA_DIR}/minimal.snow"
  "_snow_|snow_runtime_start"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_emit_lib_no_entry
  snowc
  0
  "compile|--emit-llvm|--emit-lib|-o|${ARTIFACT_DIR}/emit_lib.a|${DATA_DIR}/minimal.snow"
  "target triple"
  "snow_runtime_start"
  ""
)

add_cli_assert_test(
  snowc_compile_target_override
  snowc
  0
  "compile|--emit-llvm|--target|x86_64-unknown-linux-gnu|-o|${ARTIFACT_DIR}/target_override.exe|${DATA_DIR}/minimal.snow"
  "target triple = \"x86_64-unknown-linux-gnu\""
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_star_import_warning
  snowc
  0
  "compile|-o|${ARTIFACT_DIR}/star_warning.exe|${CASES_DIR}/star_import_warning.snow"
  "W_STAR_IMPORT_DISCOURAGED"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_ambiguous_import_fail
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/ambiguous_fail.exe|${CASES_DIR}/ambiguous_import.snow"
  "AmbiguousSymbol"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_lex_error_fail
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/lex_fail.exe|${CASES_DIR}/lex_error.snow"
  "E_LEX_UNKNOWN_CHAR"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_dup_symbol_fail
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/dup_fail.exe|${CASES_DIR}/dup_symbol.snow"
  "E_SEMA_DUP_SYMBOL"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_visibility_linkage
  snowc
  0
  "compile|--emit-llvm|-o|${ARTIFACT_DIR}/visibility_linkage.exe|${CASES_DIR}/visibility_linkage.snow"
  "define private i32|define internal i32|_helper_[0-9a-f]+|_util_[0-9a-f]+|_main_[0-9a-f]+"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_artifact_object
  snowc
  0
  "compile|--emit-object|-o|${ARTIFACT_DIR}/test_obj.o|${DATA_DIR}/minimal.snow"
  "artifact:"
  ""
  "${ARTIFACT_DIR}/test_obj.o"
)

add_cli_assert_test(
  snowc_build_project_ok
  snowc
  0
  "build|--opt=2|--emit-sir|${PROJECT_OK_DIR}"
  "build modules: 2|math\\.vector|main"
  ""
  "${PROJECT_OK_DIR}/snow-build/main.exe"
)

add_cli_assert_test(
  snowc_build_project_ok_emit_lib
  snowc
  0
  "build|--emit-lib|--emit-llvm|${PROJECT_OK_DIR}"
  "target triple"
  "snow_runtime_start"
  "${PROJECT_OK_DIR}/snow-build/main.a"
)

add_cli_assert_test(
  snowc_build_project_manifest_main_target
  snowc
  0
  "build|--emit-llvm|${PROJECT_MANIFEST_DIR}"
  "build modules: 1|app|target triple = \"x86_64-unknown-linux-gnu\""
  ""
  "${PROJECT_MANIFEST_DIR}/snow-build/main.exe"
)

add_cli_assert_test(
  snowc_build_project_manifest_cli_target_override
  snowc
  0
  "build|--emit-llvm|--target|aarch64-apple-darwin|${PROJECT_MANIFEST_DIR}"
  "target triple = \"aarch64-apple-darwin\""
  ""
  "${PROJECT_MANIFEST_DIR}/snow-build/main.exe"
)

add_cli_assert_test(
  snowc_build_project_cycle_fail
  snowc
  1
  "build|${PROJECT_CYCLE_DIR}"
  "E_MODULE_CYCLE"
  ""
  ""
)

add_cli_assert_test(
  snow_diff_harness_match
  snow-diff-harness
  0
  "--java-cmd|$<TARGET_FILE:snowc>|--snowc|$<TARGET_FILE:snowc>|${DATA_DIR}/minimal.snow"
  "classification=MATCH"
  ""
  ""
)
