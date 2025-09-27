package org.jcnc.snow.vm.commands.system.control.multiplex.utils;

import java.io.IOException;
import java.nio.channels.Selector;

/**
 * {@code SelectorUtils} 提供 {@link Selector} 的辅助方法，
 * 封装带超时参数的 select 操作。
 *
 * <p><b>语义：</b>
 * 根据 {@code timeoutMs} 参数，调用 {@link Selector#select()} /
 * {@link Selector#selectNow()} / {@link Selector#select(long)}，实现阻塞、立即返回或超时等待。
 * </p>
 *
 * <p><b>参数：</b>
 * <ul>
 *   <li>{@code selector} — 要执行选择操作的 {@link Selector}</li>
 *   <li>{@code timeoutMs} — 超时时间（毫秒）
 *     <ul>
 *       <li>{@code < 0} → 阻塞直到有事件</li>
 *       <li>{@code = 0} → 非阻塞，立即返回</li>
 *       <li>{@code > 0} → 阻塞指定毫秒数，超时后返回</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 返回就绪的 channel 数量。若无就绪事件，则可能返回 0。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>I/O 错误时抛出 {@link IOException}</li>
 * </ul>
 * </p>
 */
public class SelectorUtils {

    /**
     * 根据 {@code timeoutMs} 执行 {@link Selector} 的选择操作。
     *
     * @param selector  选择器
     * @param timeoutMs 超时（<0 = 阻塞，=0 = 立即返回，>0 = 指定毫秒）
     * @return 就绪的 channel 数
     * @throws IOException 发生 I/O 错误时抛出
     */
    public static int selectWithTimeout(Selector selector, int timeoutMs) throws IOException {
        if (timeoutMs < 0) {
            return selector.select();
        } else if (timeoutMs == 0) {
            return selector.selectNow();
        } else {
            return selector.select(timeoutMs);
        }
    }
}
