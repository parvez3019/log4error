package io.github.parvez3019;

import ch.qos.logback.classic.Level;
import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggerCallSiteTest {
    @Test
    void loggerViewsShareBufferAndFlushWithTheirOriginalLoggerNames() {
        try (LogCapture defaultCapture = LogCapture.attach(Logger.class);
             LogCapture serviceCapture = LogCapture.attach(Service.class)) {
            Logger requestLogger = new Logger(10);
            Logger serviceLogger = requestLogger.withLogger(Service.class);
            requestLogger.info("request started");
            serviceLogger.debug("loading customer {}", 42);
            assertEquals(2, requestLogger.bufferedSize());
            assertEquals(0, defaultCapture.events().size());
            assertEquals(0, serviceCapture.events().size());
            serviceLogger.error("request failed");
            assertEquals(1, defaultCapture.events().size());
            assertEquals("request started", defaultCapture.events().get(0).getFormattedMessage());
            assertEquals(2, serviceCapture.events().size());
            assertEquals(Level.DEBUG, serviceCapture.events().get(0).getLevel());
            assertEquals("loading customer 42", serviceCapture.events().get(0).getFormattedMessage());
            assertEquals(Level.ERROR, serviceCapture.events().get(1).getLevel());
            assertEquals("request failed", serviceCapture.events().get(1).getFormattedMessage());
            assertEquals(0, requestLogger.bufferedSize());
        }
    }
    private static final class Service {
    }
}
