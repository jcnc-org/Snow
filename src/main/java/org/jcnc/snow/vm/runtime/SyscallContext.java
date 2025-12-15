package org.jcnc.snow.vm.runtime;

/**
 * Thread-local context for enforcing the "no host object leakage" syscall ABI rule.
 */
public record SyscallContext(int opcode, String name, String handlerClassName) {
}