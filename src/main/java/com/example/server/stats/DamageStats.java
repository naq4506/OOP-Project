package com.example.server.stats;

import com.fasterxml.jackson.annotation.JsonValue; 
import java.util.HashMap;
import java.util.Map;

public class DamageStats implements AnalysisStats {

    private final Map<String, Integer> counts = new HashMap<>();

    public void update(String damageType) {
        if (damageType == null || damageType.isEmpty()) {
            damageType = "Khác";
        }
        counts.put(damageType, counts.getOrDefault(damageType, 0) + 1);
    }

    @Override
    @JsonValue  
    public Map<String, Integer> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "DamageStats" + counts.toString();
    }
}