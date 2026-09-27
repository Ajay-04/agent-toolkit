package com.ajay.agent.eval;

import com.ajay.agent.AgentLoop;
import com.ajay.agent.ModelAdapter;
import com.ajay.agent.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs scripted scenarios against the agent loop and scores them.
 *
 * <p>This is the "unit test" for agent behavior: the model is mocked with a
 * scripted behavior per scenario, so results are deterministic. It checks
 * <em>what the agent did</em> (tool sequence) and <em>what it concluded</em>
 * (final answer), not the model's wording. When you plug in a real model,
 * the same scenarios become regression tests — flaky by nature, so run them
 * N times and track pass rate instead of a single boolean.
 */
public final class EvalHarness {

    private final ToolRegistry tools;
    private final AgentLoop loop;

    public EvalHarness(ToolRegistry tools, AgentLoop loop) {
        this.tools = tools;
        this.loop = loop;
    }

    public List<EvalResult> run(List<EvalScenario> scenarios) {
        List<EvalResult> results = new ArrayList<>();
        for (EvalScenario s : scenarios) {
            ModelAdapter model = new ModelAdapter.Mock(s.modelBehavior());
            AgentLoop.AgentResult result = loop.run(s.query(), model, s.maxIterations());
            results.add(score(s, result));
        }
        return results;
    }

    private EvalResult score(EvalScenario s, AgentLoop.AgentResult result) {
        List<String> failures = new ArrayList<>();

        if (!result.completed()) {
            failures.add("agent did not complete: " + result.finalAnswer());
        }

        List<String> actualTools = result.steps().stream()
                .map(step -> step.action().replaceAll("\\{.*", ""))
                .toList();
        if (!actualTools.equals(s.expectedToolSequence())) {
            failures.add("tool sequence: expected " + s.expectedToolSequence()
                    + " but got " + actualTools);
        }

        for (String expected : s.expectedAnswerContains()) {
            if (!result.finalAnswer().contains(expected)) {
                failures.add("final answer missing expected text: '" + expected + "'");
            }
        }

        return new EvalResult(s.name(), failures.isEmpty(), failures, result.steps().size());
    }

    /** Prints a human-readable report; returns true if every scenario passed. */
    public static boolean printReport(List<EvalResult> results) {
        int passed = 0;
        for (EvalResult r : results) {
            String mark = r.passed() ? "PASS" : "FAIL";
            System.out.printf("[%s] %s (%d steps)%n", mark, r.scenarioName(), r.stepsTaken());
            for (String f : r.failures()) {
                System.out.println("       - " + f);
            }
            if (r.passed()) {
                passed++;
            }
        }
        System.out.printf("%n%d/%d scenarios passed%n", passed, results.size());
        return passed == results.size();
    }
}
