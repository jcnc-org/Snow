package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.compiler.ir.builder.IRBuilderScope;
import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.*;

/**
 * {@code VMProgramBuilder} 负责后端阶段的 VM 指令序列组装及符号修补。
 * <ul>
 *     <li>管理函数与标签到指令地址的映射</li>
 *     <li>支持 CALL 和分支指令的延迟回填（符号修补）</li>
 *     <li>支持槽位类型标注（用于类型检查和后端优化，可选）</li>
 * </ul>
 *
 * <p><b>符号修补机制：</b></p>
 * <ul>
 *     <li>支持根据函数简名进行匹配，例如 Student.getName 可匹配到 Person.getName，实现方法复用和继承支持。</li>
 *     <li>自动处理 super 调用的符号绑定，例如 Student.super 可自动绑定到 Person.__init__。</li>
 *     <li>所有未解决的 CALL 或分支符号在 build() 阶段将抛出异常，便于调试和定位错误。</li>
 * </ul>
 */
public final class VMProgramBuilder {

    /**
     * 未解析地址的占位符，便于后期批量修补
     */
    private static final String PLACEHOLDER = "-1";
    /**
     * VM 指令列表
     */
    private final List<String> code = new ArrayList<>();
    /**
     * 槽位(寄存器)类型映射表(如 I/F...，用于类型检查或代码生成优化)
     */
    private final Map<Integer, Character> slotType = new HashMap<>();
    /**
     * 符号(函数名/标签)到指令序号的映射表
     */
    private final Map<String, Integer> addr = new HashMap<>();
    /**
     * 所有待修补的 CALL 指令集合
     */
    private final List<CallFix> callFixes = new ArrayList<>();
    /**
     * 所有待修补的分支指令集合
     */
    private final List<BranchFix> branchFixes = new ArrayList<>();
    /**
     * 当前代码指针(已生成指令的数量/下一个指令的位置)
     */
    private int pc = 0;

    /**
     * 提取给定名称的最后一个片段。
     * 主要用于从“模块.类.方法”这样的全限定名中，
     * 提取出末尾的简单标识符。
     *
     * @param name 输入的符号全名（可能包含多个 '.' 分隔的层级）
     * @return 最后一个 '.' 之后的子串；如果没有 '.'，则返回原始字符串
     */
    private static String lastSegment(String name) {
        int i = name.lastIndexOf('.');
        return (i < 0) ? name : name.substring(i + 1);
    }

    /**
     * 读取当前已生成的代码列表（不可变视图）。
     */
    public List<String> getCode() {
        return Collections.unmodifiableList(code);
    }

    /**
     * 获取当前 pc。
     */
    public int getPc() {
        return pc;
    }

    /**
     * 设置槽位的类型前缀（如 'I','F','R'）。
     */
    public void setSlotType(int slot, char prefix) {
        slotType.put(slot, prefix);
    }

    /**
     * 获取槽位的类型前缀，默认为 'I'(整数类型)。
     */
    public char getSlotType(int slot) {
        return slotType.getOrDefault(slot, 'I');
    }

    /**
     * 声明一个函数/标签的起始地址，并尝试修补所有引用到此符号的 CALL/BRANCH。
     *
     * @param name 函数或标签全名（如 "Person.getName"、"loop.start"）
     */
    public void beginFunction(String name) {
        addr.put(name, pc);
        patchCallFixes(name);
        patchBranchFixes(name);
    }

    /**
     * 函数结束接口，目前无具体实现，便于将来扩展。
     */
    public void endFunction() {
    }

    /**
     * 添加一条 VM 指令或标签（末尾':'视为标签）。
     *
     * @param line 指令或标签
     */
    public void emit(String line) {
        if (line.endsWith(":")) {
            String label = line.substring(0, line.length() - 1);
            addr.put(label, pc);
            patchBranchFixes(label);
            return;
        }
        code.add(line);
        pc++;
    }

