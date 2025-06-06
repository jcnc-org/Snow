package org.jcnc.snow.compiler.backend.builder;

import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.*;

/**
 * VMProgramBuilder：构建线性 VM 程序（即按顺序存放所有 VM 指令）。
 * <p>
 * 本类用于编译器后端，将所有生成的 VM 指令（包括分支和调用指令）统一存储、管理。
 * 支持符号（如函数入口、标签地址）的延迟解析与回填（fix-up）机制，
 * 可在目标尚未定义时提前生成分支或调用指令，定义后自动修正。
 * </p>
 * <p>
 * 常用于处理跨函数、跨标签的 CALL/JUMP 等复杂控制流，确保最终生成的 VM 指令地址一致正确。
 * </p>
 */
public final class VMProgramBuilder {
    /** 未解析目标的 CALL 指令信息（待修补） */
    private record CallFix(int index, String target, int nArgs) {}

    /** 未解析目标的分支指令（JUMP/IC_* 等待修补） */
    private record BranchFix(int index, String label) {}

    /** 占位符：用于表示尚未确定的符号地址 */
    private static final String PLACEHOLDER = "-1";

    /** 按顺序存放的 VM 指令文本 */
    private final List<String> code = new ArrayList<>();

    // 虚拟机槽位编号到数据类型前缀的映射（如 0 -> 'I', 1 -> 'D' 等）
    private final Map<Integer, Character> slotType = new HashMap<>();

    /** 符号（如函数名、标签名）到其首地址（即指令序号/偏移量）的映射表
     *  主要用于跳转和调用，定位具体的代码位置 */
    private final Map<String, Integer> addr = new HashMap<>();

    /** 所有待回填（fix-up）的 CALL 调用指令记录
     *  由于被调用目标地址在编译时可能尚未确定，需要先记录，最终统一回填 */
    private final List<CallFix> callFixes = new ArrayList<>();

    /** 所有待回填（fix-up）的分支跳转指令记录
     *  与 CALL 类似，分支指令的目标地址也可能需要编译后期再补充 */
    private final List<BranchFix> branchFixes = new ArrayList<>();

    /** 程序计数器（Program Counter），表示下一个生成指令将插入的位置 */
    private int pc = 0;

    /**
     * 设置某个槽位对应的数据类型前缀
     * @param slot   槽位编号
     * @param prefix 类型前缀（如 'I' 表示 int，'D' 表示 double 等）
     */
    public void setSlotType(int slot, char prefix) {
        slotType.put(slot, prefix);
    }

    /**
     * 获取某个槽位对应的数据类型前缀
     * 若未指定则返回默认类型 'I'（int）
     * @param slot 槽位编号
     * @return 类型前缀（如 'I', 'D' 等）
     */
    public char getSlotType(int slot) {
        return slotType.getOrDefault(slot, 'I');
    }


    /**
     * 标记函数入口或标签，并尝试修补所有等候该符号的指令。
     * @param name 符号名（函数名/标签名）
     */
    public void beginFunction(String name) {
        addr.put(name, pc);            // 记录当前地址为入口
        patchCallFixes(name);          // 修补所有待该符号的 CALL
        patchBranchFixes(name);        // 修补所有待该符号的分支
    }

    /**
     * 结束函数（当前实现为空，方便 API 统一）。
     */
    public void endFunction() { /* no-op */ }

    /**
     * 写入一条 VM 指令或标签。
     * <ul>
     *   <li>如果以冒号结尾，视为标签，仅登记其地址，不写入指令流，也不递增 pc。</li>
     *   <li>否则写入实际指令，并自增 pc。</li>
     * </ul>
     * @param line 一行 VM 指令文本或标签名（结尾有冒号）
     */
    public void emit(String line) {
        if (line.endsWith(":")) {                  // 是标签定义行
            String label = line.substring(0, line.length() - 1);
            addr.put(label, pc);                   // 记录标签地址
            patchBranchFixes(label);               // 修补所有以该标签为目标的分支指令
            return;                                // 标签行不写入 code，不递增 pc
        }
        code.add(line);                            // 普通指令写入 code
        pc++;
    }

    /**
     * 生成 CALL 指令。
     * 支持延迟修补：若目标已知，直接写入地址；否则写入占位并登记 fix-up。
     * @param target 目标函数名
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
     * 生成分支（JUMP 或 IC_*）指令。
     * 支持延迟修补机制。
     * @param opcode 指令名
     * @param label  目标标签名
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
     * 构建最终 VM 代码文本列表。
     * <ul>
     *   <li>若存在未解析符号（CALL 或分支），则抛出异常。</li>
     *   <li>否则返回不可变指令流。</li>
     * </ul>
     * @return 完整 VM 指令流
     * @throws IllegalStateException 若有未修补的符号引用
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
     * 解析符号地址。若全限定名找不到则降级尝试简单名。
     * @param sym 符号名
     * @return    地址或 null（未定义）
     */
    private Integer resolve(String sym) {
        Integer a = addr.get(sym);
        if (a == null && sym.contains(".")) {
            a = addr.get(sym.substring(sym.lastIndexOf('.') + 1));
        }
        return a;
    }

    /**
     * 修补所有以 name 为目标的 CALL 占位符。
     * @param name 新定义的函数名
     */
    private void patchCallFixes(String name) {
        for (Iterator<CallFix> it = callFixes.iterator(); it.hasNext();) {
            CallFix f = it.next();
            // 目标函数名完全匹配或后缀匹配（兼容全限定名）
            if (f.target.equals(name) || f.target.endsWith("." + name)) {
                code.set(f.index, VMOpCode.CALL + " " + addr.get(name) + " " + f.nArgs);
                it.remove();
            }
        }
    }

    /**
     * 修补所有以 label 为目标的分支占位符。
     * @param label 新定义的标签名
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
