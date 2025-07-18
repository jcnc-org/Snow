package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

/**
 * “比较 + 条件跳转” 复合指令: 
 *   if ( left <cmpOp> right ) jump targetLabel;
 * <p>
 * 其中 cmpOp 只能是 IROpCode.CMP_* 六种比较操作码。
 */
public final class IRCompareJumpInstruction extends IRInstruction {

    private final IROpCode cmpOp;                 // CMP_EQ / CMP_NE / CMP_LT / ...
    private final IRVirtualRegister left, right;  // 两个比较操作数
    private final String targetLabel;             // 跳转目标

    public IRCompareJumpInstruction(IROpCode cmpOp,
                                    IRVirtualRegister left,
                                    IRVirtualRegister right,
                                    String targetLabel) {
        this.cmpOp = cmpOp;
        this.left  = left;
        this.right = right;
        this.targetLabel = targetLabel;
    }

    @Override
    public IROpCode op() {
        return cmpOp;
    }

    public IRVirtualRegister left()  { return left;  }
    public IRVirtualRegister right() { return right; }
    public String            label() { return targetLabel; }

    @Override
    public String toString() {
        return cmpOp + " " + left + ", " + right + " -> " + targetLabel;
    }

    /** 暂无访问者实现，留空 */
    @Override
    public void accept(IRVisitor visitor) { /* no-op */ }
}
