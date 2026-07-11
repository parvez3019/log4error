package io.github.parvez3019;

import ch.qos.logback.classic.Level;
import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.Test;

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
