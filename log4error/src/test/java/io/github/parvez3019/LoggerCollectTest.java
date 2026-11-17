package io.github.parvez3019;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoggerCollectTest {

    @Test
    void bufferedInfoPreservesThrowableOnFlush() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            RuntimeException ex = new RuntimeException("boom");
            logger.info("failed op {}", "pay", ex);
            logger.error("request failed");

            ILoggingEvent infoEvent = capture.events().get(0);
            assertEquals("failed op pay", infoEvent.getFormattedMessage());
            assertNotNull(infoEvent.getThrowableProxy());
            assertEquals("boom", ((ThrowableProxy) infoEvent.getThrowableProxy()).getThrowable().getMessage());
        }
    }

    @Test
    void bufferedDebugPreservesThrowableOnFlush() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.debug("dbg {}", 1, new IllegalStateException("bad"));
            logger.error("err");
            assertNotNull(capture.events().get(0).getThrowableProxy());
        }
    }

    @Test
    void argArrayIsDefensivelyCopied() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            Object[] args = new Object[]{"original"};
            logger.info("value {}", args);
            args[0] = "mutated";
            logger.error("flush");
            assertEquals("value original", capture.events().get(0).getFormattedMessage());
        }
    }

    @Test
    void collectWithoutThrowableStoresNullThrowable() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.info("msg {}", "a");
            logger.error("e");
            assertNull(capture.events().get(0).getThrowableProxy());
        }
    }
}
