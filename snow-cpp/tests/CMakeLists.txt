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
  snowc_compile_body_ops_ok
  snowc
  0
  "compile|-o|${ARTIFACT_DIR}/body_ops_ok.exe|${CASES_DIR}/body_ops_ok.snow"
  "artifact:"
  "E_LEX_UNKNOWN_CHAR"
  ""
)

add_cli_assert_test(
  snowc_compile_return_literal_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/return_literal.exe|${CASES_DIR}/return_literal.snow"
  "ret 7 : i32"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_expr_arith_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/expr_arith.exe|${CASES_DIR}/expr_arith.snow"
  "mul 2, 3|add 1, %"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_expr_arith_llvm
  snowc
  0
  "compile|--emit-llvm|-o|${ARTIFACT_DIR}/expr_arith_llvm.exe|${CASES_DIR}/expr_arith.snow"
  "mul i32 2, 3|add i32 1, %[0-9]+|ret i32 %[0-9]+"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_expr_compare_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/expr_compare.exe|${CASES_DIR}/expr_compare.snow"
  "add 1, 2|lt %[0-9]+, 5|ret %[0-9]+ : bool"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_expr_return_type_mismatch
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/expr_ret_mismatch.exe|${CASES_DIR}/expr_ret_mismatch.snow"
  "E_SEMA_RET_TYPE"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_expr_mod_unsupported
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/expr_mod_unsupported.exe|${CASES_DIR}/expr_mod_unsupported.snow"
  "E_SEMA_UNSUPPORTED_OP"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_if_phi_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/if_phi.exe|${CASES_DIR}/if_phi.snow"
  "cond_br %[0-9]+, if_then, if_else|phi [0-9%]+, if_then, [0-9%]+, if_else|ret %[0-9]+ : i32"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_if_phi_llvm
  snowc
  0
  "compile|--emit-llvm|-o|${ARTIFACT_DIR}/if_phi_llvm.exe|${CASES_DIR}/if_phi.snow"
  "br i1 %[0-9]+, label %if_then, label %if_else|phi i32 \\[ [0-9%]+, %if_then \\], \\[ [0-9%]+, %if_else \\]"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_if_cond_type_mismatch
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/if_cond_bad.exe|${CASES_DIR}/if_cond_bad.snow"
  "E_SEMA_IF_COND_TYPE"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_while_cfg_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/while_cfg.exe|${CASES_DIR}/while_cfg.snow"
  "entry:|br loop_cond|loop_cond:|cond_br %[0-9]+, loop_body, loop_exit|loop_body:|br loop_cond|loop_exit:|ret 7 : i32"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_while_break_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/while_break.exe|${CASES_DIR}/while_break.snow"
  "loop_body:|br loop_exit|loop_exit:|ret 9 : i32"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_while_cfg_llvm
  snowc
  0
  "compile|--emit-llvm|-o|${ARTIFACT_DIR}/while_cfg_llvm.exe|${CASES_DIR}/while_cfg.snow"
  "entry:|br label %loop_cond|loop_cond:|br i1 %[0-9]+, label %loop_body, label %loop_exit|loop_body:|br label %loop_cond|loop_exit:|ret i32 7"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_while_break_llvm
  snowc
  0
  "compile|--emit-llvm|-o|${ARTIFACT_DIR}/while_break_llvm.exe|${CASES_DIR}/while_break.snow"
  "loop_body:|br label %loop_exit|loop_exit:|ret i32 9"
  ""
  ""
)

add_cli_assert_test(
  snowc_compile_while_cond_type_mismatch
  snowc
  1
  "compile|-o|${ARTIFACT_DIR}/while_cond_bad.exe|${CASES_DIR}/while_cond_bad.snow"
  "E_SEMA_WHILE_COND_TYPE"
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
  snowc_compile_call_with_args_sir
  snowc
  0
  "compile|--emit-sir|-o|${ARTIFACT_DIR}/call_with_args.exe|${CASES_DIR}/call_with_args.snow"
  "call _snow_.*_sum_[0-9a-f]+, 1, 2|ret %[0-9]+ : i32"
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
