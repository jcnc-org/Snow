package org.jcnc.snow.vm.execution;


import org.jcnc.snow.vm.factories.CommandFactory;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * Command Execution Handler (CommandExecutionHandler)
 * <p>
 * This class is responsible for invoking the appropriate command object based on the opcode
 * and executing the command logic. It retrieves specific command instances through the {@link CommandFactory},
 * and calls the {@code execute} method of the command to handle the virtual machine's instruction execution process.
 * </p>
 */
public record CommandExecutionHandler(OperandStack operandStack, LocalVariableStore localVariableStore,
                                      CallStack callStack) {
    /**
     * Constructor to initialize the command execution handler.
     *
     * @param operandStack       The stack manager used to manage the virtual machine's stack data.
     * @param localVariableStore The local variable table used to store the local variables used during instruction execution.
     * @param callStack          The call stack used to manage the method invocation hierarchy during execution.
     */
    public CommandExecutionHandler {
    }

    /**
     * Handles the given instruction operation.
     * <p>
     * This method retrieves the corresponding command instance based on the passed opcode
     * and executes it. If the command is invalid or an error occurs during execution,
     * an error message is printed, and -1 is returned to indicate that the program should terminate or an error occurred.
     * </p>
     *
     * @param opCode    The opcode representing the type of instruction to execute (e.g., PUSH, POP, ADD, etc.).
     * @param parts     The array of parameters for the instruction, for example, `10` in `PUSH 10` is the parameter.
     * @param currentPC The current Program Counter (PC) indicating the current instruction's position.
     * @return The address of the next instruction to execute. A return value of -1 indicates termination or an error.
     * @throws IllegalArgumentException If the opcode is invalid, this exception will be thrown.
     */
    public int handle(int opCode, String[] parts, int currentPC) {
        try {
            Command command = CommandFactory
                    .getInstruction(opCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown instruction: " + opCode));

            LocalVariableStore currentLVS = callStack.peekFrame().getLocalVariableStore();

            return command.execute(parts, currentPC, operandStack, currentLVS, callStack);
        } catch (Exception e) {
            System.err.println("Command execution error (PC=" + currentPC + ") -> "
                    + e.getMessage());
            return -1;   // Ensure the VM main loop terminates safely
        }
    }
}
