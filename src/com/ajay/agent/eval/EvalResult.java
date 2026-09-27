package com.ajay.agent.eval;

import java.util.List;

/** The scored outcome of one scenario. */
public record EvalResult(String scenarioName, boolean passed, List<String> failures, int stepsTaken) {}
