package org.jcnc.snow.vm.engine;

/**
 * Signals a Snow-level process exit without terminating the host JVM.
 *
 * <p>Thrown by the {@code EXIT} syscall handler and intercepted by the VM engine to halt execution
 * and record an exit code.</p>
 */
public final class VMExitSignal extends RuntimeException {
    private final int exitCode;

    public VMExitSignal(int exitCode) {
        super("VM exited with code " + exitCode);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}

