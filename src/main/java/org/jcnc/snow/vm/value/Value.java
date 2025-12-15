package org.jcnc.snow.vm.value;

/**
 * Snow runtime Value model (phase-1).
 *
 * <p>
 * All observable Snow values stored in the VM operand stack must be {@link Value}.
 * </p>
 */
public sealed interface Value permits
        NullValue,
        BoolValue,
        ByteValue,
        ShortValue,
        IntValue,
        LongValue,
        FloatValue,
        DoubleValue,
        RefValue {

    NullValue NULL = NullValue.INSTANCE;
}