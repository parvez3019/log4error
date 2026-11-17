package io.github.parvez3019;

import io.github.parvez3019.support.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerRequestLifecycleIT {

    private final LoggerThreadLocal threadLocal = new LoggerThreadLocal();

    @AfterEach
    void cleanup() {
        threadLocal.remove();
    }

    @Test
    void requestLifecycleFlushesOnErrorAndDoesNotLeakToNextRequest() {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            threadLocal.set(new Logger());
            Logger first = threadLocal.getLogger();
            first.info("req1-a");
            first.debug("req1-b");
            first.error("req1-failed");
            assertEquals(3, capture.events().size());
            threadLocal.remove();

            capture.clear();
            threadLocal.set(new Logger());
            Logger second = threadLocal.getLogger();
            second.info("req2-only");
            assertEquals(1, second.bufferedSize());
            assertTrue(capture.events().isEmpty());
            threadLocal.remove();
            assertEquals(0, second.bufferedSize());
        }
    }
}
