package io.github.parvez3019;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
@RequestScope
public class Logger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    private final List<InfoLoggerEvent> collectedLogs;

    /**
     * No argument constructor for Logger.Class
     */
    public Logger() {
        collectedLogs = new LinkedList<>();
    }

    /**
     * This method will be pushing info logs in to a stack of string
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void info(String message, Object... obj) {
        collectLog(LoggerLevel.INFO, message, obj);
    }

    /**
     * This method will be pushing debug logs in to a stack of string
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void debug(String message, Object... obj) {
        collectLog(LoggerLevel.DEBUG, message, obj);
    }

    /**
     * Error method will be print error, along with printing the whole info log stack, and will be responsible
     * for clearing the stack
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void error(String message, Object... obj) {
        printInfoLogs();
        LOGGER.error(message, obj);
        clearInfoLogStack();
    }

    /**
     * Print info log stack
     */
    public void printInfoLogs() {
        collectedLogs.forEach(Logger::printLog);
    }

    /**
     * Clear up the info log stack
     */
    public void clearInfoLogStack() {
        collectedLogs.clear();
    }

    /**
     * Wrapper over Logger info method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void pInfo(String message, Object... obj) {
        LOGGER.info(message, obj);
    }

    /**
     * Wrapper over Logger error method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void pError(String message, Object... obj) {
        LOGGER.error(message, obj);
    }

    /**
     * Wrapper over Logger warn method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void pWarn(String message, Object... obj) {
        LOGGER.warn(message, obj);
    }

    /**
     * Wrapper over Logger debug method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void pDebug(String message, Object... obj) {
        LOGGER.debug(message, obj);
    }

    private void collectLog(LoggerLevel level, String message, Object[] obj) {
        Throwable throwableCandidate = MessageFormatter.getThrowableCandidate(obj);
        if (throwableCandidate != null) {
            Object[] trimmedCopy = MessageFormatter.trimmedCopy(obj);
            this.collectedLogs.add(new InfoLoggerEvent(level, message, trimmedCopy));
        } else {
            this.collectedLogs.add(new InfoLoggerEvent(level, message, obj));
        }
    }

    private static void printLog(InfoLoggerEvent p) {
        if (p.level() == LoggerLevel.INFO) {
            LOGGER.info(p.message(), p.argArray());
        }
        if (p.level() == LoggerLevel.DEBUG) {
            LOGGER.debug(p.message(), p.argArray());
        }
    }
}
