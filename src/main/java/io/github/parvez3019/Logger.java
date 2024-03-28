package io.github.parvez3019;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Stack;

@Component
@RequestScope
public class Logger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    private final Stack<InfoLoggerEvent> infoLogs;

    /**
     * No argument constructor for Logger.Class
     */
    public Logger() {
        infoLogs = new Stack<>();
    }

    /**
     * This method will be pushing info logs in to a stack of string
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public void info(String message, Object... obj) {
        Throwable throwableCandidate = MessageFormatter.getThrowableCandidate(obj);
        if (throwableCandidate != null) {
            Object[] trimmedCopy = MessageFormatter.trimmedCopy(obj);
            this.infoLogs.push(new InfoLoggerEvent(message, trimmedCopy));
        } else {
            this.infoLogs.push(new InfoLoggerEvent(message, null));
        }
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
        infoLogs.forEach(p -> {
            LOGGER.info(p.getMessage(), p.getArgArray());
        });
    }

    /**
     * Clear up the info log stack
     */
    public void clearInfoLogStack() {
        infoLogs.clear();
    }

    /**
     * Wrapper over Logger info method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public static void printInfo(String message, Object... obj) {
        LOGGER.info(message, obj);
    }

    /**
     * Wrapper over Logger error method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public static void printError(String message, Object... obj) {
        LOGGER.error(message, obj);
    }

    /**
     * Wrapper over Logger warn method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public static void warn(String message, Object... obj) {
        LOGGER.warn(message, obj);
    }

    /**
     * Wrapper over Logger debug method
     *
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj     - array of objects
     */
    public static void debug(String message, Object... obj) {
        LOGGER.debug(message, obj);
    }
}
