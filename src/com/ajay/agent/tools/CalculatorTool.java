package com.ajay.agent.tools;

import com.ajay.agent.Tool;
import com.ajay.agent.ToolException;

import java.util.Map;

/**
 * Evaluates arithmetic expressions ({@code expression} arg) with a small
 * hand-written recursive-descent parser: +, -, *, /, parentheses, decimals.
 *
 * <p>Deliberately not {@code ScriptEngine}: no arbitrary code execution from
 * model-generated input.
 */
public final class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "Evaluates an arithmetic expression. Args: {expression: \"12*8+5\"}.";
    }

    @Override
    public String call(Map<String, String> args) throws ToolException {
        String expr = args.get("expression");
        if (expr == null || expr.isBlank()) {
            throw new ToolException("missing required arg: expression");
        }
        try {
            double value = new Parser(expr).parse();
            // Print integers without a decimal point.
            return value == Math.rint(value)
                    ? Long.toString(Math.round(value))
                    : Double.toString(value);
        } catch (IllegalArgumentException e) {
            throw new ToolException("bad expression '" + expr + "': " + e.getMessage());
        }
    }

    /** expression = term (('+' | '-') term)* ; term = factor (('*' | '/') factor)* ... */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s.replaceAll("\\s+", "");
        }

        double parse() {
            double v = expression();
            if (pos != s.length()) {
                throw new IllegalArgumentException("unexpected char at " + pos);
            }
            return v;
        }

        private double expression() {
            double v = term();
            while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                char op = s.charAt(pos++);
                double rhs = term();
                v = op == '+' ? v + rhs : v - rhs;
            }
            return v;
        }

        private double term() {
            double v = factor();
            while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) {
                char op = s.charAt(pos++);
                double rhs = factor();
                if (op == '/' && rhs == 0) {
                    throw new IllegalArgumentException("division by zero");
                }
                v = op == '*' ? v * rhs : v / rhs;
            }
            return v;
        }

        private double factor() {
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                double v = expression();
                if (pos >= s.length() || s.charAt(pos) != ')') {
                    throw new IllegalArgumentException("missing ')'");
                }
                pos++;
                return v;
            }
            int start = pos;
            while (pos < s.length()
                    && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("expected number at " + pos);
            }
            return Double.parseDouble(s.substring(start, pos));
        }
    }
}
