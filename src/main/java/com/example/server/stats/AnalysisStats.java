package com.example.server.stats;

import java.util.Map;


public interface AnalysisStats {

    Map<String, Integer> getCounts();

    default String getStatsName() {
        return this.getClass().getSimpleName();
    }
}

