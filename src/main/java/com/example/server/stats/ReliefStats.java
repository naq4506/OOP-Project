package com.example.server.stats;

import com.fasterxml.jackson.annotation.JsonValue; // Import
import java.util.HashMap;
import java.util.Map;

public class ReliefStats implements AnalysisStats {
    private final Map<String, Integer> counts = new HashMap<>();

    public void update(String sentiment) {
        if (sentiment == null || sentiment.isEmpty()) sentiment = "neutral";
        counts.put(sentiment, counts.getOrDefault(sentiment, 0) + 1);
    }

    @Override
    @JsonValue // <--- THÊM DÒNG NÀY
    public Map<String, Integer> getCounts() {
        return counts;
    }
}