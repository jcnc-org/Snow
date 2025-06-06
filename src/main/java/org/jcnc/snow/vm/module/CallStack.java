package org.jcnc.snow.vm.module;

import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CallStack manages the stack frames, maintaining the function call hierarchy.
 */
public class CallStack {
    private static final int MAX_STACK_DEPTH = 1024; // Stack overflow protection
    private final Deque<StackFrame> stack = new ArrayDeque<>();
    /**
     * Default constructor for creating an instance of CallStack.
     * This constructor is empty as no specific initialization is required.
     */
    public CallStack() {
        // Empty constructor
    }

    /**
     * Pushes a new stack frame onto the call stack.
     *
     * @param frame The stack frame to be pushed.
     * @throws StackOverflowError If the stack exceeds the maximum allowed depth.
     */
    public void pushFrame(StackFrame frame) {
        if (stack.size() >= MAX_STACK_DEPTH) {
            throw new StackOverflowError("Call stack overflow. Maximum depth: " + MAX_STACK_DEPTH);
        }
        stack.push(frame);
    }

    /**
     * Pops the current stack frame from the call stack.
     *
     * @return The popped stack frame.
     */
    public StackFrame popFrame() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Call stack is empty, cannot pop.");
        }
        // Kill this StackFrame LocalVariableStore

        return stack.pop();
    }

    /**
     * Retrieves the current (top) stack frame without removing it.
     *
     * @return The top stack frame.
     */
    public StackFrame peekFrame() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Call stack is empty, cannot peek.");
        }
        return stack.peek();
    }

    /**
     * Takes a snapshot of the current call stack.
     * Useful for debugging during exceptions.
     *
     * @return A string representing the call stack snapshot.
     */
    public String takeSnapshot() {
        StringBuilder snapshot = new StringBuilder("--- Call Stack Snapshot ---\n");
        for (StackFrame frame : stack) {
            snapshot.append("Method: ").append(frame.getMethodContext().methodName())
                    .append(", Return Address: ").append(frame.getReturnAddress())
                    .append(", Locals: ").append(frame.getLocalVariableStore().getLocalVariables())
                    .append("\n");
        }
        return snapshot.toString();
    }

    /**
     * Checks if the call stack is empty.
     *
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Prints the current state of the call stack.
     */
    public void printCallStack() {
        LoggingUtils.logInfo("--- Call Stack State ---", "\n");
        for (StackFrame frame : stack) {
            frame.printFrame();
        }
    }
}