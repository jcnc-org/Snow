package org.jcnc.snow.vm.runtime;

import java.util.Arrays;

public final class SnowBytesObject implements HeapObject {
    private final byte[] bytes;

    public SnowBytesObject(byte[] bytes) {
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    @Override
    public HeapObjectKind kind() {
        return HeapObjectKind.BYTES;
    }

    public int length() {
        return bytes.length;
    }

    public byte get(int index) {
        return bytes[index];
    }

    public void set(int index, byte value) {
        bytes[index] = value;
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public byte[] unsafeBytes() {
        return bytes;
    }
}