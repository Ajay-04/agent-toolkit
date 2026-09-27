package com.ajay.agent.eval;

import com.ajay.agent.ModelAdapter;

import java.util.List;
import java.util.function.Function;

/**
 * One scripted agent test.
 *
 * <ul>
 *   <li>{@code modelBehavior} scripts the mock model for this scenario.</li>
 *   <li>{@code expectedToolSequence} asserts the agent called these tools in order
 *       (derived from the recorded steps' actions).</li>
 *   <li>{@code expectedAnswerContains} asserts substrings of the final answer.</li>
 * </ul>
 */
public record EvalScenario(
        String name,
        String query,
        Function<List<ModelAdapter.Message>, ModelAdapter.ModelResponse> modelBehavior,
        List<String> expectedToolSequence,
        List<String> expectedAnswerContains,
        int maxIterations) {

    public EvalScenario(String name,
                        String query,
                        Function<List<ModelAdapter.Message>, ModelAdapter.ModelResponse> modelBehavior,
                        List<String> expectedToolSequence,
                        List<String> expectedAnswerContains) {
        this(name, query, modelBehavior, expectedToolSequence, expectedAnswerContains, 6);
    }
}
