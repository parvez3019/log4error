package io.github.parvez3019.spring;

import io.github.parvez3019.Logger;
import io.github.parvez3019.LoggerThreadLocal;

/**
 * Static access to the request-scoped {@link Logger} bound by {@link Log4ErrorFilter}.
 *
 * <pre>{@code
 * RequestLogger.get().info("Processing order {}", orderId);
 * ...
 * RequestLogger.get().error("Payment failed for order {}", orderId, ex); // flushes buffered lines
 * }</pre>
 *
 * Outside a request (e.g. a scheduled job or a thread the filter did not see), {@link #get()}
 * binds a new default Logger to the current thread. Call {@link #clear()} when that work is done
 * so pooled threads do not keep buffered events.
 */
public final class RequestLogger {

    private static final LoggerThreadLocal HOLDER = new LoggerThreadLocal();

    private RequestLogger() {
    }

    /**
     * @return the Logger bound to the current thread, creating one if none is bound
     */
    public static Logger get() {
        return HOLDER.getLogger();
    }

    /**
     * @return true when a Logger is bound to the current thread
     */
    public static boolean isBound() {
        return HOLDER.get() != null;
    }

    /**
     * Discards any buffered events and unbinds the Logger from the current thread.
     */
    public static void clear() {
        HOLDER.remove();
    }

    static void bind(Logger logger) {
        HOLDER.set(logger);
    }
}
