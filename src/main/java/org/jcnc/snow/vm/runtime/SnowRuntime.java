package org.jcnc.snow.vm.runtime;

/**
 * Process-wide Snow runtime context (heap + global runtime services).
 *
 * <p>
 * This is intentionally minimal for the first migration phase.
 * Future backends should provide an equivalent per-VM instance runtime.
 * </p>
 */
public final class SnowRuntime {
    private static final SnowRuntime INSTANCE = new SnowRuntime();

    private final ObjectHeap heap = new ObjectHeap();
    private final ThreadLocal<SyscallContext> syscallContext = new ThreadLocal<>();

    private SnowRuntime() {
    }

    public static SnowRuntime get() {
        return INSTANCE;
    }

    public ObjectHeap heap() {
        return heap;
    }

    public SyscallContext syscallContext() {
        return syscallContext.get();
    }

    public AutoCloseable enterSyscall(int opcode, String name, String handlerClassName) {
        SyscallContext prev = syscallContext.get();
        syscallContext.set(new SyscallContext(opcode, name, handlerClassName));
        return () -> syscallContext.set(prev);
    }
}