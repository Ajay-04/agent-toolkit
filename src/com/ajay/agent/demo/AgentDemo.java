package com.ajay.agent.demo;

import com.ajay.agent.AgentLoop;
import com.ajay.agent.ModelAdapter;
import com.ajay.agent.ToolRegistry;
import com.ajay.agent.eval.EvalHarness;
import com.ajay.agent.eval.EvalResult;
import com.ajay.agent.eval.EvalScenario;
import com.ajay.agent.tools.CalculatorTool;
import com.ajay.agent.tools.FlakyTool;
import com.ajay.agent.tools.SearchTool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Runs the eval harness over three scripted scenarios and prints a sample
 * ReAct trace.
 *
 * <p>The "model" is scripted per scenario (a {@code ModelAdapter.Mock}), so the
 * run is fully offline and deterministic. What's real: the tool registry, the
 * ReAct loop, the retry-with-backoff on tool calls, and the scoring.
 */
public class AgentDemo {

    public static void main(String[] args) {
        ToolRegistry tools = new ToolRegistry();
        tools.register(new CalculatorTool());
        tools.register(new SearchTool()
                .addDocument("refunds", "Refunds are issued to the original payment method "
                        + "within 5-7 business days. Contact support with your order number "
                        + "if the refund hasn't arrived.")
                .addDocument("shipping", "Standard shipping takes 3-5 business days and is "
                        + "free over $50. Express shipping arrives in 1-2 business days."));

        AgentLoop loop = new AgentLoop(tools);
        EvalHarness harness = new EvalHarness(tools, loop);

        List<EvalResult> results = harness.run(List.of(
                calculatorScenario(),
                searchScenario(),
                flakyToolScenario(tools)));

        System.out.println("== Eval report ==");
        boolean allPassed = EvalHarness.printReport(results);

        System.out.println("\n== Sample ReAct trace (calculator scenario) ==");
        printTrace(loop, tools, calculatorScenario());

        if (!allPassed) {
            System.exit(1);
        }
    }

    // -- Scenarios ----------------------------------------------------------

    private static EvalScenario calculatorScenario() {
        Function<List<ModelAdapter.Message>, ModelAdapter.ModelResponse> behavior = history -> {
            ModelAdapter.Message last = history.get(history.size() - 1);
            if (last.role() == ModelAdapter.Role.TOOL) {
                return new ModelAdapter.ModelResponse.FinalAnswer(
                        "12 * 8 + 5 = " + last.content().strip() + ".");
            }
            return new ModelAdapter.ModelResponse.ToolCall(
                    "This is arithmetic; I should compute it instead of guessing.",
                    "calculator", Map.of("expression", "12*8+5"));
        };
        return new EvalScenario(
                "calculator",
                "What is 12 * 8 plus 5?",
                behavior,
                List.of("calculator"),
                List.of("101"));
    }

    private static EvalScenario searchScenario() {
        Function<List<ModelAdapter.Message>, ModelAdapter.ModelResponse> behavior = history -> {
            ModelAdapter.Message last = history.get(history.size() - 1);
            if (last.role() == ModelAdapter.Role.TOOL) {
                return new ModelAdapter.ModelResponse.FinalAnswer(
                        "According to the knowledge base: " + last.content());
            }
            return new ModelAdapter.ModelResponse.ToolCall(
                    "I need the refund policy from the knowledge base.",
                    "search", Map.of("query", "refund window"));
        };
        return new EvalScenario(
                "knowledge-base-search",
                "What is the refund window?",
                behavior,
                List.of("search"),
                List.of("5-7 business days"));
    }

    private static EvalScenario flakyToolScenario(ToolRegistry tools) {
        // Swap the calculator for one that fails twice: the loop's retry
        // must absorb the failures and still land the answer.
        tools.register(new FlakyTool(new CalculatorTool(), 2));
        EvalScenario base = calculatorScenario();
        return new EvalScenario(
                "flaky-tool-retry",
                base.query(), base.modelBehavior(), base.expectedToolSequence(),
                base.expectedAnswerContains());
    }

    // -- Trace printing -----------------------------------------------------

    private static void printTrace(AgentLoop loop, ToolRegistry tools, EvalScenario scenario) {
        // Fresh registry: the flaky wrapper from the eval must not leak into the trace.
        ToolRegistry clean = new ToolRegistry();
        clean.register(new CalculatorTool());
        AgentLoop.AgentResult result =
                new AgentLoop(clean).run(scenario.query(),
                        new ModelAdapter.Mock(scenario.modelBehavior()), 6);
        for (AgentLoop.Step step : result.steps()) {
            System.out.println("Thought:     " + step.thought());
            System.out.println("Action:      " + step.action());
            System.out.println("Observation: " + step.observation());
            System.out.println();
        }
        System.out.println("Final answer: " + result.finalAnswer());
    }
}
