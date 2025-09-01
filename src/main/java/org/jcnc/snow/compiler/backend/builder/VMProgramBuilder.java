package org.jcnc.snow.compiler.backend.builder;

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

    // ==============================
    // 槽位类型操作
    // ==============================

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

    // =============函数/标签声明与指令生成=================

    /**
     * 声明一个函数/标签的起始地址，并尝试修补所有引用到此符号的 CALL/BRANCH。
     * @param name 函数或标签全名（如 "Person.getName"、"loop.start"）
     */
    public void beginFunction(String name) {
        addr.put(name, pc);
        patchCallFixes(name);
        patchBranchFixes(name);
    }

    /** 函数结束接口，目前无具体实现，便于将来扩展。 */
    public void endFunction() {}

    /**
     * 添加一条 VM 指令或标签（末尾':'视为标签）。
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
     * 添加一条 CALL 指令。目标尚未声明时，使用占位符并登记延迟修补。
     * @param target  目标函数全名（IR 侧生成）
     * @param nArgs   实参个数
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
            emit(opcode + " " + a);
        } else {
            emit(opcode + " " + PLACEHOLDER);
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
                    "Unresolved symbols — CALL: " + callFixes + ", BRANCH: " + branchFixes
            );
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
     * 对所有待修补的 CALL 指令进行补丁。
     * 匹配规则：
     *   1. 全限定名完全匹配
     *   2. 简名(最后一段)匹配
     *   3. super 调用：target 以 .super 结尾、name 以 .__init__ 结尾
     * @param name 新声明的符号名
     */
    private void patchCallFixes(String name) {
        // 当前函数的简名（去掉前缀）
        String nameSimple = lastSegment(name);

        for (Iterator<CallFix> it = callFixes.iterator(); it.hasNext(); ) {
            CallFix f = it.next();

            // 全限定名完全匹配
            boolean qualifiedMatch = f.target.equals(name);
            // 简名匹配
            String targetSimple = lastSegment(f.target);
            boolean simpleMatch = targetSimple.equals(nameSimple);
            // super 调用绑定
            boolean superMatch = f.target.endsWith(".super") && name.endsWith(".__init__");

            if (qualifiedMatch || simpleMatch || superMatch) {
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
     * 获取符号名最后一段（即最后一个 '.' 之后的名字，若无 '.' 则为原名）。
     * @param sym 符号全名
     * @return 简名
     */
    private static String lastSegment(String sym) {
        int p = sym.lastIndexOf('.');
        return (p >= 0 && p < sym.length() - 1) ? sym.substring(p + 1) : sym;
    }
}
