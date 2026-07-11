package io.github.parvez3019;

/**
 * ThreadLocal holder for a request-scoped {@link Logger}.
 * Create one instance (e.g. in a servlet filter), {@link #set} at request start,
 * and {@link #remove} in {@code finally}.
 */
public class LoggerThreadLocal extends ThreadLocal<Logger> {

    @Override
    public Logger get() {
        return super.get();
    }

    @Override
    public void set(Logger value) {
        super.set(value);
    }

    /**
     * Clears the buffered log stack (if present) and removes the ThreadLocal value.
     * Does not create a Logger when none is bound.
     */
    @Override
    public void remove() {
        Logger logger = super.get();
        if (logger != null) {
            logger.clearInfoLogStack();
        }
        super.remove();
    }

    /**
     * @return the Logger for this thread, creating and binding one if absent
     */
    public Logger getLogger() {
        Logger logger = super.get();
        if (logger == null) {
            logger = new Logger();
            super.set(logger);
        }
        return logger;
    }
}
