package com.ajay.agent.tools;

import com.ajay.agent.Tool;
import com.ajay.agent.ToolException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keyword search over a small in-memory document set. Args: {query: "..."}.
 * Ranks documents by shared-token count — a stand-in for the vector retrieval
 * in the rag-pipeline project; the agent doesn't care how retrieval works.
 */
public final class SearchTool implements Tool {

    private final Map<String, String> documents = new HashMap<>();

    public SearchTool addDocument(String id, String text) {
        documents.put(id, text);
        return this;
    }

    @Override
    public String name() {
        return "search";
    }

    @Override
    public String description() {
        return "Searches the knowledge base. Args: {query: \"refund window\"}.";
    }

    @Override
    public String call(Map<String, String> args) throws ToolException {
        String query = args.get("query");
        if (query == null || query.isBlank()) {
            throw new ToolException("missing required arg: query");
        }
        Set<String> queryTokens = tokens(query);
        return documents.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), overlap(queryTokens, tokens(e.getValue()))))
                .filter(e -> e.getValue() > 0)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> "[" + e.getKey() + "] " + documents.get(e.getKey()))
                .orElse("No matching documents found.");
    }

    private static Set<String> tokens(String text) {
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() > 1)
                .map(SearchTool::singularize)
                .collect(Collectors.toSet());
    }

    /** Naive stemming: "refunds" -> "refund". Enough for a demo; use a real
     * stemmer (or embeddings) for anything serious. */
    private static String singularize(String t) {
        return t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")
                ? t.substring(0, t.length() - 1)
                : t;
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int n = 0;
        for (String t : a) {
            if (b.contains(t)) {
                n++;
            }
        }
        return n;
    }
}
