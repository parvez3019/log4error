package org.parvez3019.os;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Stack;

@Component
@RequestScope
public class Logger {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    private final Stack<String> infoLogs;

    /**
     * No argument constructor for Logger.Class
     */
    public Logger() {
        infoLogs = new Stack<>();
    }

    /**
     * This method will be pushing info logs in to a stack of string
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
     */
    public void info(String message, Object... obj) {
        this.infoLogs.push(MessageFormatter.arrayFormat(message, obj).getMessage());
    }


    /**
     * Error method will be print error, along with printing the whole info log stack, and will be responsible
     * for clearing the stack
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
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
        infoLogs.forEach(LOGGER::info);
    }

    /**
     * Clear up the info log stack
     */
    public void clearInfoLogStack() {
        infoLogs.clear();
    }

    /**
     * Wrapper over Logger info method
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
     */
    public static void printInfo(String message, Object... obj) {
        LOGGER.info(message, obj);
    }

    /**
     * Wrapper over Logger error method
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
     */
    public static void printError(String message, Object... obj) {
        LOGGER.error(message, obj);
    }

    /**
     * Wrapper over Logger warn method
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
     */
    public static void warn(String message, Object... obj) {
        LOGGER.warn(message, obj);
    }

    /**
     * Wrapper over Logger debug method
     * @param message takes the logs message in pattern format eg "Exception occurred :{}, at time: {}, ex, time.now()"
     * @param obj - array of objects
     */
    public static void debug(String message, Object... obj) {
        LOGGER.debug(message, obj);
    }
}
