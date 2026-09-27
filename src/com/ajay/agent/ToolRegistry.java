package com.ajay.agent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named registry of the tools available to an agent run. */
public final class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public Tool get(String name) throws ToolException {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new ToolException("unknown tool: " + name);
        }
        return tool;
    }

    public Collection<Tool> all() {
        return tools.values();
    }

    /** Renders tool definitions for a model prompt (name + description). */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (Tool t : tools.values()) {
            sb.append("- ").append(t.name()).append(": ").append(t.description()).append('\n');
        }
        return sb.toString();
    }
}
