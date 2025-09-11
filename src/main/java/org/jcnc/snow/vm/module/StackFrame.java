package org.jcnc.snow.vm.module;

/**
 * The {@code StackFrame} class represents a single frame in the call stack during program execution.
 * It holds the execution context for a method invocation, including information such as the return
 * address, local variables, operand stack, and method context.
 *
 * <p>This class encapsulates the state of a method call, including the local variables, operand stack,
 * and metadata regarding the method being invoked. It provides a mechanism for managing method invocation
 * details in a virtual machine or interpreter context.</p>
 */
public class StackFrame {

    /**
     * The return address indicates the position in the program to return to after the method execution completes.
     */
    private final int returnAddress;

    /**
     * The {@code LocalVariableStore} holds the local variables specific to the current method invocation.
     */
    private final LocalVariableStore localVariableStore;

    /**
     * The {@code OperandStack} stores the operand stack for the method execution, which contains temporary
     * values used during the evaluation of expressions.
     */
    private final OperandStack operandStack;

    /**
     * The {@code MethodContext} contains metadata about the method, such as its name and the method's arguments.
     */
    private final MethodContext methodContext;

    /**
     * Constructs a new {@code StackFrame} with the specified return address, local variable store, and method context.
     *
     * @param returnAddress      The address to return to after the method execution. It is the point in the program
     *                           where control will resume once the method has completed.
     * @param localVariableStore The local variable store that holds all local variables for the current method
     *                           invocation. It provides a way to access and manipulate local variables.
     * @param methodContext      The method context providing metadata for the method invocation, including the method
     *                           name, parameter types, and other relevant data.
     */
    public StackFrame(int returnAddress, LocalVariableStore localVariableStore, MethodContext methodContext) {
        this.returnAddress = returnAddress;
        this.localVariableStore = localVariableStore;
        this.operandStack = new OperandStack();
        this.methodContext = methodContext;
    }

    /**
     * Retrieves the return address for the current method invocation. This address indicates where control should
     * return after the method completes execution.
     *
     * @return The return address as an integer.
     */
    public int getReturnAddress() {
        return returnAddress;
    }

    /**
     * Retrieves the local variable store associated with this method invocation. The store contains the local
     * variables that are specific to the current execution context.
     *
     * @return The local variable store for the current method.
     */
    public LocalVariableStore getLocalVariableStore() {
        return localVariableStore;
    }

    /**
     * Retrieves the operand stack used by the current method invocation. The operand stack contains temporary values
     * required for evaluating expressions and managing method execution.
     *
     * @return The operand stack used during the method execution.
     */
    public OperandStack getOperandStack() {
        return operandStack;
    }

    /**
     * Retrieves the method context for the current method invocation. The method context includes metadata such as
     * the method's name, parameter types, and other relevant data for the method execution.
     *
     * @return The method context that describes the invoked method.
     */
    public MethodContext getMethodContext() {
        return methodContext;
    }

    /**
     * Prints the details of the stack frame, including the return address, method context, local variables, and
     * operand stack. This method is useful for debugging and inspecting the state of a method invocation.
     */
    public void printFrame() {
//        LoggingUtils.logInfo("----- Stack Frame -----", "");
//        LoggingUtils.logInfo("Return Address:", String.valueOf(returnAddress));
//        LoggingUtils.logInfo("Method Context:", methodContext.toString());
//        LoggingUtils.logInfo("Local Variables:", localVariableStore.getLocalVariables().toString());
//        operandStack.printOperandStack();
    }
}
