package io.github.parvez3019;

public record InfoLoggerEvent(LoggerLevel level, String message, Object[] argArray) {
}
