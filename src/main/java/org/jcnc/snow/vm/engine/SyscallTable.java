package org.jcnc.snow.vm.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SyscallTable provides a single, shared source of truth for:
 * - resolving syscall subcommands (numeric opcode or mnemonic name) to an int opcode
 * - determining the value category (I/L/R) a syscall returns for codegen purposes
 *
 * <p>
 * This class is intentionally usable by both the VM and the compiler backend, so that
 * syscall metadata does not drift between layers.
 * </p>
 */
public final class SyscallTable {

    public enum AbiType {
        VOID,
        I8,
        I16,
        I32,
        I64,
        F32,
        F64,
        STRING,
        BYTES,
        ARRAY,
        DICT,
        ANY
    }

    public record SyscallSpec(int opcode, String name, AbiType ret, AbiType[] args, boolean varargs) {
    }

    private static final Map<String, Integer> NAME_TO_OPCODE = new ConcurrentHashMap<>();
    private static final Map<Integer, SyscallSpec> OPCODE_TO_SPEC = new ConcurrentHashMap<>();

    static {
        // 1) Populate name -> opcode from SyscallOpCode constants via reflection.
        for (Field field : SyscallOpCode.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods) || !Modifier.isFinal(mods)) continue;
            if (field.getType() != int.class) continue;
            try {
                int opcode = field.getInt(null);
                NAME_TO_OPCODE.put(field.getName().toUpperCase(Locale.ROOT), opcode);
            } catch (IllegalAccessException ignored) {
            }
        }

        // 2) Register syscall specs (typed ABI surface).
        // Default for unknown opcodes is I32.
        spec(SyscallOpCode.READ, "READ", AbiType.BYTES, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.WRITE, "WRITE", AbiType.I32, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.OPEN, "OPEN", AbiType.I32, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.CLOSE, "CLOSE", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SEEK, "SEEK", AbiType.I64, AbiType.I32, AbiType.I64, AbiType.I32);

        spec(SyscallOpCode.PIPE, "PIPE", AbiType.ARRAY);
        spec(SyscallOpCode.STAT, "STAT", AbiType.DICT, AbiType.STRING);
        spec(SyscallOpCode.FSTAT, "FSTAT", AbiType.DICT, AbiType.I32);
        spec(SyscallOpCode.READDIR, "READDIR", AbiType.ARRAY, AbiType.STRING);

        spec(SyscallOpCode.SELECT, "SELECT", AbiType.DICT, AbiType.ARRAY, AbiType.ARRAY, AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.EPOLL_WAIT, "EPOLL_WAIT", AbiType.ARRAY, AbiType.I32, AbiType.I32, AbiType.I32);

        spec(SyscallOpCode.ARR_LEN, "ARR_LEN", AbiType.I32, AbiType.ARRAY);
        spec(SyscallOpCode.ARR_GET, "ARR_GET", AbiType.ANY, AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.ARR_SET, "ARR_SET", AbiType.I32, AbiType.ARRAY, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.ARR_PUSH, "ARR_PUSH", AbiType.I32, AbiType.ARRAY, AbiType.ANY);
        spec(SyscallOpCode.ARR_POP, "ARR_POP", AbiType.ANY, AbiType.ARRAY);
        spec(SyscallOpCode.ARR_INSERT, "ARR_INSERT", AbiType.I32, AbiType.ARRAY, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.ARR_REMOVE, "ARR_REMOVE", AbiType.ANY, AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.ARR_RESIZE, "ARR_RESIZE", AbiType.I32, AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.ARR_CLEAR, "ARR_CLEAR", AbiType.I32, AbiType.ARRAY);

        spec(SyscallOpCode.STR_LEN, "STR_LEN", AbiType.I32, AbiType.STRING);
        spec(SyscallOpCode.STR_TO_UTF8, "STR_TO_UTF8", AbiType.BYTES, AbiType.STRING);
        spec(SyscallOpCode.UTF8_TO_STR, "UTF8_TO_STR", AbiType.STRING, AbiType.BYTES);
        spec(SyscallOpCode.STR_FROM_CODEPOINT, "STR_FROM_CODEPOINT", AbiType.STRING, AbiType.I32);

        spec(SyscallOpCode.BYTES_LEN, "BYTES_LEN", AbiType.I32, AbiType.BYTES);
        spec(SyscallOpCode.BYTES_GET, "BYTES_GET", AbiType.I8, AbiType.BYTES, AbiType.I32);
        spec(SyscallOpCode.BYTES_SET, "BYTES_SET", AbiType.I32, AbiType.BYTES, AbiType.I32, AbiType.I8);
        spec(SyscallOpCode.BYTES_NEW, "BYTES_NEW", AbiType.BYTES, AbiType.I32);
        spec(SyscallOpCode.BYTES_CONCAT, "BYTES_CONCAT", AbiType.BYTES, AbiType.BYTES, AbiType.BYTES);

        spec(SyscallOpCode.RANDOM_BYTES, "RANDOM_BYTES", AbiType.BYTES, AbiType.I32);

        spec(SyscallOpCode.TIMEOFDAY, "TIMEOFDAY", AbiType.ARRAY);

        spec(SyscallOpCode.STDOUT_WRITE, "STDOUT_WRITE", AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.STDERR_WRITE, "STDERR_WRITE", AbiType.I32, AbiType.ANY);

        spec(SyscallOpCode.MEMINFO, "MEMINFO", AbiType.DICT);

        spec(SyscallOpCode.SEND, "SEND", AbiType.I32, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.RECV, "RECV", AbiType.BYTES, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SENDTO, "SENDTO", AbiType.I32, AbiType.I32, AbiType.ANY, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.RECVFROM, "RECVFROM", AbiType.ARRAY, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.GETPEERNAME, "GETPEERNAME", AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.GETSOCKNAME, "GETSOCKNAME", AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.GETADDRINFO, "GETADDRINFO", AbiType.ARRAY, AbiType.STRING, AbiType.STRING, AbiType.ANY);

        // Returns string or null; represent as ANY (stored in 'R') until nullable types exist.
        spec(SyscallOpCode.GETENV, "GETENV", AbiType.ANY, AbiType.STRING);
        spec(SyscallOpCode.ERRSTR, "ERRSTR", AbiType.STRING);
        spec(SyscallOpCode.ERRNO, "ERRNO", AbiType.I32);
    }

    private SyscallTable() {
    }

    private static void spec(int opcode, String name, AbiType ret, AbiType... args) {
        OPCODE_TO_SPEC.put(opcode, new SyscallSpec(opcode, name, ret, args, false));
    }

    /**
     * Resolves a syscall subcommand token to an opcode.
     * Accepts:
     * - hex numeric string: "0x1001"
     * - decimal numeric string: "4097"
     * - mnemonic name: "READ"
     */
    public static int resolveOpcode(String subcmd) {
        if (subcmd == null) throw new IllegalArgumentException("syscall subcmd is null");
        String token = subcmd.trim();
        if (token.isEmpty()) throw new IllegalArgumentException("syscall subcmd is empty");

        // numeric
        if (token.startsWith("0x") || token.startsWith("0X")) {
            return Integer.parseInt(token.substring(2), 16);
        }
        boolean allDigits = true;
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch < '0' || ch > '9') {
                allDigits = false;
                break;
            }
        }
        if (allDigits) {
            return Integer.parseInt(token);
        }

        // mnemonic
        Integer opcode = NAME_TO_OPCODE.get(token.toUpperCase(Locale.ROOT));
        if (opcode == null) {
            throw new IllegalArgumentException("Unknown syscall subcmd: " + subcmd);
        }
        return opcode;
    }

    /**
     * Returns the codegen prefix for a syscall return value.
     * - 'I' for INT-like values
     * - 'L' for long values
     * - 'R' for reference values (arrays, strings, maps, objects)
     */
    public static SyscallSpec spec(int opcode) {
        return OPCODE_TO_SPEC.get(opcode);
    }

    /**
     * Returns the recommended STORE prefix for a syscall return type.
     * Falls back to 'I' for unknown opcodes (legacy behavior).
     */
    public static char returnStorePrefix(int opcode) {
        SyscallSpec spec = OPCODE_TO_SPEC.get(opcode);
        AbiType ret = (spec == null) ? AbiType.I32 : spec.ret();
        return switch (ret) {
            case VOID -> 'V';
            case I8 -> 'B';
            case I16 -> 'S';
            case I32 -> 'I';
            case I64 -> 'L';
            case F32 -> 'F';
            case F64 -> 'D';
            case STRING, BYTES, ARRAY, DICT, ANY -> 'R';
        };
    }
}
