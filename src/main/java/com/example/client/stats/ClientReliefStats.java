package com.example.client.stats;

import java.util.HashMap;
import java.util.Map;

public class ClientReliefStats implements ClientStats {
    private final Map<String, Integer> counts = new HashMap<>();

    /**
     * Cập nhật sentiment cho loại hàng cứu trợ
     */
    public void update(String sentiment) {
        if (sentiment == null || sentiment.isEmpty()) sentiment = "neutral";
        counts.put(sentiment, counts.getOrDefault(sentiment, 0) + 1);
    }

    @Override
    public Map<String, Integer> getCounts() {
        return counts;
    }

    public String getStatsName() {
        return "Relief Item Sentiment Statistics";
    }
}

