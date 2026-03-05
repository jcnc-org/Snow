# Snow 语言语法规范 v1（C++ 主线，中文）

状态: Accepted

日期: 2026-03-05

## 1. 适用范围与规范优先级

- 本规范仅覆盖当前 C++ 主线已实现且可测试验证的语法子集。
- 本规范的实现权威来源为：
  - `include/snow/frontend/token.h`
  - `src/frontend/lexer.cpp`
  - `include/snow/frontend/ast.h`
  - `src/frontend/parser_*.cpp`
  - `src/sema/sema.cpp`
  - `tests/data/cli_cases.tsv`
- 历史 Java/VM 时代语法与 playground 旧语法仅作参考，不属于 active 规范。

## 2. 词法规范（Lexer）

### 2.1 空白与注释

- 空白字符会被跳过。
- 支持单行注释：`// ...`。

### 2.2 Token 集（与 `TokenType` 对齐）

- 标识与字面量：
  - `Identifier`
  - `Number`（当前仅十进制整数字面量）
- 关键字：
  - `KeywordImport` (`import`)
  - `KeywordAs` (`as`)
  - `KeywordFn` (`fn`)
  - `KeywordPub` (`pub`)
  - `KeywordInternal` (`internal`)
  - `KeywordPrivate` (`private`)
  - `KeywordReturn` (`return`)
  - `KeywordIf` (`if`)
  - `KeywordElse` (`else`)
  - `KeywordWhile` (`while`)
  - `KeywordBreak` (`break`)
  - `KeywordContinue` (`continue`)
  - `KeywordLet` (`let`)
- 分隔符与操作符：
  - `Arrow` (`->`)
  - `Dot` (`.`)
  - `Comma` (`,`)
  - `Colon` (`:`)
  - `Semicolon` (`;`)
  - `Star` (`*`)
  - `Plus` (`+`)
  - `Minus` (`-`)
  - `Slash` (`/`)
  - `Percent` (`%`)
  - `Equal` (`=`)
  - `EqualEqual` (`==`)
  - `Less` (`<`)
  - `LessEqual` (`<=`)
  - `Greater` (`>`)
  - `GreaterEqual` (`>=`)
  - `Bang` (`!`)
  - `BangEqual` (`!=`)
  - `LParen` (`(`)
  - `RParen` (`)`)
  - `LBrace` (`{`)
  - `RBrace` (`}`)
  - `EndOfFile`
  - `Unknown`

## 3. 语法（Parser，EBNF）

```ebnf
module         := { import_decl | function_decl } EOF ;

import_decl    := "import" import_path [ "as" IDENT ] [ ";" ] ;
import_path    := IDENT { "." IDENT } [ "." "*" ] ;

function_decl  := [ visibility ] "fn" IDENT "(" [ param_list ] ")" "->" type ( block | [ ";" ] ) ;
visibility     := "pub" | "internal" | "private" ;
param_list     := param { "," param } ;
param          := IDENT ":" type ;
type           := IDENT ;

block          := "{" { statement | block } "}" ;

statement      := return_stmt
                | let_stmt
                | if_stmt
                | while_stmt
                | break_stmt
                | continue_stmt
                | assign_stmt
                | expr_stmt ;

return_stmt    := "return" [ expr ] ";" ;
let_stmt       := "let" IDENT [ ":" type ] "=" expr ";" ;
if_stmt        := "if" expr block [ "else" block ] ;
while_stmt     := "while" expr block ;
break_stmt     := "break" ";" ;
continue_stmt  := "continue" ";" ;
assign_stmt    := IDENT "=" expr ";" ;
expr_stmt      := expr ";" ;

expr           := precedence_expr ;
precedence_expr:= primary { binop precedence_expr } ;
primary        := NUMBER
                | IDENT [ "(" [ arg_list ] ")" ]
                | "(" expr ")" ;
arg_list       := expr { "," expr } ;
binop          := "+" | "-" | "*" | "/" | "%" | "==" | "!=" | "<" | ">" | "<=" | ">=" ;
```

说明：

