package com.ajay.agent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The model side of the agent loop, behind an interface so the loop doesn't
 * care whether it's talking to a mock, a local model, or an API.
 *
 * <p>Given the conversation so far and the available tools, the model either
 * produces a final answer or requests a tool call (ReAct-style: the
 * {@code thought} is the model's reasoning for the call, recorded in the trace).
 */
public interface ModelAdapter {

    /** Decides the next step: final answer or tool call. */
    ModelResponse next(List<Message> history, ToolRegistry tools);

    /** A single message in the conversation. */
    record Message(Role role, String content, String toolName) {
        public static Message user(String content) {
            return new Message(Role.USER, content, null);
        }

        public static Message assistant(String content) {
            return new Message(Role.ASSISTANT, content, null);
        }

        public static Message toolResult(String toolName, String content) {
            return new Message(Role.TOOL, content, toolName);
        }
    }

    enum Role { USER, ASSISTANT, TOOL }

    /** Either {@link FinalAnswer} or {@link ToolCall}. */
    sealed interface ModelResponse permits ModelResponse.FinalAnswer, ModelResponse.ToolCall {
        record FinalAnswer(String text) implements ModelResponse {}
        record ToolCall(String thought, String toolName, Map<String, String> args)
                implements ModelResponse {}
    }

    /**
     * Offline stand-in for a real model. The {@code behavior} function IS the
     * "model": it inspects the conversation history and returns the next step.
     * Scenarios script it; production code swaps in an API adapter.
     */
    final class Mock implements ModelAdapter {
        private final Function<List<Message>, ModelResponse> behavior;

        public Mock(Function<List<Message>, ModelResponse> behavior) {
            this.behavior = behavior;
        }

        @Override
        public ModelResponse next(List<Message> history, ToolRegistry tools) {
            return behavior.apply(history);
        }
    }
}
