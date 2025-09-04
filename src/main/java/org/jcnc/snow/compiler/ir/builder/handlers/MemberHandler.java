package org.jcnc.snow.compiler.ir.builder.handlers;

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

import java.util.ArrayList;
import java.util.List;

/**
 * 成员访问（MemberExpressionNode）处理器。
 * <p>
 * 负责处理对象字段、静态常量、模块常量等成员访问表达式，自动分派到对应的 IR 指令。
 * 支持对模块常量的优化直接折叠，也支持动态对象字段读取。
 */
public class MemberHandler implements ExpressionHandler<MemberExpressionNode> {
    /**
     * 处理成员访问表达式，返回对应的虚拟寄存器。
     *
     * @param b   表达式构建器
     * @param mem 成员访问 AST 节点
     * @return 存放成员值的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, MemberExpressionNode mem) {
        // ===== 1. 优先判定为模块常量（形如 mod.CONST）=====
        if (mem.object() instanceof IdentifierNode oid) {
            String mod = oid.name();
            Object c = GlobalConstTable.get(mod + "." + mem.member());
            if (c != null) {
                // 命中全局常量表，直接生成常量加载指令
                IRVirtualRegister r = b.ctx().newRegister();
                b.ctx().addInstruction(new LoadConstInstruction(r, new IRConstant(c)));
                return r;
            }
        }

        // ===== 2. 常规对象字段访问（如 obj.field）=====
        IRVirtualRegister objReg = b.build(mem.object()); // 先获取对象寄存器

        // ===== 3. 推断 owner 类型（优先用 object 的类型，否则 fallback 到 this 的类型）=====
        String ownerType = null;
        if (mem.object() instanceof IdentifierNode oid) {
            ownerType = b.ctx().getScope().lookupType(oid.name());
        }
        if (ownerType == null || ownerType.isEmpty()) {
            // 若找不到 object 的类型，则尝试 this
            String thisType = b.ctx().getScope().lookupType("this");
            if (thisType != null) ownerType = thisType;
        }
        if (ownerType == null || ownerType.isEmpty()) {
            throw new IllegalStateException("无法解析成员访问接收者的类型");
        }

        // ===== 4. 查询字段索引（field offset），找不到直接报错 =====
        Integer fieldIndex = b.ctx().getScope().lookupFieldIndex(ownerType, mem.member());
        if (fieldIndex == null) {
            throw new IllegalStateException("类型 " + ownerType + " 不存在字段: " + mem.member());
        }

        // ===== 5. 生成下标参数，转为 __index_r 统一成员取值指令 =====
        IRVirtualRegister idxReg = b.ctx().newRegister();
        b.ctx().addInstruction(new LoadConstInstruction(idxReg,
                IRConstant.fromNumber(Integer.toString(fieldIndex))));

        IRVirtualRegister out = b.ctx().newRegister();
        List<IRValue> args = new ArrayList<>();
        args.add(objReg);
        args.add(idxReg);
        b.ctx().addInstruction(new CallInstruction(out, "__index_r", args));
        return out;
    }
}
