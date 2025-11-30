package com.example.server.service.analyzer;

import com.example.server.stats.DamageStats;
import com.example.server.stats.ReliefStats;
import com.example.server.stats.SentimentStats;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnalyzerFactory {

    public enum AnalyzerType {
        DAMAGE,
        RELIEF,
        SENTIMENT,
        RELIEF_TIMELINE
    }

    private final Map<AnalyzerType, Analyzer<?>> analyzerMap = new HashMap<>();

    public AnalyzerFactory() {
        System.out.println("AnalyzerFactory bean created!");
        analyzerMap.put(AnalyzerType.DAMAGE, new DamageAnalyzer());
        analyzerMap.put(AnalyzerType.RELIEF, new ReliefAnalyzer());
        analyzerMap.put(AnalyzerType.SENTIMENT, new SentimentAnalyzer());
        analyzerMap.put(AnalyzerType.RELIEF_TIMELINE, new ReliefTimelineAnalyzer());
    }

    @SuppressWarnings("unchecked")
    public <T> Analyzer<T> getAnalyzer(AnalyzerType type) {
        return (Analyzer<T>) analyzerMap.get(type);
    }

    public boolean containsType(AnalyzerType type) {
        return analyzerMap.containsKey(type);
    }
}
