package io.github.parvez3019.example;

import io.github.parvez3019.Logger;
import io.github.parvez3019.LoggerThreadLocal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class LoggerFilterExample extends OncePerRequestFilter {
    private static final String X_REQUEST_ID = "x-request-id";
    private static final LoggerThreadLocal requestLogInfoThreadLocal = new LoggerThreadLocal();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean headerFound = StringUtils.hasLength(request.getHeader(X_REQUEST_ID));
        String requestID = headerFound ? request.getHeader(X_REQUEST_ID) : UUID.randomUUID().toString();
        MDC.put(X_REQUEST_ID, requestID);
        requestLogInfoThreadLocal.set(new Logger());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(X_REQUEST_ID);
            requestLogInfoThreadLocal.remove();
        }
    }

    /**
     * @return will return you the logger instance that can statically imported in your classes.
     */
    public static Logger Logger() {
        return requestLogInfoThreadLocal.getLogger();
    }

}

