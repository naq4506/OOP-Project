package com.example.server.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * Thống kê số lượng post theo loại thiệt hại
 */
public class DamageStats implements AnalysisStats {

    private final Map<String, Integer> counts = new HashMap<>();

    public void update(String damageType) {
        if (damageType == null || damageType.isEmpty()) {
            damageType = "Khác";
        }
        counts.put(damageType, counts.getOrDefault(damageType, 0) + 1);
    }

    @Override
    public Map<String, Integer> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "DamageStats" + counts.toString();
    }
}
