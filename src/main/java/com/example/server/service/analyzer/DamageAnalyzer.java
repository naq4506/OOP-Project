package com.example.server.service.analyzer;

import com.example.server.dto.AnalysisResponse;
import com.example.server.model.SocialPostEntity;
import com.example.server.stats.DamageStats;

import java.util.List;

public class DamageAnalyzer implements Analyzer<DamageStats> {

    @Override
    public AnalysisResponse<DamageStats> analyze(List<SocialPostEntity> posts) {
        try {
            DamageStats stats = new DamageStats();

            for (SocialPostEntity post : posts) {
                stats.update(post.getDamageType());
            }

            return AnalysisResponse.success(stats);
        } catch (Exception e) {
            return AnalysisResponse.error("Error occurred while analyzing damage: " + e.getMessage());
        }
    }
}

