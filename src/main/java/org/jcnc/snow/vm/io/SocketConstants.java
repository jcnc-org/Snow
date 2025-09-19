package org.jcnc.snow.vm.io;

/**
 * {@code SocketConstants} 定义常用的 socket 协议族和类型常量，供虚拟机所有
 * 网络相关系统调用统一引用。
 *
 * <p>
 * <b>协议族（family）：</b>
 * <ul>
 *   <li>{@link #AF_INET}   ：IPv4，值为 2</li>
 *   <li>{@link #AF_INET6}  ：IPv6，值为 10</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>套接字类型（type）：</b>
 * <ul>
 *   <li>{@link #SOCK_STREAM} ：流式 socket，通常用于 TCP，值为 1</li>
 *   <li>{@link #SOCK_DGRAM}  ：数据报 socket，通常用于 UDP，值为 2</li>
 * </ul>
 * </p>
 */
public final class SocketConstants {

    /**
     * IPv4 协议族（family）常量
     */
    public static final int AF_INET = 2;

    /**
     * IPv6 协议族（family）常量
     */
    public static final int AF_INET6 = 10;

    /**
     * 流式（TCP）套接字类型常量
     */
    public static final int SOCK_STREAM = 1;

    /**
     * 数据报（UDP）套接字类型常量
     */
    public static final int SOCK_DGRAM = 2;
}
