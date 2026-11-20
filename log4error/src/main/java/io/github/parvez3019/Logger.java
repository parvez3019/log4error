package io.github.parvez3019;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Request-scoped buffer for INFO/DEBUG logs that are written only when an error occurs
 * (or when explicitly flushed). Happy-path calls avoid I/O.
 */
public class Logger {
    public static final int DEFAULT_MAX_BUFFER_SIZE = 500;

    private final org.slf4j.Logger slf4jLogger;
    private final BufferState buffer;

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
        this(slf4jLogger, new BufferState(maxBufferSize));
    }

    private Logger(org.slf4j.Logger slf4jLogger, BufferState buffer) {
        if (slf4jLogger == null) {
            throw new IllegalArgumentException("slf4jLogger must not be null");
        }
        this.slf4jLogger = slf4jLogger;
        this.buffer = buffer;
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
     * Creates a logger view that uses the given class name while sharing this logger's buffer.
     *
     * @param clazz class whose name is attached to events written through the returned view
     * @return a logger view sharing this logger's request buffer
     */
    public Logger withLogger(Class<?> clazz) {
        return new Logger(LoggerFactory.getLogger(Objects.requireNonNull(clazz, "clazz")), buffer);
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
        buffer.events.forEach(this::printLog);
    }

    /**
     * Discards all buffered logs.
     */
    public void clearInfoLogStack() {
        buffer.events.clear();
        buffer.capWarned = false;
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
        return buffer.events.size();
    }

    /**
     * @return configured maximum buffer size
     */
    public int getMaxBufferSize() {
        return buffer.maxBufferSize;
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
        buffer.events.add(new InfoLoggerEvent(level, message, args, throwableCandidate, slf4jLogger.getName()));
    }

    private void ensureCapacity() {
        if (buffer.events.size() < buffer.maxBufferSize) {
            return;
        }
        buffer.events.remove(0);
        if (!buffer.capWarned) {
            slf4jLogger.warn(
                    "log4error buffer cap ({}) reached; dropping oldest buffered events for this request",
                    buffer.maxBufferSize
            );
            buffer.capWarned = true;
        }
    }

    private void printLog(InfoLoggerEvent event) {
        org.slf4j.Logger eventLogger = LoggerFactory.getLogger(event.loggerName());
        Object[] args = withThrowable(event.argArray(), event.throwable());
        if (event.level() == LoggerLevel.INFO) {
            eventLogger.info(event.message(), args);
        } else if (event.level() == LoggerLevel.DEBUG) {
            eventLogger.debug(event.message(), args);
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
    private static final class BufferState {
        private final List<InfoLoggerEvent> events = new ArrayList<>();
        private final int maxBufferSize;
        private boolean capWarned;

        private BufferState(int maxBufferSize) {
            if (maxBufferSize < 1) {
                throw new IllegalArgumentException("maxBufferSize must be at least 1");
            }
            this.maxBufferSize = maxBufferSize;
        }
    }
}
