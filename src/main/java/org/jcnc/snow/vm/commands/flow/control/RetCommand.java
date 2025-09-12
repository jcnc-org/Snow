package org.jcnc.snow.vm.commands.flow.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.module.StackFrame;
import org.jcnc.snow.vm.utils.LoggingUtils;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * Implements the {@code RET} instruction (method return).
 *
 * <p><strong>Root-frame protection:</strong> The command first inspects the
 * top frame without popping it. If the frame’s return address is {@code 0}
 * (the root frame) the VM signals normal termination by returning
 * {@link #PROGRAM_END}. The frame is <em>not</em> removed and its locals are
 * kept intact. All other frames are popped and their locals cleared.</p>
 */
public class RetCommand implements Command {

    /**
     * Sentinel value that tells the VM loop to terminate gracefully.
     */
    private static final int PROGRAM_END = Integer.MAX_VALUE;

    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore localVariableStore,
                       CallStack callStack) {

        /* ----- Guard: must have at least one frame ----- */
        if (callStack.isEmpty()) {
            throw new IllegalStateException("RET: call stack is empty.");
        }

        StackFrame topFrame = callStack.peekFrame();

        /* ----- Root frame: do NOT pop, just end program ----- */
        if (topFrame.getReturnAddress() == PROGRAM_END) {
            LoggingUtils.logInfo("", "\nReturn <root>");
            return PROGRAM_END;          // VM main loop should break
        }

        /* ----- Normal frame: pop & clean locals, then resume caller ----- */
        StackFrame finished = callStack.popFrame();
        finished.getLocalVariableStore().clearVariables();

        int returnAddr = finished.getReturnAddress();
//        print("\nReturn " + returnAddr);
        return returnAddr;
    }
}
