package com.example.server;

import com.example.server.Preprocess.EnhancedPreprocess;
import com.example.server.model.SocialPostEntity;
import com.example.server.service.analyzer.ReliefAnalyzer;
import com.example.server.dto.AnalysisResponse;
import com.example.server.stats.ReliefStats;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class TestReliefAnalysis {

    public static void main(String[] args) {
        SocialPostEntity post1 = new SocialPostEntity();
        post1.setContent("Cần gạo và mì tôm. Thật vui khi nhận hỗ trợ!");
        post1.setComments(new ArrayList<>(Arrays.asList("Rất tích cực", "Tiếp tục hỗ trợ nhé!")));

        SocialPostEntity post2 = new SocialPostEntity();
        post2.setContent("Nhà bị hư hại, tiêu cực quá.");
        post2.setComments(new ArrayList<>(Arrays.asList("Khó khăn quá", "Mất điện nhiều")));

        SocialPostEntity post3 = new SocialPostEntity();
        post3.setContent("Cần thêm nước uống và thuốc men."); 
        post3.setComments(new ArrayList<>(Arrays.asList("Mong sớm được giúp")));

        List<SocialPostEntity> posts = new ArrayList<>();
        posts.add(post1);
        posts.add(post2);
        posts.add(post3);
        EnhancedPreprocess preprocess = new EnhancedPreprocess();
        List<SocialPostEntity> cleanedPosts = preprocess.clean(posts);


        System.out.println("=== Preprocess Output ===");
        for (SocialPostEntity p : cleanedPosts) {
            System.out.println("Content: " + p.getContent());
            System.out.println("ReliefItem: " + p.getReliefItem());
            System.out.println("PostSentiment: " + p.getSentiment());
            System.out.println("CommentSentiments: " + p.getCommentSentiments());
            System.out.println("---------------");
        }


        ReliefAnalyzer analyzer = new ReliefAnalyzer();
        AnalysisResponse<Map<String, ReliefStats>> response = analyzer.analyze(cleanedPosts);


        System.out.println("=== Relief Analysis Result ===");
        Map<String, ReliefStats> data = response.getData();
        for (String reliefItem : data.keySet()) {
            ReliefStats stats = data.get(reliefItem);
            System.out.println("ReliefItem: " + reliefItem);
            System.out.println("Counts: " + stats.getCounts());
            System.out.println("----------------");
        }
    }
}
