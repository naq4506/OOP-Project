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
                String content = post.getContent();
                String damageType = post.getDamageType();

                System.out.println("=== DEBUG DamageAnalyzer ===");
                System.out.println("Post content: " + content);
                System.out.println("Damage type before update: " + damageType);

                if (damageType == null || damageType.isEmpty()) {
                    System.out.println("-> WARNING: damageType is null/empty. Will be counted as 'Other'.");
                }

                stats.update(damageType);

                System.out.println("Current stats: " + stats.getCounts());
                System.out.println("=======================================");
            }

            return AnalysisResponse.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return AnalysisResponse.error("Error occurred while analyzing damage: " + e.getMessage());
        }
    }
}
