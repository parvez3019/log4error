package org.parvez3019.os;

/**
 * ThreadLocal Instance can be created with the help of this class, this provides a custom wrapper over
 * Logger and ThreadLocal
 */
public class LoggerThreadLocal extends ThreadLocal<Logger>{

    /**
     * @return return TheadLocal instance variable
     */
    @Override
    public Logger get() {
        return super.get();
    }

    /**
     * @param value the value to be stored in the current thread's copy of
     *              this thread-local.
     */
    @Override
    public void set(Logger value) {
        super.set(value);
    }

    /**
     * Will clear Logger info stack and remove logger instance from ThreadLocal Object
     */
    @Override
    public void remove() {
        getLogger().clearInfoLogStack();
        super.remove();
    }

    /**
     * @return a instance of Logger, if logger instance is null, then it will create a new instance,
     * will set it to thread local context and returns it.
     */
    public Logger getLogger() {
        if (super.get() == null) {
            Logger logger = new Logger();
            super.set(logger);
            return logger;
        }
        return super.get();
    }
}
