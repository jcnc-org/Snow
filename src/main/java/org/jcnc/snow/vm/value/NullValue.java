package org.jcnc.snow.vm.value;

public final class NullValue implements Value {
    static final NullValue INSTANCE = new NullValue();

    private NullValue() {
    }

    @Override
    public String toString() {
        return "null";
    }
}