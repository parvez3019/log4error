package io.github.parvez3019;

import ch.qos.logback.classic.Level;
import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerCapTest {

    @Test
    void dropsOldestWhenCapExceededAndWarnsOnce() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger(2);
            logger.info("one");
            logger.info("two");
            logger.info("three");

            assertEquals(2, logger.bufferedSize());
            assertEquals(1, capture.events().stream().filter(e -> e.getLevel() == Level.WARN).count());

            capture.clear();
            logger.info("four");
            assertEquals(0, capture.events().stream().filter(e -> e.getLevel() == Level.WARN).count());

            capture.clear();
            logger.error("fail");
            assertEquals(3, capture.events().size());
            assertEquals("three", capture.events().get(0).getFormattedMessage());
            assertEquals("four", capture.events().get(1).getFormattedMessage());
            assertEquals("fail", capture.events().get(2).getFormattedMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 17})
    void retainsNewestEventsInOrderAfterRepeatedOverflow(int cap) {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger(cap);
            int eventCount = 1000;
            for (int i = 0; i < eventCount; i++) {
                logger.info("event {}", i);
                assertEquals(Math.min(i + 1, cap), logger.bufferedSize());
            }

            assertEquals(1, capture.events().size());
            assertEquals(Level.WARN, capture.events().get(0).getLevel());
            capture.clear();

            logger.printInfoLogs();
            assertEquals(cap, capture.events().size());
            assertEquals(cap, logger.bufferedSize());
            for (int i = 0; i < cap; i++) {
                assertEquals("event " + (eventCount - cap + i),
                        capture.events().get(i).getFormattedMessage());
            }

            capture.clear();
            logger.error("fail");
            assertEquals(cap + 1, capture.events().size());
            assertEquals("event " + (eventCount - cap), capture.events().get(0).getFormattedMessage());
            assertEquals("fail", capture.events().get(cap).getFormattedMessage());
            assertEquals(0, logger.bufferedSize());
        }
    }

    @Test
    void clearingAfterOverflowResetsBufferAndWarning() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            Logger logger = new Logger(2);
            for (int cycle = 0; cycle < 2; cycle++) {
                for (int i = 0; i < 20; i++) {
                    logger.info("cycle {} event {}", cycle, i);
                }
                assertEquals(1, capture.events().size());
                assertEquals(Level.WARN, capture.events().get(0).getLevel());
                capture.clear();

                logger.printInfoLogs();
                assertEquals(2, capture.events().size());
                assertEquals("cycle " + cycle + " event 18", capture.events().get(0).getFormattedMessage());
                assertEquals("cycle " + cycle + " event 19", capture.events().get(1).getFormattedMessage());
                logger.clearInfoLogStack();
                assertEquals(0, logger.bufferedSize());
                capture.clear();
                logger.printInfoLogs();
                assertTrue(capture.events().isEmpty());
            }
        }
    }

    @Test
    void rejectsInvalidCap() {
        assertThrows(IllegalArgumentException.class, () -> new Logger(0));
    }

    @Test
    void defaultCapIsPositive() {
        assertTrue(new Logger().getMaxBufferSize() >= 1);
        assertEquals(Logger.DEFAULT_MAX_BUFFER_SIZE, new Logger().getMaxBufferSize());
    }
}
