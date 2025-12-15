package com.example.server.service.analyzer;

import com.example.server.dto.AnalysisResponse;
import com.example.server.model.SocialPostEntity;
import com.example.server.stats.ReliefStats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReliefTimelineAnalyzer implements Analyzer<Map<String, Map<LocalDate, ReliefStats>>> {

    @Override
    public AnalysisResponse<Map<String, Map<LocalDate, ReliefStats>>> analyze(List<SocialPostEntity> posts) {
        try {
            Map<String, Map<LocalDate, ReliefStats>> result = new HashMap<>();

            for (SocialPostEntity post : posts) {
                String reliefItem = post.getReliefItem();
                if (reliefItem == null || reliefItem.isEmpty()) reliefItem = "Other";

                LocalDate date = post.getPostDate().toLocalDate();

                result.computeIfAbsent(reliefItem, k -> new HashMap<>())
                      .computeIfAbsent(date, k -> new ReliefStats())
                      .update(post.getSentiment());

                if (post.getComments() != null) {
                    for (String comment : post.getComments()) {
                        String commentSentiment = post.getSentiment(); 
                        result.get(reliefItem)
                              .computeIfAbsent(date, k -> new ReliefStats())
                              .update(commentSentiment);
                    }
                }
            }

            return AnalysisResponse.success(result);

        } catch (Exception e) {
            return AnalysisResponse.error("Error occurred while analyzing relief items over time: " + e.getMessage());
        }
    }
}

