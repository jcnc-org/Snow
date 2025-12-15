package org.jcnc.snow.vm.runtime;

public record SnowStringObject(String value) implements HeapObject {
    public SnowStringObject(String value) {
        this.value = value == null ? "" : value;
    }

    @Override
    public HeapObjectKind kind() {
        return HeapObjectKind.STRING;
    }
}