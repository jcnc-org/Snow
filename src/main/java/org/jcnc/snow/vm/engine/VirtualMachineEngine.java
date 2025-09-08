package org.jcnc.snow.vm.engine;

import org.jcnc.snow.vm.execution.CommandExecutionHandler;
import org.jcnc.snow.vm.module.*;

import java.util.List;

/**
 * Virtual-Machine Engine ({@code VirtualMachineEngine})
 *
 * <p>Interprets and executes a list of VM instructions while maintaining
 * a program counter (PC) and the runtime data structures required for
 * operand manipulation and method invocation.</p>
 *
 * <ul>
 *   <li>{@link OperandStack} — stores intermediate values</li>
 *   <li>{@link LocalVariableStore} — holds locals for the <em>current</em>
 *       stack frame</li>
 *   <li>{@link CallStack} — manages stack frames and return addresses</li>
 *   <li>{@link CommandExecutionHandler} — dispatches opcodes</li>
 * </ul>
 * <p>
 * Root-frame contract:
 * <p>
 * A <strong>root stack frame</strong> is pushed <em>once</em> via
 * {@link #ensureRootFrame()} before the first instruction executes
 * and is never popped.  When a {@code RET} executed in the root frame
 * returns {@link #PROGRAM_END}, the main loop exits gracefully.
 */
public class VirtualMachineEngine {

    /* ---------- Constants ---------- */

    /**
     * Sentinel PC value that signals “terminate program gracefully”.
     */
    private static final int PROGRAM_END = Integer.MAX_VALUE;

    /**
     * Sentinel returned by {@link CommandExecutionHandler#handle} to halt immediately.
     */
    private static final int HALT = -1;

    /* ---------- Runtime state ---------- */

    private final OperandStack operandStack;
    private final LocalVariableStore localVariableStore;
    private final CallStack callStack;
    private final CommandExecutionHandler commandExecutionHandler;

    private int programCounter;

    /* ---------- Construction ---------- */

    /**
     * Builds a VM engine with fresh runtime structures.
     */
    public VirtualMachineEngine() {
        this.operandStack = new OperandStack();
        this.callStack = new CallStack();
        this.localVariableStore = new LocalVariableStore(); // shared with root frame
        this.commandExecutionHandler =
                new CommandExecutionHandler(operandStack, localVariableStore, callStack);
        this.programCounter = 0;
    }

    /* package-private accessor used by debug helpers */
    CallStack getCallStack() {
        return callStack;
    }

    /* ---------- Execution ---------- */

    /**
     * Executes the supplied program <em>in place</em>.
     *
     * @param program textual instructions (“opcode arg1 arg2 …”)
     * @throws IllegalArgumentException if {@code program} is null / empty
     */
    public void execute(List<String> program) {

        if (program == null || program.isEmpty())
            throw new IllegalArgumentException("The command list cannot be empty or null.");

        /* Ensure a single root frame is present. */
        ensureRootFrame();

        /* -------- Main interpreter loop -------- */
        while (true) {

            /* graceful termination */
            if (programCounter == PROGRAM_END) break;

            /* bounds check */
            if (programCounter < 0 || programCounter >= program.size()) break;

            /* -------------------------------------------------
             * 1) 取指并忽略空行 / 以 '#' 开头的注释行
             * ------------------------------------------------- */
            String rawLine = program.get(programCounter).trim();
            if (rawLine.isEmpty() || rawLine.startsWith("#")) {
                programCounter++;     // 跳过并继续
                continue;
            }

            String[] parts = rawLine.split(" ");

            if (parts.length < 1) {
                System.err.println("Invalid command format at PC=" + programCounter +
                        " -> Missing opcode");
                break;
            }

            try {
                int opCode = parseOpCode(parts[0]);

                int nextPC = commandExecutionHandler.handle(opCode, parts, programCounter);

                /* HALT / PROGRAM_END → exit */
                if (nextPC == HALT || nextPC == PROGRAM_END) {
                    programCounter = PROGRAM_END;
                    continue;
                }

                /* 如果处理器未修改 PC，则默认顺序执行下一行 */
                programCounter = (nextPC == programCounter) ? programCounter + 1 : nextPC;

            } catch (IllegalArgumentException e) {
                System.err.println("Command error at PC=" + programCounter + " -> "
                        + e.getMessage());
                break;
            }
        }

        /* ---------- compact root locals & print debug info ---------- */
        if (!callStack.isEmpty()) {
            LocalVariableStore rootLvs = callStack.peekFrame().getLocalVariableStore();
            rootLvs.compact();       // trim leading / trailing null slots
        }
    }

    /* ---------- Helper: ensure root frame ---------- */

    /**
     * Pushes the root frame (returnAddress = {@value #PROGRAM_END}) iff it isn’t there yet.
     * This frame is never popped during normal execution.
     */
    private void ensureRootFrame() {
        if (!callStack.isEmpty()) return;           // already initialised

        /* The returnAddress of the root frame must be PROGRAM_END so that the main loop can exit correctly when the root function RETs.*/
        MethodContext rootCtx = new MethodContext("root", null);
        StackFrame rootFrame = new StackFrame(PROGRAM_END, localVariableStore, rootCtx);
        callStack.pushFrame(rootFrame);
    }

    /* ---------- Debug helpers ---------- */

    /**
     * Prints operand stack + call-stack snapshot.
     */
    public void printStack() {
        operandStack.printOperandStack();
        callStack.printCallStack();
    }

    /**
     * Prints the local-variable table of the current top frame.
     */
    public void printLocalVariables() {
        if (callStack.isEmpty()) {
            System.out.println("Local variable table is empty");
            return;
        }
        callStack.peekFrame().getLocalVariableStore().printLv();
    }

    /* ---------- Utility ---------- */

    /**
     * Parses textual opcode to integer.
     */
    private int parseOpCode(String opCodeStr) {
        try {
            return Integer.parseInt(opCodeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid opcode -> " + opCodeStr, e);
        }
    }
}
