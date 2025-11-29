package com.example.server.service;
import com.example.server.model.SocialPostEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
public interface sentimentAnalysis {
    class SentimentRatio {
        public int positive = 0;
        public int negative = 0;
        public int neutral = 0;
        public double positiveRatio;
        public double negativeRatio;
        public double positiveNegativeRatio;
        public void compute() {
            int total = positive + negative + neutral;
            if (total == 0) return;
            positiveRatio = (double) positive / total;
            negativeRatio = (double) negative / total;
            positiveNegativeRatio = (double) positive / negative;
        }
    }
    class SentimentChange {
        public double positiveDelta;
        public double negativeDelta;
    }
    enum TrendDirection {RISE, FALL, STABLE}
    class SentimentTrend {
        public LocalDate start;
        public LocalDate end;
        public TrendDirection direction;
        public SentimentTrend(LocalDate start, LocalDate end, TrendDirection direction) {
            this.start = start;
            this.end = end;
            this.direction = direction;
        }
    }
 // Holds cumulative sentiment over time.
    public static class CumulativeSentiment {
        public int positive = 0;
        public int negative = 0;
        public int neutral = 0;

        public double positiveRatio;
        public double negativeRatio;
        public double neutralRatio;

        public void computeRatio() {
            int total = positive + negative + neutral;
            if (total == 0) {
                positiveRatio = negativeRatio = neutralRatio = 0;
            } else {
                positiveRatio = (double) positive / total;
                negativeRatio = (double) negative / total;
                neutralRatio = (double) neutral / total;
            }
        }

        public CumulativeSentiment copy() {
            CumulativeSentiment c = new CumulativeSentiment();
            c.positive = this.positive;
            c.negative = this.negative;
            c.neutral = this.neutral;
            c.positiveRatio = this.positiveRatio;
            c.negativeRatio = this.negativeRatio;
            c.neutralRatio = this.neutralRatio;
            return c;
        }
    }
    public static Map<String, Map<LocalDate, CumulativeSentiment>> cumulativeSentimentTimeline(Map<String, Map<LocalDate, SentimentRatio>> dailyRatios) {
        Map<String, Map<LocalDate, CumulativeSentiment>> cumulative = new HashMap<>();
        for (String category : dailyRatios.keySet()) {
            Map<LocalDate, SentimentRatio> daily = dailyRatios.get(category);
            Map<LocalDate, CumulativeSentiment> timeline = new TreeMap<>();
            CumulativeSentiment running = new CumulativeSentiment();
            // Days sorted chronologically
            List<LocalDate> days = daily.keySet().stream().sorted().toList();
            for (LocalDate day : days) {
                SentimentRatio r = daily.get(day);

                // Add today's numbers to cumulative total
                running.positive += r.positive;
                running.negative += r.negative;
                running.neutral += r.neutral;

                running.computeRatio();

                // Store a copy (important to avoid mutability issues)
                timeline.put(day, running.copy());
            }

            cumulative.put(category, timeline);
        }
        return cumulative;
    }

}
