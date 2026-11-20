package io.github.parvez3019.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Log4ErrorAppenderTest {

    @Test
    void buffersInfoAndDebugUntilAnErrorFlushesThemInOrder() {
        CapturingAppender delegate = new CapturingAppender();
        delegate.start();
        Log4ErrorAppender appender = new Log4ErrorAppender("test", 10, List.of(delegate));
        appender.start();

        appender.append(event(Level.INFO, "started"));
        appender.append(event(Level.DEBUG, "loading"));
        assertTrue(delegate.events.isEmpty());
        assertEquals(2, appender.bufferedSize());

        appender.append(event(Level.ERROR, "failed"));

        assertEquals(List.of("started", "loading", "failed"), messages(delegate.events));
        assertEquals(0, appender.bufferedSize());
    }

    @Test
    void successfulRequestCanDiscardBufferedEvents() {
        CapturingAppender delegate = new CapturingAppender();
        delegate.start();
        Log4ErrorAppender appender = new Log4ErrorAppender("test", 10, List.of(delegate));
        appender.start();

        appender.append(event(Level.INFO, "successful request"));
        appender.clearThreadBuffer();
        appender.append(event(Level.ERROR, "next request failed"));

        assertEquals(List.of("next request failed"), messages(delegate.events));
    }

    @Test
    void bufferCapDropsOldestEvents() {
        CapturingAppender delegate = new CapturingAppender();
        delegate.start();
        Log4ErrorAppender appender = new Log4ErrorAppender("test", 2, List.of(delegate));
        appender.start();

        appender.append(event(Level.INFO, "one"));
        appender.append(event(Level.INFO, "two"));
        appender.append(event(Level.INFO, "three"));
        appender.append(event(Level.ERROR, "failed"));

        assertEquals(List.of("two", "three", "failed"), messages(delegate.events));
    }

    @Test
    void warningEventsPassThroughWithoutFlushingTheBuffer() {
        CapturingAppender delegate = new CapturingAppender();
        delegate.start();
        Log4ErrorAppender appender = new Log4ErrorAppender("test", 10, List.of(delegate));
        appender.start();

        appender.append(event(Level.INFO, "buffered"));
        appender.append(event(Level.WARN, "warning"));

        assertEquals(List.of("warning"), messages(delegate.events));
        assertEquals(1, appender.bufferedSize());
        appender.clearThreadBuffer();
    }

    private static LogEvent event(Level level, String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("example.Service")
                .setLevel(level)
                .setMessage(new SimpleMessage(message))
                .build();
    }

    private static List<String> messages(List<LogEvent> events) {
        return events.stream().map(event -> event.getMessage().getFormattedMessage()).toList();
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event);
        }
    }
}
