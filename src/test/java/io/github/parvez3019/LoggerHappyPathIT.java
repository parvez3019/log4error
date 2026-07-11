package io.github.parvez3019;

import io.github.parvez3019.support.LogCapture;
import io.github.parvez3019.support.TestLoggerFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerHappyPathIT {

    private final TestLoggerFilter filter = new TestLoggerFilter();

    @AfterEach
    void cleanup() {
        TestLoggerFilter.threadLocal().remove();
    }

    @Test
    void happyPathDoesNotWriteBufferedLogs() throws Exception {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            FilterChain chain = (req, res) -> {
                TestLoggerFilter.Logger().info("breadcrumb {}", 1);
                TestLoggerFilter.Logger().debug("detail {}", 2);
            };
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
            assertTrue(capture.events().isEmpty());
        }
    }
}
