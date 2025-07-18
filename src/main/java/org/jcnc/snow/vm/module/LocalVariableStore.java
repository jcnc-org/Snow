package org.jcnc.snow.vm.module;

import org.jcnc.snow.vm.gui.LocalVariableStoreSwing;
import org.jcnc.snow.vm.utils.LoggingUtils;
import org.jcnc.snow.vm.engine.VMMode;

import java.util.ArrayList;
import java.util.Objects;

/**
 * The {@code LocalVariableStore} represents a simple dynamically-sized
 * local-variable table (<em>frame locals</em>) of the VM.
 *
 * <p>It supports random access via {@link #setVariable(int, Object)}
 * / {@link #getVariable(int)} and can <strong>compact</strong> itself
 * by trimming trailing {@code null} slots after execution has finished.</p>
 */
public class LocalVariableStore {

    private final ArrayList<Object> localVariables;
    private final VMMode vmMode;

    /* ---------- construction ---------- */

    public LocalVariableStore(VMMode vmMode, int initialCapacity) {
        this.localVariables = new ArrayList<>(initialCapacity);
        this.vmMode = vmMode;
        handleMode();
    }

    public LocalVariableStore(VMMode vmMode) {
        this.localVariables = new ArrayList<>();
        this.vmMode = vmMode;
        handleMode();
    }

    /* ---------- public API ---------- */

    /** Sets the value at {@code index}, expanding the list if necessary. */
    public void setVariable(int index, Object value) {
        ensureCapacity(index + 1);
        localVariables.set(index, value);
    }

    /* ------------------------------------------------------------
     * 兼容早期实现: VM 指令译码器可直接调用 store / load
     * 而无需关心内部命名差异。
     * ------------------------------------------------------------ */
    public void store(int index, Object value) { setVariable(index, value); }
    public Object load(int index)              { return getVariable(index); }

    /** Returns the value at {@code index}. */
    public Object getVariable(int index) {
        /* 修改点 #1 —— 自动扩容以避免 LOAD 越界异常  */
        if (index < 0)
            throw new IndexOutOfBoundsException("Negative LV index: " + index);
        ensureCapacity(index + 1);
        return localVariables.get(index);   // 可能为 null，符合 JVM 语义
    }

    /** Exposes the backing list (read-only preferred). */
    public ArrayList<Object> getLocalVariables() {
        return localVariables;
    }

    /** Prints every slot to the logger. */
    public void printLv() {
        if (localVariables.isEmpty()) {
            LoggingUtils.logInfo("Local variable table is empty", "");
            return;
        }
        LoggingUtils.logInfo("\n### VM Local Variable Table:", "");
        for (int i = 0; i < localVariables.size(); i++) {
            LoggingUtils.logInfo("",
                    String.format("%d: %s", i, localVariables.get(i)));
        }
    }

    /** Clears all variables (used when a stack frame is popped). */
    public void clearVariables() {
        localVariables.clear();
    }

    /**
     * Compacts the table by <b>removing trailing {@code null} slots</b>.
     * <p>Call this once after program termination (e.g. in
     * {@code VirtualMachineEngine.execute()} before printing) to get
     * cleaner debug output without affecting execution-time indices.</p>
     */
    public void compact() {
        /* 修改点 #2 —— 仅删除“尾部” null，而不是整表过滤 */
        int i = localVariables.size() - 1;
        while (i >= 0 && localVariables.get(i) == null) {
            localVariables.remove(i);
            i--;
        }
    }


    /* ---------- internal helpers ---------- */

    /** Ensures backing list can hold {@code minCapacity} slots. */
    private void ensureCapacity(int minCapacity) {
        /* 修改点 #3 —— 使用 while 循环填充 null，确保 slot 可随机写入 */
        while (localVariables.size() < minCapacity) {
            localVariables.add(null);
        }
    }

    /** Mode-specific UI hook (unchanged). */
    private void handleMode() {
        /* no-op */
        if (Objects.requireNonNull(vmMode) == VMMode.DEBUG) {
            LocalVariableStoreSwing.display(this, "Local Variable Table");
        }
    }
}
