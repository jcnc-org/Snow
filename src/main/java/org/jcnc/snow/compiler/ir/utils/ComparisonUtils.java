package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IROpCodeMappings;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.Map;

/**
 * 比较运算辅助工具：
 * 根据左右操作数类型（目前通过字面量后缀 <code>L/l</code> 判定）选择
 * 正确的 IR 比较指令，保证 int/long 均能正常运行。
 */
public final class ComparisonUtils {
    private ComparisonUtils() {
    }

    /**
     * 判断给定操作符是否为比较运算符
     */
    public static boolean isComparisonOperator(String op) {
        // 两张表 key 完全一致，只需检查一张
        return IROpCodeMappings.CMP_I32.containsKey(op);
    }

    /**
     * 返回符合操作数位宽的比较 IROpCode。
     *
     * @param op    比较符号（==, !=, <, >, <=, >=）
     * @param left  左操作数 AST
     * @param right 右操作数 AST
     */
    public static IROpCode cmpOp(String op, ExpressionNode left, ExpressionNode right) {
        boolean useLong = isLongLiteral(left) || isLongLiteral(right);
        Map<String, IROpCode> table = useLong ? IROpCodeMappings.CMP_L64
                : IROpCodeMappings.CMP_I32;
        return table.get(op);
    }

    /* ------------ 内部工具 ------------ */

    private static boolean isLongLiteral(ExpressionNode node) {
        if (node instanceof NumberLiteralNode(String value, NodeContext _)) {
            return value.endsWith("L") || value.endsWith("l");
        }
        return false;                   // 变量暂不处理（后续可扩展符号表查询）
    }
}
