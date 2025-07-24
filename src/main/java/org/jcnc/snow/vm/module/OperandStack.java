package org.jcnc.snow.vm.module;

import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

/**
 * OperandStack class provides the stack management implementation.
 * <p>
 * It uses Deque (ArrayDeque) to implement basic stack operations, including push, pop, checking if the stack is empty,
 * getting the stack size, and printing the stack's current state.
 * </p>
 */
public class OperandStack {
    private final Deque<Object> stack = new ArrayDeque<>();

    /**
     * Default constructor for creating an instance of OperandStack.
     * This constructor is empty as no specific initialization is required.
     */
    public OperandStack() {
        // Empty constructor
    }

    /**
     * Push operation, adds an integer value to the stack.
     * <p>
     * The push operation is implemented using Deque's push method to add the integer value to the top of the stack.
     * </p>
     *
     * @param value The value to be pushed onto the stack.
     */
    public void push(Object value) {
        stack.push(value);
    }

    /**
     * Pop operation, removes and returns the top element of the stack.
     * <p>
     * The pop operation is implemented using Deque's pop method. If the stack is empty, an exception will be thrown.
     * </p>
     *
     * @return The top element of the stack.
     * @throws IllegalStateException If the stack is empty, an exception is thrown indicating that the pop operation cannot be performed.
     */
    public Object pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty, cannot pop");
        }
        return stack.pop();
    }


    /**
     * Checks if the stack is empty.
     * <p>
     * The isEmpty method of Deque is used to check if the stack is empty.
     * </p>
     *
     * @return Returns true if the stack is empty, otherwise false.
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Gets the current size of the stack.
     * <p>
     * The size method of Deque is used to get the size of the stack, which is the number of elements in the stack.
     * </p>
     *
     * @return The number of elements in the stack.
     */
    public int size() {
        return stack.size();
    }

    /**
     * Prints the current state of the stack.
     * <p>
     * Logs the list of all elements in the stack, showing its content. Uses LoggingUtils to log the information.
     * </p>
     */
    public void printOperandStack() {
        LoggingUtils.logInfo("\n\nOperand Stack state:", stack + "\n");
    }

    /**
     * Retrieves the top value of the stack without removing it.
     * <p>This method returns the top element of the stack without modifying the stack itself.</p>
     *
     * @return The top value of the stack.
     * @throws EmptyStackException if the stack is empty.
     */
    public Object peek() {
        if (stack.isEmpty()) {
            throw new EmptyStackException();
        }
        return stack.peek();
    }
}