- 实际实现采用优先级爬升解析：
  - `== !=` 最低
  - `< > <= >=`
  - `+ -`
  - `* / %` 最高
- `import` 语句末尾分号当前为可选。

## 4. AST 结构约束（与 `ast.h` 对齐）

- `Expr::Kind`：
  - `Number`
  - `Identifier`
  - `Call`
  - `Binary`
- `Statement::Kind`：
  - `Return`
  - `Expr`
  - `Assign`
  - `If`
  - `While`
  - `Break`
  - `Continue`
  - `Let`
- `BinaryOp`：
  - `Add`
  - `Sub`
  - `Mul`
  - `Div`
  - `Mod`
  - `Eq`
  - `Ne`
  - `Lt`
  - `Gt`
  - `Le`
  - `Ge`

## 5. 语义约束（Sema）

- 解析通过不代表语义通过；以下为当前语义强约束：
  - `return` 在 v1 语义检查阶段要求必须有表达式（否则 `E_SEMA_RET_MISSING`）。
  - `if` 与 `while` 条件必须为 `bool/i1`（否则 `E_SEMA_IF_COND_TYPE` / `E_SEMA_WHILE_COND_TYPE`）。
  - 赋值目标必须已定义且类型兼容（`E_SEMA_ASSIGN_UNDEFINED` / `E_SEMA_ASSIGN_TYPE`）。
  - 调用目标必须可解析，参数个数与类型必须匹配（`E_SEMA_CALL_UNDEFINED` / `E_SEMA_CALL_ARITY` / `E_SEMA_CALL_ARG_TYPE`）。
  - `break` / `continue` 仅可出现在循环内（`E_SEMA_BREAK_OUTSIDE_LOOP` / `E_SEMA_CONTINUE_OUTSIDE_LOOP`）。
  - `import x.*` 可用但默认告警 `W_STAR_IMPORT_DISCOURAGED`。

## 6. 诊断约束（语法相关）

- 词法错误：
  - `E_LEX_UNKNOWN_CHAR`
- 语法错误（示例）：
  - `E_PARSE_TOPLEVEL`
  - `E_PARSE_FN_NAME`
  - `E_PARSE_FN_ARROW`
  - `E_PARSE_RETURN_SEMI`
  - `E_PARSE_LET_SEMI`
  - `E_PARSE_ASSIGN_SEMI`
- 语义错误（语法行为直接相关）：
  - `E_SEMA_RET_TYPE`
  - `E_SEMA_LET_TYPE`
  - `E_SEMA_ASSIGN_TYPE`
  - `E_SEMA_CALL_ARITY`
  - `E_SEMA_CALL_ARG_TYPE`

## 7. 明确不支持或受限项（v1）

- `%` 运算符：
  - parser 接受为 `BinaryOp::Mod`；
  - sema 统一报错 `E_SEMA_UNSUPPORTED_OP`，当前视为受限语法。
- 字符串字面量（`"..."`）：
  - lexer 当前不支持，触发 `E_LEX_UNKNOWN_CHAR`。
- 本规范不包含旧语法（如 `module:`, `struct:`, `loop:` 风格）与 legacy 语法扩展。

## 8. 规范与实现映射

- 词法定义：`include/snow/frontend/token.h`, `src/frontend/lexer.cpp`
- 语法定义：`include/snow/frontend/ast.h`, `src/frontend/parser_module.cpp`, `src/frontend/parser_stmt.cpp`, `src/frontend/parser_expr.cpp`
- 语义约束：`src/sema/sema.cpp`
- 回归入口：`tests/data/cli_cases.tsv`, `tests/data/cases/*.snow`

## 9. 变更治理

- 任何 `lexer/parser/AST/sema` 语法行为变更，必须同变更提交更新：
  - 本文档 `docs/Snow-Language-Syntax-v1-zh.md`
  - 机器清单 `docs/Snow-Language-Syntax-v1.manifest.json`
  - 对应测试（unit 或 `tests/data/cli_cases.tsv` + case）
- `tools/check_syntax_alignment.ps1` 为 hard-fail gate，要求文档、清单、代码、测试四者一致。
