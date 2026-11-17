package io.github.parvez3019;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {

    @Test
    void errorFlushesBufferedLogsThenErrorAndClears() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.info("step {}", 1);
            logger.debug("detail {}", "x");
            logger.error("failed {}", "order");

            List<ILoggingEvent> events = capture.events();
            assertEquals(3, events.size());
            assertEquals(Level.INFO, events.get(0).getLevel());
            assertEquals("step 1", events.get(0).getFormattedMessage());
            assertEquals(Level.DEBUG, events.get(1).getLevel());
            assertEquals("detail x", events.get(1).getFormattedMessage());
            assertEquals(Level.ERROR, events.get(2).getLevel());
            assertEquals("failed order", events.get(2).getFormattedMessage());
            assertEquals(0, logger.bufferedSize());
        }
    }

    @Test
    void happyPathBuffersWithoutWriting() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.info("a");
            logger.debug("b");
            assertEquals(2, logger.bufferedSize());
            assertTrue(capture.events().isEmpty());
            logger.clearInfoLogStack();
            assertEquals(0, logger.bufferedSize());
            assertTrue(capture.events().isEmpty());
        }
    }

    @Test
    void printInfoLogsDoesNotClear() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.info("keep");
            logger.printInfoLogs();
            assertEquals(1, capture.events().size());
            assertEquals(1, logger.bufferedSize());
        }
    }

    @Test
    void pMethodsWriteImmediately() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger();
            logger.pInfo("i");
            logger.pWarn("w");
            logger.pDebug("d");
            logger.pError("e");
            assertEquals(0, logger.bufferedSize());
            assertEquals(4, capture.events().size());
            assertEquals(Level.INFO, capture.events().get(0).getLevel());
            assertEquals(Level.WARN, capture.events().get(1).getLevel());
            assertEquals(Level.DEBUG, capture.events().get(2).getLevel());
            assertEquals(Level.ERROR, capture.events().get(3).getLevel());
        }
    }
}
