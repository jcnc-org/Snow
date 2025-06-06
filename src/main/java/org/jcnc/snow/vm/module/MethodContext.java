package org.jcnc.snow.vm.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata about the current method invocation for debugging purposes.
 * <p>
 * This class holds information about the method name and its arguments, which can be useful for debugging
 * and tracking method invocations in the virtual machine context. If no arguments are provided, an empty list
 * is used to avoid null references.
 * </p>
 *
 * @param methodName The name of the method being invoked.
 * @param arguments  The list of arguments passed to the method. If null, an empty list is used.
 */
public record MethodContext(String methodName, List<Object> arguments) {

    /**
     * Constructs a new MethodContext with the provided method name and arguments.
     * If the provided arguments are null, an empty list is used instead.
     *
     * @param methodName The name of the method being invoked.
     * @param arguments  The list of arguments passed to the method. If null, an empty list is used.
     */
    public MethodContext {
        arguments = arguments != null ? arguments : new ArrayList<>();
    }

    /**
     * Returns a string representation of the method invocation.
     * The format is: "Method: {methodName}, Args: {arguments}"
     *
     * @return A string representation of the method invocation, including its name and arguments.
     */
    @Override
    public String toString() {
        return "Method: " + methodName + ", Args: " + arguments;
    }
}
