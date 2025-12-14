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

    public enum ReturnKind {
        INT,
        LONG,
        REF
    }

    private static final Map<String, Integer> NAME_TO_OPCODE = new ConcurrentHashMap<>();
    private static final Map<Integer, ReturnKind> OPCODE_TO_RETURN = new ConcurrentHashMap<>();

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

        // 2) Declare non-default return kinds.
        // Default is INT for unspecified opcodes.
        mark(ReturnKind.REF,
                SyscallOpCode.READ,
                SyscallOpCode.STAT,
                SyscallOpCode.FSTAT,
                SyscallOpCode.PIPE,
                SyscallOpCode.READLINK,
                SyscallOpCode.GETCWD,
                SyscallOpCode.READDIR,
                SyscallOpCode.SELECT,
                SyscallOpCode.EPOLL_WAIT,
                SyscallOpCode.IO_WAIT,
                SyscallOpCode.RECV,
                SyscallOpCode.RECVFROM,
                SyscallOpCode.GETSOCKOPT,
                SyscallOpCode.GETPEERNAME,
                SyscallOpCode.GETSOCKNAME,
                SyscallOpCode.GETADDRINFO,
                SyscallOpCode.THREAD_JOIN,
                SyscallOpCode.GETENV,
                SyscallOpCode.ERRSTR,
                SyscallOpCode.RANDOM_BYTES,
                SyscallOpCode.MEMINFO,
                SyscallOpCode.TIMEOFDAY,
                SyscallOpCode.STR_TO_UTF8,
                SyscallOpCode.UTF8_TO_STR,
                SyscallOpCode.STR_FROM_CODEPOINT
        );

        mark(ReturnKind.LONG,
                SyscallOpCode.SEEK,
                SyscallOpCode.CLOCK_GETTIME,
                SyscallOpCode.TICK_MS
        );
        // STR_LEN returns INT by default (UTF-8 byte length).
    }

    private SyscallTable() {
    }

    private static void mark(ReturnKind kind, int... opcodes) {
        for (int opcode : opcodes) {
            OPCODE_TO_RETURN.put(opcode, kind);
        }
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
    public static char returnPrefix(int opcode) {
        ReturnKind kind = OPCODE_TO_RETURN.getOrDefault(opcode, ReturnKind.INT);
        return switch (kind) {
            case REF -> 'R';
            case LONG -> 'L';
            case INT -> 'I';
        };
    }
}

