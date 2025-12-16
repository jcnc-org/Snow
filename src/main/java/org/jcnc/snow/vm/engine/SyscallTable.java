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
        STRUCT,
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
        // This table is the ABI contract shared by the compiler and VM.
        // Unknown opcodes must NOT silently fall back to a default, otherwise codegen can miscompile STORE/LOAD prefixes.

        // region FD (0x1000–0x10FF)
        spec(SyscallOpCode.OPEN, "OPEN", AbiType.I32, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.READ, "READ", AbiType.BYTES, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.WRITE, "WRITE", AbiType.I32, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.SEEK, "SEEK", AbiType.I64, AbiType.I32, AbiType.I64, AbiType.I32);
        spec(SyscallOpCode.CLOSE, "CLOSE", AbiType.VOID, AbiType.I32);
        spec(SyscallOpCode.STAT, "STAT", AbiType.DICT, AbiType.STRING);
        spec(SyscallOpCode.FSTAT, "FSTAT", AbiType.DICT, AbiType.I32);
        spec(SyscallOpCode.UNLINK, "UNLINK", AbiType.VOID, AbiType.STRING);
        spec(SyscallOpCode.DUP, "DUP", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.DUP2, "DUP2", AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.PIPE, "PIPE", AbiType.ARRAY);
        spec(SyscallOpCode.TRUNCATE, "TRUNCATE", AbiType.VOID, AbiType.STRING, AbiType.I64);
        spec(SyscallOpCode.FTRUNCATE, "FTRUNCATE", AbiType.I32, AbiType.I32, AbiType.I64);
        spec(SyscallOpCode.RENAME, "RENAME", AbiType.I32, AbiType.STRING, AbiType.STRING);
        spec(SyscallOpCode.LINK, "LINK", AbiType.I32, AbiType.STRING, AbiType.STRING);
        spec(SyscallOpCode.SYMLINK, "SYMLINK", AbiType.I32, AbiType.STRING, AbiType.STRING);
        spec(SyscallOpCode.READLINK, "READLINK", AbiType.STRING, AbiType.STRING);
        spec(SyscallOpCode.SET_NONBLOCK, "SET_NONBLOCK", AbiType.I32, AbiType.I32, AbiType.I32);
        // endregion

        // region FS (0x1100–0x11FF)
        spec(SyscallOpCode.MKDIR, "MKDIR", AbiType.I32, AbiType.STRING, AbiType.ANY);
        spec(SyscallOpCode.RMDIR, "RMDIR", AbiType.I32, AbiType.STRING);
        spec(SyscallOpCode.CHDIR, "CHDIR", AbiType.I32, AbiType.STRING);
        spec(SyscallOpCode.GETCWD, "GETCWD", AbiType.STRING);
        spec(SyscallOpCode.READDIR, "READDIR", AbiType.ARRAY, AbiType.STRING);
        spec(SyscallOpCode.CHMOD, "CHMOD", AbiType.I32, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.FCHMOD, "FCHMOD", AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.UTIME, "UTIME", AbiType.I32, AbiType.STRING, AbiType.I64, AbiType.I64);
        // endregion

        // region Console IO (0x1200–0x12FF)
        spec(SyscallOpCode.STDIN_READ, "STDIN_READ", AbiType.STRING);
        spec(SyscallOpCode.STDOUT_WRITE, "STDOUT_WRITE", AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.STDERR_WRITE, "STDERR_WRITE", AbiType.I32, AbiType.ANY);
        // endregion

        // region Multiplex (0x1300–0x13FF)
        spec(SyscallOpCode.SELECT, "SELECT", AbiType.DICT, AbiType.ARRAY, AbiType.ARRAY, AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.EPOLL_CREATE, "EPOLL_CREATE", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.EPOLL_CTL, "EPOLL_CTL", AbiType.I32, AbiType.I32, AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.EPOLL_WAIT, "EPOLL_WAIT", AbiType.ARRAY, AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.IO_WAIT, "IO_WAIT", AbiType.ARRAY, AbiType.ARRAY, AbiType.I32);
        // endregion

        // region Socket / Net (0x1400–0x14FF)
        spec(SyscallOpCode.SOCKET, "SOCKET", AbiType.I32, AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.BIND, "BIND", AbiType.I32, AbiType.I32, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.LISTEN, "LISTEN", AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.ACCEPT, "ACCEPT", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.CONNECT, "CONNECT", AbiType.I32, AbiType.I32, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.SEND, "SEND", AbiType.I32, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.RECV, "RECV", AbiType.BYTES, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SENDTO, "SENDTO", AbiType.I32, AbiType.I32, AbiType.ANY, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.RECVFROM, "RECVFROM", AbiType.ARRAY, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SHUTDOWN, "SHUTDOWN", AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SETSOCKOPT, "SETSOCKOPT", AbiType.I32, AbiType.I32, AbiType.I32, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.GETSOCKOPT, "GETSOCKOPT", AbiType.ANY, AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.GETPEERNAME, "GETPEERNAME", AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.GETSOCKNAME, "GETSOCKNAME", AbiType.ARRAY, AbiType.I32);
        spec(SyscallOpCode.GETADDRINFO, "GETADDRINFO", AbiType.ARRAY, AbiType.STRING, AbiType.STRING, AbiType.ANY);
        // endregion

        // region Process / Thread (0x1500–0x15FF)
        spec(SyscallOpCode.EXIT, "EXIT", AbiType.VOID, AbiType.I32);
        spec(SyscallOpCode.FORK, "FORK", AbiType.I32, AbiType.ARRAY);
        spec(SyscallOpCode.EXEC, "EXEC", AbiType.VOID, AbiType.DICT, AbiType.ARRAY, AbiType.STRING);
        spec(SyscallOpCode.WAIT, "WAIT", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.GETPID, "GETPID", AbiType.I32);
        spec(SyscallOpCode.GETPPID, "GETPPID", AbiType.I32);
        spec(SyscallOpCode.THREAD_CREATE, "THREAD_CREATE", AbiType.I32, AbiType.ANY, AbiType.ANY);
        spec(SyscallOpCode.THREAD_JOIN, "THREAD_JOIN", AbiType.ANY, AbiType.I32);
        spec(SyscallOpCode.SLEEP, "SLEEP", AbiType.I32, AbiType.I32);
        // endregion

        // region Sync (0x1600–0x16FF)
        spec(SyscallOpCode.MUTEX_NEW, "MUTEX_NEW", AbiType.I32);
        spec(SyscallOpCode.MUTEX_LOCK, "MUTEX_LOCK", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.MUTEX_TRYLOCK, "MUTEX_TRYLOCK", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.MUTEX_UNLOCK, "MUTEX_UNLOCK", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.COND_NEW, "COND_NEW", AbiType.I32);
        spec(SyscallOpCode.COND_WAIT, "COND_WAIT", AbiType.I32, AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.COND_SIGNAL, "COND_SIGNAL", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.COND_BROADCAST, "COND_BROADCAST", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SEM_NEW, "SEM_NEW", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SEM_WAIT, "SEM_WAIT", AbiType.I32, AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.SEM_POST, "SEM_POST", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.RWLOCK_NEW, "RWLOCK_NEW", AbiType.I32);
        spec(SyscallOpCode.RWLOCK_RLOCK, "RWLOCK_RLOCK", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.RWLOCK_WLOCK, "RWLOCK_WLOCK", AbiType.I32, AbiType.I32);
        spec(SyscallOpCode.RWLOCK_UNLOCK, "RWLOCK_UNLOCK", AbiType.I32, AbiType.I32);
        // endregion

        // region Time (0x1700–0x17FF)
        spec(SyscallOpCode.CLOCK_GETTIME, "CLOCK_GETTIME", AbiType.I64, AbiType.I32);
        spec(SyscallOpCode.NANOSLEEP, "NANOSLEEP", AbiType.I32, AbiType.I64);
        spec(SyscallOpCode.TIMEOFDAY, "TIMEOFDAY", AbiType.ARRAY);
        spec(SyscallOpCode.TICK_MS, "TICK_MS", AbiType.I64);
        // endregion

        // region Array runtime builtins (0x1800–0x18FF)
        spec(SyscallOpCode.ARR_LEN, "ARR_LEN", AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.ARR_GET, "ARR_GET", AbiType.ANY, AbiType.ANY, AbiType.I32);
        spec(SyscallOpCode.ARR_SET, "ARR_SET", AbiType.I32, AbiType.ANY, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.ARR_PUSH, "ARR_PUSH", AbiType.I32, AbiType.ANY, AbiType.ANY);
        spec(SyscallOpCode.ARR_POP, "ARR_POP", AbiType.ANY, AbiType.ANY);
        spec(SyscallOpCode.ARR_INSERT, "ARR_INSERT", AbiType.I32, AbiType.ANY, AbiType.I32, AbiType.ANY);
        spec(SyscallOpCode.ARR_REMOVE, "ARR_REMOVE", AbiType.ANY, AbiType.ANY, AbiType.I32);
        spec(SyscallOpCode.ARR_RESIZE, "ARR_RESIZE", AbiType.I32, AbiType.ANY, AbiType.I32);
        spec(SyscallOpCode.ARR_CLEAR, "ARR_CLEAR", AbiType.I32, AbiType.ANY);

        // region Object / struct runtime builtins (0x18E0–0x18EF)
        spec(SyscallOpCode.OBJ_NEW, "OBJ_NEW", AbiType.STRUCT, AbiType.STRING, AbiType.I32);
        spec(SyscallOpCode.OBJ_GET, "OBJ_GET", AbiType.ANY, AbiType.STRUCT, AbiType.I32);
        spec(SyscallOpCode.OBJ_SET, "OBJ_SET", AbiType.VOID, AbiType.STRUCT, AbiType.I32, AbiType.ANY);
        // endregion
        // endregion

        // region Sys / Env (0x1900–0x19FF)
        // Returns string or null; represent as ANY (stored in 'R') until nullable types exist.
        spec(SyscallOpCode.GETENV, "GETENV", AbiType.ANY, AbiType.STRING);
        spec(SyscallOpCode.SETENV, "SETENV", AbiType.I32, AbiType.STRING, AbiType.ANY, AbiType.I32);
        spec(SyscallOpCode.NCPU, "NCPU", AbiType.I32);
        spec(SyscallOpCode.RANDOM_BYTES, "RANDOM_BYTES", AbiType.BYTES, AbiType.I32);
        spec(SyscallOpCode.ERRSTR, "ERRSTR", AbiType.STRING);
        spec(SyscallOpCode.ERRNO, "ERRNO", AbiType.I32);
        spec(SyscallOpCode.MEMINFO, "MEMINFO", AbiType.DICT);
        // endregion

        // region String/Bytes runtime builtins (0x1A00–0x1A1F)
        spec(SyscallOpCode.STR_LEN, "STR_LEN", AbiType.I32, AbiType.STRING);
        spec(SyscallOpCode.STR_TO_UTF8, "STR_TO_UTF8", AbiType.BYTES, AbiType.STRING);
        spec(SyscallOpCode.UTF8_TO_STR, "UTF8_TO_STR", AbiType.STRING, AbiType.BYTES);
        spec(SyscallOpCode.STR_FROM_CODEPOINT, "STR_FROM_CODEPOINT", AbiType.STRING, AbiType.I32);

        spec(SyscallOpCode.BYTES_LEN, "BYTES_LEN", AbiType.I32, AbiType.BYTES);
        spec(SyscallOpCode.BYTES_GET, "BYTES_GET", AbiType.I8, AbiType.BYTES, AbiType.I32);
        spec(SyscallOpCode.BYTES_SET, "BYTES_SET", AbiType.I32, AbiType.BYTES, AbiType.I32, AbiType.I8);
        spec(SyscallOpCode.BYTES_NEW, "BYTES_NEW", AbiType.BYTES, AbiType.I32);
        spec(SyscallOpCode.BYTES_CONCAT, "BYTES_CONCAT", AbiType.BYTES, AbiType.BYTES, AbiType.BYTES);
        // endregion
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
        if (spec == null) {
            throw new IllegalStateException("Missing syscall ABI spec for opcode: 0x"
                    + Integer.toHexString(opcode).toUpperCase(Locale.ROOT)
                    + " (register it in SyscallTable)");
        }
        AbiType ret = spec.ret();
        return switch (ret) {
            case VOID -> 'V';
            case I8 -> 'B';
            case I16 -> 'S';
            case I32 -> 'I';
            case I64 -> 'L';
            case F32 -> 'F';
            case F64 -> 'D';
            case STRING, BYTES, ARRAY, DICT, STRUCT, ANY -> 'R';
        };
    }
}
