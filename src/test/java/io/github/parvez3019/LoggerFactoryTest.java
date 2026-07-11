package io.github.parvez3019;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggerFactoryTest {

    @Test
    void noArgUsesDefaultLoggerName() {
        Logger logger = new Logger();
        assertEquals(Logger.class.getName(), logger.getSlf4jLogger().getName());
    }

    @Test
    void ofClassUsesCallSiteName() {
        Logger logger = Logger.of(LoggerFactoryTest.class);
        assertEquals(LoggerFactoryTest.class.getName(), logger.getSlf4jLogger().getName());
    }

    @Test
    void ofSlf4jLoggerUsesProvidedBackend() {
        org.slf4j.Logger backend = LoggerFactory.getLogger("custom.logger");
        Logger logger = Logger.of(backend);
        assertEquals("custom.logger", logger.getSlf4jLogger().getName());
    }

    @Test
    void nullSlf4jLoggerRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Logger((org.slf4j.Logger) null));
    }
}
