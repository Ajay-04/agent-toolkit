package com.ajay.agent.tools;

import com.ajay.agent.Tool;
import com.ajay.agent.ToolException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps another tool and fails the first {@code failuresBeforeSuccess} calls.
 * Exists to prove the agent loop's retry-with-backoff actually works — the
 * eval harness asserts the flaky tool eventually succeeds.
 */
public final class FlakyTool implements Tool {

    private final Tool delegate;
    private final int failuresBeforeSuccess;
    private final AtomicInteger calls = new AtomicInteger();

    public FlakyTool(Tool delegate, int failuresBeforeSuccess) {
        this.delegate = delegate;
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String description() {
        return delegate.description();
    }

    @Override
    public String call(Map<String, String> args) throws ToolException {
        int n = calls.incrementAndGet();
        if (n <= failuresBeforeSuccess) {
            throw new ToolException("simulated transient failure (call " + n + ")");
        }
        return delegate.call(args);
    }
}
