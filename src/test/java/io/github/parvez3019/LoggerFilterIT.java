package io.github.parvez3019;

import io.github.parvez3019.support.TestLoggerFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerFilterIT {

    private final TestLoggerFilter filter = new TestLoggerFilter();

    @AfterEach
    void cleanup() {
        TestLoggerFilter.threadLocal().remove();
        MDC.clear();
    }

    @Test
    void installsLoggerAndCleansMdcAndThreadLocal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TestLoggerFilter.X_REQUEST_ID, "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertEquals("req-123", MDC.get(TestLoggerFilter.X_REQUEST_ID));
            assertNotNull(TestLoggerFilter.Logger());
            TestLoggerFilter.Logger().info("inside");
            assertEquals(1, TestLoggerFilter.Logger().bufferedSize());
        };

        filter.doFilter(request, response, chain);

        assertNull(MDC.get(TestLoggerFilter.X_REQUEST_ID));
        assertNull(TestLoggerFilter.threadLocal().get());
    }

    @Test
    void cleansUpWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get(TestLoggerFilter.X_REQUEST_ID));
        assertNull(TestLoggerFilter.threadLocal().get());
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertNotNull(MDC.get(TestLoggerFilter.X_REQUEST_ID));
            assertTrue(MDC.get(TestLoggerFilter.X_REQUEST_ID).length() > 0);
        };
        filter.doFilter(request, response, chain);
    }
}
