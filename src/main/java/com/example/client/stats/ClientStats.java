package com.example.client.stats;

import java.util.Map;


public interface ClientStats {

    Map<String, Integer> getCounts();

    default String getStatsName() {
        return this.getClass().getSimpleName();
    }
}

