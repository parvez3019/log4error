package io.github.parvez3019.support;

import io.github.parvez3019.Logger;
import io.github.parvez3019.LoggerThreadLocal;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Minimal request filter for integration tests (mirrors examples/LoggerFilterExample).
 */
public class TestLoggerFilter implements Filter {
    public static final String X_REQUEST_ID = "x-request-id";
    private static final LoggerThreadLocal requestLogInfoThreadLocal = new LoggerThreadLocal();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String header = httpRequest.getHeader(X_REQUEST_ID);
        String requestID = (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
        MDC.put(X_REQUEST_ID, requestID);
        requestLogInfoThreadLocal.set(new Logger());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(X_REQUEST_ID);
            requestLogInfoThreadLocal.remove();
        }
    }

    public static Logger Logger() {
        return requestLogInfoThreadLocal.getLogger();
    }

    public static LoggerThreadLocal threadLocal() {
        return requestLogInfoThreadLocal;
    }
}