    /**
     * 生成一条 {@code CALL} 指令。
     * <p>
     * 该方法根据目标字符串 {@code target} 的特征，决定生成的 CALL 指令类型：
     * </p>
     * <ul>
     *     <li><b>静态可解析调用：</b>
     *     如果 {@code target} 能够在编译/汇编期解析到绝对地址，
     *     则生成形如 {@code CALL <addr> <nArgs>} 的指令。</li>
     *
     *     <li><b>虚函数调用：</b>
     *     如果 {@code target} 包含 {@code "::"}，但无法在静态期解析，
     *     则认为是虚函数调用。此时生成形如
     *     {@code CALL @Class::method <nArgs>} 的指令，
     *     并在运行时通过 vtable 进行方法查找。</li>
     *
     *     <li><b>待回填调用：</b>
     *     如果以上两种情况都不满足，则生成一个带占位符的指令
     *     {@code CALL <PLACEHOLDER> <nArgs>}，
     *     并将该调用信息记录到 {@code callFixes} 列表中，
     *     以便在后续回填阶段修正目标地址。</li>
     * </ul>
     *
     * @param target 调用目标，可以是绝对地址、类方法签名（如 {@code "Animal::speak"}）或符号引用。
     * @param nArgs  调用参数个数。
     */
    public void emitCall(String target, int nArgs) {
        Integer addr = resolve(target);
        if (addr != null) {
            /* 静态可解析：直接生成绝对地址调用 */
            emit(VMOpCode.CALL + " " + addr + " " + nArgs);
        } else if (target.contains("::")) {
            /* 虚函数调用：运行时通过 vtable 查找 */
            emit(VMOpCode.CALL + " @" + target + " " + nArgs);
        } else {
            /* 待回填调用：记录占位符，稍后修正 */
            emit(VMOpCode.CALL + " " + PLACEHOLDER + " " + nArgs);
            callFixes.add(new CallFix(pc - 1, target, nArgs));
        }
    }


    /**
     * 添加一条分支指令(如 JMP/BR/BEQ)，若目标未定义则延后修补。
     *
     * @param opcode 指令操作码
     * @param label  跳转目标标签名
     */
    public void emitBranch(String opcode, String label) {
        Integer a = resolve(label);
        if (a != null) {
            emit(opcode + " " + a);
        } else {
            emit(opcode + " " + PLACEHOLDER);
            branchFixes.add(new BranchFix(pc - 1, label));
        }
    }

    /**
     * 完成代码生成，输出最终 VM 指令序列。
     * <p>
     * 在最终报错前，统一做一次“继承链回填”：
     * <ol>
     *   <li>优先尝试精确目标名（子类方法/重写优先）</li>
     *   <li>否则递归查找父类同名方法</li>
     *   <li>最后仅在唯一情况下允许“简名唯一匹配”</li>
     * </ol>
     * 如果还有未修补的调用或分支，将抛出异常（包含全部未解析符号，便于调试）。
     *
     * @return 指令序列(不可变)
     * @throws IllegalStateException 如果存在未修补符号
     */
    public List<String> build() {
        // --- 统一做一次继承链回填 ---
        patchRemainingFixesByInheritance();

        if (!callFixes.isEmpty() || !branchFixes.isEmpty()) {
            throw new IllegalStateException("""
                    Unresolved symbols while building:
                      calls   = %s
                      branches= %s
                    """.formatted(callFixes, branchFixes));
        }
        return List.copyOf(code);
    }

    /**
     * 解析符号地址，仅支持全名精准匹配。
     *
     * @param sym 符号全名
     * @return 地址(指令序号)，未找到返回 null
     */
    private Integer resolve(String sym) {
        return addr.get(sym);
    }

