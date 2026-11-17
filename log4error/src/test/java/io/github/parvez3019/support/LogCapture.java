package io.github.parvez3019.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Attaches a ListAppender to a Logback logger for assertions in tests.
 */
public final class LogCapture implements AutoCloseable {
    private final Logger logbackLogger;
    private final ListAppender<ILoggingEvent> listAppender;

    private LogCapture(Class<?> clazz) {
        this.logbackLogger = (Logger) LoggerFactory.getLogger(clazz);
        this.listAppender = new ListAppender<>();
        this.listAppender.start();
        this.logbackLogger.addAppender(listAppender);
    }

    public static LogCapture attach(Class<?> clazz) {
        return new LogCapture(clazz);
    }

    public List<ILoggingEvent> events() {
        return listAppender.list;
    }

    public void clear() {
        listAppender.list.clear();
    }

    @Override
    public void close() {
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
    }
}
