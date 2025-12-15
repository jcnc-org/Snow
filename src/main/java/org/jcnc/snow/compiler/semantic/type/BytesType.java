package org.jcnc.snow.compiler.semantic.type;

/**
 * BytesType represents the Snow runtime Bytes value (byte sequence).
 *
 * <p>Surface syntax currently maps {@code byte[]} to {@code BytesType}.</p>
 */
public final class BytesType implements Type {
    public static final BytesType INSTANCE = new BytesType();

    private BytesType() {
    }

    @Override
    public boolean isCompatible(Type other) {
        return other == this || other == BuiltinType.ANY;
    }

    @Override
    public String name() {
        return "byte[]";
    }

    @Override
    public String toString() {
        return name();
    }
}