package com.ajay.agent;

/** A tool the agent can call. */
public interface Tool {

    /** Unique name the model uses to invoke this tool, e.g. "calculator". */
    String name();

    /** One-line description shown to the model. */
    String description();

    /**
     * Executes the tool. Throw {@link ToolException} for expected failures
     * (bad args, downstream error) — the agent loop retries those.
     */
    String call(java.util.Map<String, String> args) throws ToolException;
}
