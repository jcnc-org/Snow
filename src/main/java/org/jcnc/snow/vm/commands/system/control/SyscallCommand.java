package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * SyscallCommand —— 虚拟机系统调用分发器
 * <p>
 * 根据 opcode 分发到具体的 SyscallHandler。
 */
public class SyscallCommand implements Command {

    @Override
    public int execute(String[] parts, int pc, OperandStack stack, LocalVariableStore locals, CallStack callStack) {

        if (parts.length < 2) {
            throw new IllegalArgumentException("SYSCALL missing opcode");
        }

        // parts[1] 是 SyscallOpCode 对应的整型值，可以写十六进制或十进制
        int opcode;
        try {
            opcode = Integer.decode(parts[1]); // 支持 "0x1000" / "4096"
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid syscall opcode format: " + parts[1], e);
        }

        SyscallHandler handler = SyscallFactory.getHandler(opcode);

        try {
            handler.handle(stack, locals, callStack);
        } catch (Exception e) {
            SyscallUtils.pushErr(stack, e);
        }

        return pc + 1;
    }
}
