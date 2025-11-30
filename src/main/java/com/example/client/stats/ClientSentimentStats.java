package com.example.client.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * Thống kê số lượng post theo sentiment
 */
public class ClientSentimentStats implements ClientStats {

    private final Map<String, Integer> counts = new HashMap<>();

    public void update(String sentiment) {
        if (sentiment == null || sentiment.isEmpty()) {
            sentiment = "neutral";
        }
        counts.put(sentiment, counts.getOrDefault(sentiment, 0) + 1);
    }

    @Override
    public Map<String, Integer> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "SentimentStats" + counts.toString();
    }
}

