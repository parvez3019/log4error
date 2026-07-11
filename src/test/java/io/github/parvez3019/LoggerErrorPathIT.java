package io.github.parvez3019;

import ch.qos.logback.classic.Level;
import io.github.parvez3019.support.LogCapture;
import io.github.parvez3019.support.TestLoggerFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggerErrorPathIT {

    private final TestLoggerFilter filter = new TestLoggerFilter();

    @AfterEach
    void cleanup() {
        TestLoggerFilter.threadLocal().remove();
    }

    @Test
    void errorPathFlushesBreadcrumbsThenError() throws Exception {
        try (LogCapture capture = LogCapture.attach(Logger.class)) {
            FilterChain chain = (req, res) -> {
                TestLoggerFilter.Logger().info("a");
                TestLoggerFilter.Logger().info("b");
                TestLoggerFilter.Logger().debug("c");
                TestLoggerFilter.Logger().error("failed");
                assertEquals(0, TestLoggerFilter.Logger().bufferedSize());
            };
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            assertEquals(4, capture.events().size());
            assertEquals(Level.INFO, capture.events().get(0).getLevel());
            assertEquals("a", capture.events().get(0).getFormattedMessage());
            assertEquals("b", capture.events().get(1).getFormattedMessage());
            assertEquals(Level.DEBUG, capture.events().get(2).getLevel());
            assertEquals("c", capture.events().get(2).getFormattedMessage());
            assertEquals(Level.ERROR, capture.events().get(3).getLevel());
            assertEquals("failed", capture.events().get(3).getFormattedMessage());
        }
    }
}
