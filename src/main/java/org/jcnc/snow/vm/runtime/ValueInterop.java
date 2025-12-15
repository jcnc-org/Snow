package org.jcnc.snow.vm.runtime;

import org.jcnc.snow.vm.value.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ValueInterop bridges legacy JVM instruction implementations and Snow's RVModel.
 *
 * <p>
 * This class is a temporary compatibility layer: it allows the existing VM instruction set
 * to keep operating on boxed Java primitives and Strings, while ensuring the operand stack
 * stores only {@link Value}.
 * </p>
 */
public final class ValueInterop {

    private ValueInterop() {
    }

    public static Value toValue(Object obj, String source) {
        try {
            return toValue0(obj, source);
        } catch (SnowPanicException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SnowPanicException("RVModel violation at " + source + ": cannot convert host value: "
                    + (obj == null ? "null" : obj.getClass().getName()), e);
        }
    }

    private static Value toValue0(Object obj, String source) {
        switch (obj) {
            case null -> {
                return Value.NULL;
            }
            case Value v -> {
                return v;
            }
            case Integer i -> {
                return new IntValue(i);
            }
            case Long l -> {
                return new LongValue(l);
            }
            case Double d -> {
                return new DoubleValue(d);
            }
            case Float f -> {
                return new FloatValue(f);
            }
            case Short s -> {
                return new ShortValue(s);
            }
            case Byte b -> {
                return new ByteValue(b);
            }
            // Snow currently represents boolean as int (0/1) at the VM instruction level.
            case Boolean b -> {
                return new IntValue(b ? 1 : 0);
            }
            case String s -> {
                int id = SnowRuntime.get().heap().alloc(new SnowStringObject(s));
                return new RefValue(id);
            }
            default -> {
            }
        }

        SyscallContext sc = SnowRuntime.get().syscallContext();
        switch (obj) {
            case byte[] bytes -> {
                if (sc != null) {
                    throw new SnowPanicException("Syscall ABI violation: handler returned host byte[] via " + source
                            + " (syscall=" + sc.name() + ", handler=" + sc.handlerClassName() + ")");
                }
                int id = SnowRuntime.get().heap().alloc(new SnowBytesObject(bytes));
                return new RefValue(id);
            }
            case Map<?, ?> map -> {
                if (sc != null) {
                    throw new SnowPanicException("Syscall ABI violation: handler returned host Map via " + source
                            + " (syscall=" + sc.name() + ", handler=" + sc.handlerClassName() + ")");
                }
                SnowDictObject dict = new SnowDictObject();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    dict.put(key, toValue0(e.getValue(), source));
                }
                int id = SnowRuntime.get().heap().alloc(dict);
                return new RefValue(id);
            }
            case List<?> list -> {
                if (sc != null) {
                    throw new SnowPanicException("Syscall ABI violation: handler returned host List via " + source
                            + " (syscall=" + sc.name() + ", handler=" + sc.handlerClassName() + ")");
                }
                List<Value> out = new ArrayList<>(list.size());
                for (Object e : list) out.add(toValue0(e, source));
                int id = SnowRuntime.get().heap().alloc(new SnowArrayObject(out));
                return new RefValue(id);
            }
            default -> {
            }
        }

        Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            if (sc != null) {
                throw new SnowPanicException("Syscall ABI violation: handler returned host array (" + cls.getComponentType().getName()
                        + "[]) via " + source + " (syscall=" + sc.name() + ", handler=" + sc.handlerClassName() + ")");
            }
            int n = Array.getLength(obj);
            List<Value> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(toValue0(Array.get(obj, i), source));
            int id = SnowRuntime.get().heap().alloc(new SnowArrayObject(out));
            return new RefValue(id);
        }

        throw new SnowPanicException("Unsupported host object in RVModel: " + cls.getName());
    }

    public static Object toJava(Value v) {
        if (v == null) return null;
        return switch (v) {
            case NullValue _ -> null;
            case BoolValue(boolean b) -> b;
            case ByteValue(byte b) -> b;
            case ShortValue(short s) -> s;
            case IntValue(int i) -> i;
            case LongValue(long l) -> l;
            case FloatValue(float f) -> f;
            case DoubleValue(double d) -> d;
            case RefValue(int objectId) -> toJavaHeap(objectId);
        };
    }

    private static Object toJavaHeap(int objectId) {
        HeapObject obj = SnowRuntime.get().heap().get(objectId);
        return switch (obj) {
            // Strings are safe to expose as Java String in the legacy view.
            case SnowStringObject s -> s.value();

            // Non-string heap objects must stay opaque in the legacy view, to avoid host leakage
            // and to preserve reference identity across stores/loads.
            case SnowBytesObject _,
                 SnowArrayObject _,
                 SnowDictObject _,
                 SnowStructObject _ -> new RefValue(objectId);
        };
    }
}