    /**
     * 在所有函数都已落址后，统一对剩余 CALL 进行继承链/最终回填：
     * 1. 若存在“精确目标名”，优先绑定到它（保证子类重写优先）；
     * 2. 否则沿继承链向上查找第一个已定义的同名方法并绑定；
     * 3. 若仍未命中，再尝试“简名唯一匹配”（避免多义性，只有唯一时才绑定）；
     * 未能解析者保留给后续的未解析报错逻辑。
     */
    private void patchRemainingFixesByInheritance() {
        for (Iterator<CallFix> it = callFixes.iterator(); it.hasNext(); ) {
            CallFix f = it.next();

            // 1) 精确目标名（如 Student.getName），保证优先匹配到子类重写方法
            Integer exact = addr.get(f.target);
            if (exact != null) {
                code.set(f.index, VMOpCode.CALL + " " + exact + " " + f.nArgs);
                it.remove();
                continue;
            }

            // 2) 沿父类链向上查找第一个已定义的同名方法
            int dot = f.target.indexOf('.');
            if (dot > 0) {
                String curStruct = f.target.substring(0, dot);
                String member = f.target.substring(dot + 1);

                while (curStruct != null) {
                    String cand = curStruct + "." + member;
                    Integer a = addr.get(cand);
                    if (a != null) {
                        code.set(f.index, VMOpCode.CALL + " " + a + " " + f.nArgs);
                        it.remove();
                        break;
                    }
                    curStruct = IRBuilderScope.getStructParent(curStruct);
                }
                if (!it.hasNext()) { // 防止迭代器状态错误的小优化
                    // no-op
                }
                if (!callFixes.contains(f)) { // 已移除则继续
                    continue;
                }
            }

            // 3) 简名唯一匹配（只允许唯一目标，否则放弃防止二义性）
            if (dot < 0) {
                String simple = f.target;
                String chosen = null;
                for (String k : addr.keySet()) {
                    int i = k.lastIndexOf('.');
                    String ks = (i >= 0) ? k.substring(i + 1) : k;
                    if (ks.equals(simple)) {
                        if (chosen != null && !chosen.equals(k)) {
                            chosen = null; // 多义性，放弃
                            break;
                        }
                        chosen = k;
                    }
                }
                if (chosen != null) {
                    int a = addr.get(chosen);
                    code.set(f.index, VMOpCode.CALL + " " + a + " " + f.nArgs);
                    it.remove();
                }
            }
        }
    }

    /**
     * 逐个函数定义出现时的回填：
     * 仅处理‘全名精确匹配’、‘super(...) 构造调用’和‘原本未限定名的简名匹配’。
     * 继承链回填改为在全部定义完成后统一进行，避免过早把子类调用绑到父类。
     *
     * @param name 当前刚声明/定义的函数名（通常为全限定名）
     */
    private void patchCallFixes(String name) {
        // 当前刚定义/落址的函数的“简名”（不含结构体前缀）
        String nameSimple;
        int cut = name.lastIndexOf('.');
        nameSimple = (cut >= 0) ? name.substring(cut + 1) : name;

        for (Iterator<CallFix> it = callFixes.iterator(); it.hasNext(); ) {
            CallFix f = it.next();

            // 1) 全限定名精确匹配
            boolean qualifiedMatch = f.target.equals(name);

            // 2) super(...) 绑定（用于 __init__N）
            boolean superMatch = false;
            if (f.target.endsWith(".super") && name.contains(".__init__")) {
                String tStruct = f.target.substring(0, f.target.length() - 6); // 去掉 ".super"
                String nStruct = name.substring(0, name.indexOf(".__init__"));

                int initArgc = -1;
                try {
                    String num = name.substring(name.lastIndexOf("__init__") + 8);
                    initArgc = Integer.parseInt(num);
                } catch (NumberFormatException ignored) {
                }

                // 结构名一致或参数个数一致即可认为匹配
                if (tStruct.equals(nStruct) || initArgc == f.nArgs) {
                    superMatch = true;
                }
            }

            // 3) 简名匹配（仅当“原始目标本来就是未限定名”时才允许）
            String targetSimple;
            int p = f.target.lastIndexOf('.');
            targetSimple = (p >= 0) ? f.target.substring(p + 1) : f.target;
            boolean simpleMatch = (p < 0) && targetSimple.equals(nameSimple);

            if (qualifiedMatch || superMatch || simpleMatch) {
                int targetAddr = addr.get(name);
                code.set(f.index, VMOpCode.CALL + " " + targetAddr + " " + f.nArgs);
                it.remove();
            }
        }
    }

    /**
     * 修补所有等待目标 label 的分支指令。
     *
     * @param label 目标标签
     */
    private void patchBranchFixes(String label) {
        for (Iterator<BranchFix> it = branchFixes.iterator(); it.hasNext(); ) {
            BranchFix f = it.next();
            if (f.label.equals(label)) {
                String patched = code.get(f.index).replace(PLACEHOLDER, addr.get(label).toString());
                code.set(f.index, patched);
                it.remove();
            }
        }
    }

    /**
     * 未知目标的 CALL 指令修补记录(待目标地址确定后修正)。
     */
    private record CallFix(int index, String target, int nArgs) {
    }

    /**
     * 未知目标的分支指令修补记录(待目标标签确定后修正)。
     */
    private record BranchFix(int index, String label) {
    }
}
