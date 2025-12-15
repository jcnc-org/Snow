package org.jcnc.snow.vm.runtime;

import org.jcnc.snow.vm.value.Value;

import java.util.Arrays;
import java.util.List;

/**
 * SnowStructObject is a fixed-layout object instance (struct/class storage).
 *
 * <p>Fields are addressed by a zero-based numeric index. The layout metadata (typeName + field count)
 * is for debugging and future reflection; it must not be relied upon for language semantics yet.</p>
 */
public final class SnowStructObject implements HeapObject {
    private final String typeName;
    private final Value[] fields;

    public SnowStructObject(String typeName, int fieldCount) {
        if (fieldCount < 0) throw new IllegalArgumentException("fieldCount must be >= 0");
        this.typeName = typeName == null ? "" : typeName;
        this.fields = new Value[fieldCount];
        Arrays.fill(this.fields, Value.NULL);
    }

    @Override
    public HeapObjectKind kind() {
        return HeapObjectKind.STRUCT;
    }

    public String typeName() {
        return typeName;
    }

    public int fieldCount() {
        return fields.length;
    }

    public Value get(int index) {
        checkIndex(index);
        return fields[index];
    }

    public void set(int index, Value value) {
        checkIndex(index);
        fields[index] = (value == null) ? Value.NULL : value;
    }

    public List<Value> snapshot() {
        return List.of(fields.clone());
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= fields.length) {
            throw new IndexOutOfBoundsException("STRUCT: field index out of bounds: " + index + " (len=" + fields.length + ")");
        }
    }

    @Override
    public String toString() {
        return typeName + fields.length + Arrays.toString(fields);
    }
}
