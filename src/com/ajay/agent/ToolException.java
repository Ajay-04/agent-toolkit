package com.ajay.agent;

/** An expected, retryable tool failure (bad arguments, flaky downstream, ...). */
public final class ToolException extends Exception {
    public ToolException(String message) {
        super(message);
    }

    public ToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
