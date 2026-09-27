package com.ajay.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * The ReAct loop: Thought → Action (tool call) → Observation, until the model
 * returns a final answer or {@code maxIterations} is hit.
 *
 * <p>Tool calls run through retry-with-backoff: a flaky tool gets a few chances
 * before its failure becomes the observation. A tool that fails every attempt
 * yields an {@code ERROR: ...} observation and the model decides what to do —
 * recover, try another tool, or give up gracefully.
 */
public final class AgentLoop {

    /** One Thought → Action → Observation triple, for traces and evals. */
    public record Step(String thought, String action, String observation) {}

    /** The outcome of a run. */
    public record AgentResult(String finalAnswer, List<Step> steps, boolean completed) {}

    private final ToolRegistry tools;
    private final int maxToolAttempts;
    private final long toolRetryBaseDelayMs;

    public AgentLoop(ToolRegistry tools) {
        this(tools, 3, 100);
    }

    public AgentLoop(ToolRegistry tools, int maxToolAttempts, long toolRetryBaseDelayMs) {
        this.tools = tools;
        this.maxToolAttempts = maxToolAttempts;
        this.toolRetryBaseDelayMs = toolRetryBaseDelayMs;
    }

    public AgentResult run(String query, ModelAdapter model, int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1");
        }
        List<ModelAdapter.Message> history = new ArrayList<>();
        history.add(ModelAdapter.Message.user(query));
        List<Step> steps = new ArrayList<>();

        for (int i = 0; i < maxIterations; i++) {
            ModelAdapter.ModelResponse response = model.next(List.copyOf(history), tools);
            if (response instanceof ModelAdapter.ModelResponse.FinalAnswer fa) {
                return new AgentResult(fa.text(), List.copyOf(steps), true);
            }
            var call = (ModelAdapter.ModelResponse.ToolCall) response;
            String action = call.toolName() + call.args();
            String observation = invokeWithRetry(call.toolName(), call.args());
            steps.add(new Step(call.thought(), action, observation));
            history.add(ModelAdapter.Message.assistant(
                    "Thought: " + call.thought() + " | Action: " + action));
            history.add(ModelAdapter.Message.toolResult(call.toolName(), observation));
        }
        return new AgentResult(
                "Stopped after " + maxIterations + " iterations without a final answer.",
                List.copyOf(steps), false);
    }

    private String invokeWithRetry(String toolName, java.util.Map<String, String> args) {
        long delayMs = toolRetryBaseDelayMs;
        for (int attempt = 1; ; attempt++) {
            try {
                return tools.get(toolName).call(args);
            } catch (Exception e) {
                if (attempt >= maxToolAttempts) {
                    return "ERROR: tool '" + toolName + "' failed after "
                            + attempt + " attempts: " + e.getMessage();
                }
                System.out.printf("  [tool retry] '%s' attempt %d failed (%s), retrying in %d ms%n",
                        toolName, attempt, e.getMessage(), delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "ERROR: interrupted while retrying '" + toolName + "'";
                }
                delayMs *= 2;
            }
        }
    }
}
