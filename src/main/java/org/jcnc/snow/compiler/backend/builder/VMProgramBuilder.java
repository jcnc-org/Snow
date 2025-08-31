package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.vm.engine.VMOpCode;
import java.util.*;

/**
 * VMProgramBuilder 用于构建虚拟机(VM)的最终指令列表。
 * <p>
 * 主要职责：
 * <ul>
 *     <li>维护代码指令序列和符号地址表</li>
 *     <li>支持跨函数、标签跳转的延后修补(Call/Branch Fixup)</li>
 *     <li>支持虚拟机本地槽位类型的管理(如 I/F...)</li>
 * </ul>
 */
public final class VMProgramBuilder {

    /**
     * 未知目标的 CALL 指令修补记录(待目标地址确定后修正)。
     * @param index CALL 指令在 code 列表中的位置
     * @param target 目标函数的全名
     * @param nArgs 参数个数
     */
    private record CallFix(int index, String target, int nArgs) {}

    /**
     * 未知目标的分支指令修补记录(待目标标签确定后修正)。
     * @param index 分支指令在 code 列表中的位置
     * @param label 跳转目标标签名
     */
    private record BranchFix(int index, String label) {}

    /** 未解析地址的占位符，便于后期批量修补 */
    private static final String PLACEHOLDER = "-1";

    /** VM 指令列表 */
    private final List<String> code = new ArrayList<>();
    /** 槽位(寄存器)类型映射表(如 I/F...，用于类型检查或代码生成优化) */
    private final Map<Integer, Character> slotType = new HashMap<>();
    /** 符号(函数名/标签)到指令序号的映射表 */
    private final Map<String, Integer> addr = new HashMap<>();
    /** 所有待修补的 CALL 指令集合 */
    private final List<CallFix> callFixes = new ArrayList<>();
    /** 所有待修补的分支指令集合 */
    private final List<BranchFix> branchFixes = new ArrayList<>();
    /** 当前代码指针(已生成指令的数量/下一个指令的位置) */
    private int pc = 0;

    /**
     * 设置槽位(局部变量/虚拟寄存器)的类型前缀。
     *
     * @param slot   槽位编号
     * @param prefix 类型前缀(如 'I', 'F')
     */
    public void setSlotType(int slot, char prefix) {
        slotType.put(slot, prefix);
    }

    /**
     * 获取槽位的类型前缀，默认为 'I'(整数类型)。
     *
     * @param slot 槽位编号
     * @return 类型前缀字符
     */
    public char getSlotType(int slot) {
        return slotType.getOrDefault(slot, 'I');
    }

    /* ==========================================================
       函数/标签管理
       ========================================================== */

    /**
     * 标记一个函数或标签的起始位置。
     * <p>
     * 1. 记录符号到当前 pc 的映射；
     * 2. 立即尝试修补之前所有针对该符号的延后调用和分支。
     *
     * @param name 函数名或标签名(全限定名)
     */
    public void beginFunction(String name) {
        addr.put(name, pc);
        patchCallFixes(name);
        patchBranchFixes(name);
    }

    /** 函数结尾的处理(占位，无需特殊处理)。 */
    public void endFunction() {}

    /**
     * 添加一条指令或标签到代码列表。
     *
     * @param line 指令字符串或标签字符串(若以冒号结尾为标签)
     */
    public void emit(String line) {
        if (line.endsWith(":")) {
            // 标签定义
            String label = line.substring(0, line.length() - 1);
            addr.put(label, pc);
            patchBranchFixes(label);
            return;
        }
        code.add(line);
        pc++;
    }

    /**
     * 添加一条 CALL 指令，若目标未定义则延后修补。
     *
     * @param target 目标函数全名
     * @param nArgs  参数个数
     */
    public void emitCall(String target, int nArgs) {
        Integer a = resolve(target);
        if (a != null) {
            emit(VMOpCode.CALL + " " + a + " " + nArgs);
        } else {
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
            emit(opcode + ' ' + a);
        } else {
            emit(opcode + ' ' + PLACEHOLDER);
            branchFixes.add(new BranchFix(pc - 1, label));
        }
    }

    /**
     * 完成代码生成，输出最终 VM 指令序列。
     * <p>
     * 如果存在未修补的调用或分支，将抛出异常。
     *
     * @return 指令序列(不可变)
     * @throws IllegalStateException 如果存在未修补符号
     */
    public List<String> build() {
        if (!callFixes.isEmpty() || !branchFixes.isEmpty()) {
            throw new IllegalStateException(
                    "Unresolved symbols — CALL: " + callFixes +
                            ", BRANCH: " + branchFixes);
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
     * 修补所有等待目标函数 name 的 CALL 指令。
     * <p>
     * 支持两种匹配：
     * <ul>
     *   <li>全名匹配：f.target == name</li>
     *   <li>简名匹配：f.target 不含 '.'，且等于 name 的最后一段</li>
     * </ul>
     * 这样 IR 里生成 CALL getCity 也能绑定到 Address.getCity。
     */
    private void patchCallFixes(String name) {
        // 当前函数的简名（去掉前缀）
        String simpleName = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1)
                : name;

        for (Iterator<CallFix> it = callFixes.iterator(); it.hasNext();) {
            CallFix f = it.next();

            boolean qualifiedMatch = f.target.equals(name);
            boolean simpleMatch = !f.target.contains(".") && f.target.equals(simpleName);

            if (qualifiedMatch || simpleMatch) {
                code.set(f.index, VMOpCode.CALL + " " + addr.get(name) + " " + f.nArgs);
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
        for (Iterator<BranchFix> it = branchFixes.iterator(); it.hasNext();) {
            BranchFix f = it.next();
            if (f.label.equals(label)) {
                String patched = code.get(f.index).replace(PLACEHOLDER, addr.get(label).toString());
                code.set(f.index, patched);
                it.remove();
            }
        }
    }
}
