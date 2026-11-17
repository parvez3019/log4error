package io.github.parvez3019.spring;

import io.github.parvez3019.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * Configuration for the log4error request filter, bound from {@code log4error.*}.
 */
@ConfigurationProperties(prefix = "log4error")
public class Log4ErrorProperties {

    /**
     * Whether to register the log4error request filter.
     */
    private boolean enabled = true;

    /**
     * Maximum buffered INFO/DEBUG events per request; the oldest are dropped beyond this.
     */
    private int bufferSize = Logger.DEFAULT_MAX_BUFFER_SIZE;

    /**
     * Request header to read an incoming request ID from. A UUID is generated when absent.
     */
    private String requestIdHeader = "x-request-id";

    /**
     * MDC key the request ID is stored under for the duration of the request.
     */
    private String mdcKey = "x-request-id";

    /**
     * Servlet filter order. Runs early so the buffer covers most of the request.
     */
    private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getRequestIdHeader() {
        return requestIdHeader;
    }

    public void setRequestIdHeader(String requestIdHeader) {
        this.requestIdHeader = requestIdHeader;
    }

    public String getMdcKey() {
        return mdcKey;
    }

    public void setMdcKey(String mdcKey) {
        this.mdcKey = mdcKey;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }
}
