package com.example.server.stats;

import com.fasterxml.jackson.annotation.JsonValue; // Import cái này
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
    @JsonValue  // <--- THÊM DÒNG NÀY: Để in map trực tiếp ra JSON
    public Map<String, Integer> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "DamageStats" + counts.toString();
    }
}