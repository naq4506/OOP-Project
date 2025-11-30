package com.example.server.service.analyzer;

import com.example.server.dto.AnalysisResponse;
import com.example.server.model.SocialPostEntity;
import com.example.server.stats.ReliefStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReliefAnalyzer implements Analyzer<Map<String, ReliefStats>> {

    @Override
    public AnalysisResponse<Map<String, ReliefStats>> analyze(List<SocialPostEntity> posts) {
        try {
            Map<String, ReliefStats> result = new HashMap<>();

            for (SocialPostEntity post : posts) {
                String reliefItem = post.getReliefItem();
                if (reliefItem == null || reliefItem.isEmpty()) reliefItem = "Other";

                // Update sentiment của post
                result.computeIfAbsent(reliefItem, k -> new ReliefStats())
                      .update(post.getSentiment());

                // Update sentiment của từng comment riêng biệt
                List<String> commentSentiments = post.getCommentSentiments();
                if (commentSentiments != null) {
                    for (String commentSentiment : commentSentiments) {
                        result.computeIfAbsent(reliefItem, k -> new ReliefStats())
                              .update(commentSentiment);
                    }
                }
            }

            return AnalysisResponse.success(result);

        } catch (Exception e) {
            return AnalysisResponse.error("Error occurred while analyzing relief items: " + e.getMessage());
        }
    }
}