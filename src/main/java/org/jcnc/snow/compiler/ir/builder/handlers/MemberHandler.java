package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.IRBuilderScope;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;

import java.util.*;

/**
 * {@code MemberHandler} 是成员访问表达式的处理器，
 * 负责将 obj.field 类型的成员访问降级为底层的 __index_r(obj, fieldIndex) 调用。
 * 主要解决结构体继承层级下 fieldIndex 的正确计算，避免父类字段下标重复累计的问题。
 * <p>
 * 功能概述：
 * <ul>
 *   <li>对普通对象成员访问（如 obj.field）进行翻译，将其转换为 IR 的 __index_r(obj, fieldIndex) 形式；</li>
 *   <li>处理模块常量（如 ModuleName.constName），直接生成常量加载指令；</li>
 *   <li>智能处理扁平化与未扁平化结构体布局，准确计算字段的全局下标，兼容继承链与字段去重。</li>
 * </ul>
 */
public class MemberHandler implements ExpressionHandler<MemberExpressionNode> {

    /**
     * 处理成员访问表达式，将 obj.field 转换为 __index_r(obj, fieldIndex) IR 表达式。
     * 如果是 ModuleName.constName 这样的模块级常量，直接生成常量加载。
     *
     * @param b   表达式构建器
     * @param mem 成员访问表达式 AST 节点
     * @return IR 虚拟寄存器，存放成员访问结果
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, MemberExpressionNode mem) {
        // 特殊分支：如果是 ModuleName.constName，直接从全局常量表取常量
        if (mem.object() instanceof IdentifierNode modId) {
            Object constVal = GlobalConstTable.get(modId.name() + "." + mem.member());
            if (constVal != null) {
                IRVirtualRegister outConst = b.ctx().newRegister();
                b.ctx().addInstruction(new LoadConstInstruction(outConst, IRConstant.fromObject(constVal)));
                return outConst;
            }
        }

        // 1. 计算被访问对象的寄存器
        IRVirtualRegister objReg = b.build(mem.object());

        // 2. 解析对象的类型（优先使用标识符，否则尝试 this）
        String ownerType = null;
        if (mem.object() instanceof IdentifierNode id) {
            ownerType = b.ctx().getScope().lookupType(id.name());
        }
        if (ownerType == null || ownerType.isEmpty()) {
            String thisType = b.ctx().getScope().lookupType("this");
            if (thisType != null && !thisType.isEmpty()) {
                ownerType = thisType;
            }
        }
        if (ownerType == null || ownerType.isEmpty()) {
            throw new IllegalStateException("无法解析成员访问接收者的类型");
        }

        // 3. 计算字段下标
        Integer fieldIndex = resolveFieldIndex(ownerType, mem.member());
        if (fieldIndex == null) {
            throw new IllegalStateException("类型 " + ownerType + " 不存在字段: " + mem.member());
        }

        // 4. 生成 __index_r(obj, idx) 指令
        IRVirtualRegister idxReg = b.ctx().newRegister();
        b.ctx().addInstruction(new LoadConstInstruction(
                idxReg, IRConstant.fromNumber(Integer.toString(fieldIndex))));

        IRVirtualRegister out = b.ctx().newRegister();
        List<IRValue> args = new ArrayList<>();
        args.add(objReg);
        args.add(idxReg);
        b.ctx().addInstruction(new CallInstruction(out, "__index_r", args));
        return out;
    }

    /**
     * 计算结构体字段的下标槽位。
     * <ul>
     *   <li>若子类布局已扁平化（包含父类字段），直接返回布局中的下标；</li>
     *   <li>若未扁平化，则下标 = 去重累计的祖先字段数量 + 自身布局下标；</li>
     *   <li>若字段声明于祖先类，递归上溯继承链，并累计去重的祖先字段数量；</li>
     * </ul>
     *
     * @param structName 结构体名
     * @param fieldName  字段名
     * @return 字段在对象布局中的下标，找不到时返回 null
     */
    private Integer resolveFieldIndex(String structName, String fieldName) {
        Map<String, Integer> selfLayout = IRBuilderScope.getStructLayout(structName);

        // 情况 A：字段在本类声明
        if (selfLayout != null && selfLayout.containsKey(fieldName)) {
            if (isFlattenedWithParent(structName, selfLayout)) {
                // 已扁平化：布局里的下标就是全局槽位
                return selfLayout.get(fieldName);
            } else {
                // 未扁平化：加上“去重后的祖先字段数”
                return countDistinctAncestorFields(structName) + selfLayout.get(fieldName);
            }
        }

        // 情况 B：字段在祖先类声明 —— 去重累计祖先字段数
        int offset = 0;
        Set<String> seen = new HashSet<>();
        String anc = IRBuilderScope.getStructParent(structName);
        while (anc != null) {
            Map<String, Integer> ancLayout = IRBuilderScope.getStructLayout(anc);
            if (ancLayout != null) {
                if (ancLayout.containsKey(fieldName)) {
                    // 找到声明处：offset + 祖先布局中的下标
                    return offset + ancLayout.get(fieldName);
                }
                // 累计去重后的字段数
                for (String k : ancLayout.keySet()) {
                    if (seen.add(k)) {
                        offset++;
                    }
                }
            }
            anc = IRBuilderScope.getStructParent(anc);
        }
        return null;
    }

    /**
     * 判断结构体布局是否已经“扁平化”地包含父类字段。
     * 若任意父类字段出现在子类布局中，则认为已经扁平化。
     *
     * @param structName  当前结构体名
     * @param childLayout 当前结构体的字段布局
     * @return 如果已扁平化，返回 true；否则 false
     */
    private boolean isFlattenedWithParent(String structName, Map<String, Integer> childLayout) {
        String parent = IRBuilderScope.getStructParent(structName);
        if (parent == null) return true;
        Map<String, Integer> parentLayout = IRBuilderScope.getStructLayout(parent);
        if (parentLayout == null || parentLayout.isEmpty()) return true;
        for (String k : parentLayout.keySet()) {
            if (childLayout.containsKey(k)) return true;
        }
        return false;
    }

    /**
     * 统计所有祖先类字段的数量（去重），
     * 用于未扁平化继承场景，防止字段下标因继承链重复计算。
     *
     * @param structName 当前结构体名
     * @return 所有祖先类字段（去重后）的总数
     */
    private int countDistinctAncestorFields(String structName) {
        Set<String> seen = new HashSet<>();
        String anc = IRBuilderScope.getStructParent(structName);
        while (anc != null) {
            Map<String, Integer> ancLayout = IRBuilderScope.getStructLayout(anc);
            if (ancLayout != null) {
                seen.addAll(ancLayout.keySet());
            }
            anc = IRBuilderScope.getStructParent(anc);
        }
        return seen.size();
    }
}
