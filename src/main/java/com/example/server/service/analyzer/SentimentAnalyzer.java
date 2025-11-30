package com.example.server.service.analyzer;

import com.example.server.dto.AnalysisResponse;
import com.example.server.model.SocialPostEntity;
import com.example.server.stats.SentimentStats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sentiment Analyzer (per day) for humanitarian logistics analysis
 */
public class SentimentAnalyzer implements Analyzer<Map<LocalDate, SentimentStats>> {

    @Override
    public AnalysisResponse<Map<LocalDate, SentimentStats>> analyze(List<SocialPostEntity> posts) {
        try {
            Map<LocalDate, SentimentStats> result = new HashMap<>();

            for (SocialPostEntity post : posts) {
                LocalDate date = post.getPostDate().toLocalDate();

                // sentiment of post
                String postSentiment = post.getSentiment();
                if (postSentiment != null) {
                    result.computeIfAbsent(date, k -> new SentimentStats())
                          .update(postSentiment);
                }

                // sentiment of comments
                if (post.getComments() != null) {
                    for (String comment : post.getComments()) {
                        // if needed, analyze comment separately
                        String commentSentiment = post.getSentiment(); // placeholder: use separate analysis if desired
                        result.computeIfAbsent(date, k -> new SentimentStats())
                              .update(commentSentiment);
                    }
                }
            }

            return AnalysisResponse.success(result);

        } catch (Exception e) {
            return AnalysisResponse.error("Error occurred while analyzing sentiment: " + e.getMessage());
        }
    }
}
