package io.github.parvez3019;

/**
 * A buffered log event held until flush (typically on {@link Logger#error}).
 *
 * @param level    log level of the buffered event
 * @param message  SLF4J message pattern
 * @param argArray defensive copy of formatting arguments (throwable excluded)
 * @param throwable optional trailing throwable from the original call
 */
public record InfoLoggerEvent(
        LoggerLevel level,
        String message,
        Object[] argArray,
        Throwable throwable
) {
}
