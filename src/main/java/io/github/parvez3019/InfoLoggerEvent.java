package io.github.parvez3019;

public class InfoLoggerEvent {
    private final String message;
    private final Object[] argArray;

    public InfoLoggerEvent(String message, Object[] argArray) {
        this.message = message;
        this.argArray = argArray;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgArray() {
        return argArray;
    }
}
