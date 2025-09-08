package org.jcnc.snow.vm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains a global mapping table: “virtual fd → Java NIO Channel”.
 *
 * <pre>
 *   0  → stdin   (ReadableByteChannel)
 *   1  → stdout  (WritableByteChannel)
 *   2  → stderr  (WritableByteChannel)
 *   3+ → Dynamically allocated at runtime
 * </pre>
 */
public final class FDTable {

    /**
     * Next available fd (0‒2 are reserved for standard streams)
     */
    private static final AtomicInteger NEXT_FD = new AtomicInteger(3);
    /**
     * Main mapping table: fd → Channel
     */
    private static final ConcurrentHashMap<Integer, Channel> MAP = new ConcurrentHashMap<>();

    static {
        // Wrap JVM standard streams as NIO Channels and put them into the table
        MAP.put(0, Channels.newChannel(new BufferedInputStream(System.in)));
        MAP.put(1, Channels.newChannel(new BufferedOutputStream(System.out)));
        MAP.put(2, Channels.newChannel(new BufferedOutputStream(System.err)));
    }

    private FDTable() {
    }

    /**
     * Register a new Channel, returning the allocated virtual fd.
     *
     * @param ch Channel to register
     * @return allocated fd
     */
    public static int register(Channel ch) {
        int fd = NEXT_FD.getAndIncrement();
        MAP.put(fd, ch);
        return fd;
    }

    /**
     * Retrieve the Channel by fd; returns null if fd does not exist.
     *
     * @param fd file descriptor
     * @return Channel or null if not found
     */
    public static Channel get(int fd) {
        return MAP.get(fd);
    }

    /**
     * Close and remove the fd (0‒2 are ignored).
     *
     * @param fd file descriptor to close
     * @throws IOException if an I/O error occurs
     */
    public static void close(int fd) throws IOException {
        if (fd <= 2) return;                                   // Standard streams are managed by the host JVM
        Channel ch = MAP.remove(fd);
        if (ch != null && ch.isOpen()) ch.close();
    }

    /**
     * Similar to dup(oldfd) — returns a new fd referring to the same Channel.
     *
     * @param oldfd old file descriptor to duplicate
     * @return new fd referring to the same Channel
     * @throws IllegalArgumentException if fd does not exist
     */
    public static int dup(int oldfd) {
        Channel ch = MAP.get(oldfd);
        if (ch == null)
            throw new IllegalArgumentException("Bad fd: " + oldfd);
        return register(ch);                                   // Multiple fds refer to the same Channel
    }
}
