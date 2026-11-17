package io.github.parvez3019.spring;

import io.github.parvez3019.Logger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds a fresh buffering {@link Logger} to the request thread and puts a request ID in the MDC.
 * Both are cleared when the request finishes, so pooled threads never leak buffered events.
 */
public class Log4ErrorFilter extends OncePerRequestFilter {

    private final Log4ErrorProperties properties;

    public Log4ErrorFilter(Log4ErrorProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(properties.getRequestIdHeader());
        String requestId = StringUtils.hasText(header) ? header : UUID.randomUUID().toString();
        MDC.put(properties.getMdcKey(), requestId);
        RequestLogger.bind(new Logger(properties.getBufferSize()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(properties.getMdcKey());
            RequestLogger.clear();
        }
    }
}
