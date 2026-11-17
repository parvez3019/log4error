package io.github.parvez3019;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request-scoped buffer for INFO/DEBUG logs that are written only when an error occurs
 * (or when explicitly flushed). Happy-path calls avoid I/O.
 */
public class Logger {
    public static final int DEFAULT_MAX_BUFFER_SIZE = 500;

    private final org.slf4j.Logger slf4jLogger;
    private final List<InfoLoggerEvent> collectedLogs;
    private final int maxBufferSize;
    private boolean bufferCapWarned;

    /**
     * Creates a logger that writes through {@code io.github.parvez3019.Logger}.
     */
    public Logger() {
        this(LoggerFactory.getLogger(Logger.class), DEFAULT_MAX_BUFFER_SIZE);
    }

    /**
     * Creates a logger with a custom buffer cap.
     *
     * @param maxBufferSize maximum buffered events; oldest are dropped when exceeded
     */
    public Logger(int maxBufferSize) {
        this(LoggerFactory.getLogger(Logger.class), maxBufferSize);
    }

    /**
     * Creates a logger that flushes through the given SLF4J logger.
     *
     * @param slf4jLogger backend logger used for flush and {@code p*} methods
     */
    public Logger(org.slf4j.Logger slf4jLogger) {
        this(slf4jLogger, DEFAULT_MAX_BUFFER_SIZE);
    }

    /**
     * Creates a logger with an explicit SLF4J backend and buffer cap.
     *
     * @param slf4jLogger    backend logger used for flush and {@code p*} methods
     * @param maxBufferSize maximum buffered events; oldest are dropped when exceeded
     */
    public Logger(org.slf4j.Logger slf4jLogger, int maxBufferSize) {
        if (slf4jLogger == null) {
            throw new IllegalArgumentException("slf4jLogger must not be null");
        }
        if (maxBufferSize < 1) {
            throw new IllegalArgumentException("maxBufferSize must be at least 1");
        }
        this.slf4jLogger = slf4jLogger;
        this.maxBufferSize = maxBufferSize;
        this.collectedLogs = new ArrayList<>();
    }

    /**
     * Factory that preserves call-site logger identity in flushed output.
     *
     * @param clazz class whose name is used for the SLF4J logger
     * @return a new buffering logger
     */
    public static Logger of(Class<?> clazz) {
        return new Logger(LoggerFactory.getLogger(clazz));
    }

    /**
     * Factory wrapping an existing SLF4J logger.
     *
     * @param slf4jLogger backend logger
     * @return a new buffering logger
     */
    public static Logger of(org.slf4j.Logger slf4jLogger) {
        return new Logger(slf4jLogger);
    }

    /**
     * Buffers an INFO message (no I/O until flush/error).
     */
    public void info(String message, Object... obj) {
        collectLog(LoggerLevel.INFO, message, obj);
    }

    /**
     * Buffers a DEBUG message (no I/O until flush/error).
     */
    public void debug(String message, Object... obj) {
        collectLog(LoggerLevel.DEBUG, message, obj);
    }

    /**
     * Flushes buffered logs, writes the error, then clears the buffer.
     */
    public void error(String message, Object... obj) {
        printInfoLogs();
        slf4jLogger.error(message, obj);
        clearInfoLogStack();
    }

    /**
     * Prints buffered logs without clearing the buffer.
     */
    public void printInfoLogs() {
        collectedLogs.forEach(this::printLog);
    }

    /**
     * Discards all buffered logs.
     */
    public void clearInfoLogStack() {
        collectedLogs.clear();
        bufferCapWarned = false;
    }

    /**
     * Immediate INFO write (no buffering).
     */
    public void pInfo(String message, Object... obj) {
        slf4jLogger.info(message, obj);
    }

    /**
     * Immediate ERROR write (no buffering).
     */
    public void pError(String message, Object... obj) {
        slf4jLogger.error(message, obj);
    }

    /**
     * Immediate WARN write (no buffering).
     */
    public void pWarn(String message, Object... obj) {
        slf4jLogger.warn(message, obj);
    }

    /**
     * Immediate DEBUG write (no buffering).
     */
    public void pDebug(String message, Object... obj) {
        slf4jLogger.debug(message, obj);
    }

    /**
     * @return number of currently buffered events
     */
    public int bufferedSize() {
        return collectedLogs.size();
    }

    /**
     * @return configured maximum buffer size
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * @return the SLF4J logger used for flush and pass-through methods
     */
    public org.slf4j.Logger getSlf4jLogger() {
        return slf4jLogger;
    }

    private void collectLog(LoggerLevel level, String message, Object[] obj) {
        Object[] source = obj == null ? new Object[0] : obj;
        Throwable throwableCandidate = MessageFormatter.getThrowableCandidate(source);
        Object[] args;
        if (throwableCandidate != null) {
            Object[] trimmed = MessageFormatter.trimmedCopy(source);
            args = Arrays.copyOf(trimmed, trimmed.length);
        } else {
            args = Arrays.copyOf(source, source.length);
        }
        ensureCapacity();
        collectedLogs.add(new InfoLoggerEvent(level, message, args, throwableCandidate));
    }

    private void ensureCapacity() {
        if (collectedLogs.size() < maxBufferSize) {
            return;
        }
        collectedLogs.remove(0);
        if (!bufferCapWarned) {
            slf4jLogger.warn(
                    "log4error buffer cap ({}) reached; dropping oldest buffered events for this request",
                    maxBufferSize
            );
            bufferCapWarned = true;
        }
    }

    private void printLog(InfoLoggerEvent event) {
        Object[] args = withThrowable(event.argArray(), event.throwable());
        if (event.level() == LoggerLevel.INFO) {
            slf4jLogger.info(event.message(), args);
        } else if (event.level() == LoggerLevel.DEBUG) {
            slf4jLogger.debug(event.message(), args);
        }
    }

    private static Object[] withThrowable(Object[] argArray, Throwable throwable) {
        if (throwable == null) {
            return argArray == null ? new Object[0] : argArray;
        }
        Object[] base = argArray == null ? new Object[0] : argArray;
        Object[] withThrowable = Arrays.copyOf(base, base.length + 1);
        withThrowable[base.length] = throwable;
        return withThrowable;
    }
}
