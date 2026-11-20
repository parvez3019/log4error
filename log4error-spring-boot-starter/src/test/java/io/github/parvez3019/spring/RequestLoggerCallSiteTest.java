package io.github.parvez3019.spring;

import ch.qos.logback.classic.Level;
import io.github.parvez3019.Logger;
import io.github.parvez3019.spring.support.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestLoggerCallSiteTest {
    @AfterEach
    void clearRequestLogger() {
        RequestLogger.clear();
    }

    @Test
    void classSpecificAccessorSharesRequestBufferAndPreservesLoggerName() {
        try (LogCapture capture = LogCapture.attach(Service.class)) {
            RequestLogger.bind(new Logger(10));
            RequestLogger.get(Service.class).info("loaded customer {}", 42);
            assertEquals(1, RequestLogger.get().bufferedSize());
            RequestLogger.get(Service.class).error("request failed");
            assertEquals(2, capture.events().size());
            assertEquals(Level.INFO, capture.events().get(0).getLevel());
            assertEquals("loaded customer 42", capture.events().get(0).getFormattedMessage());
            assertEquals(Level.ERROR, capture.events().get(1).getLevel());
        }
    }
    private static final class Service {
    }
}
