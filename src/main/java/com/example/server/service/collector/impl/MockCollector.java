package com.example.server.service.collector.impl; // <--- DÒNG NÀY QUAN TRỌNG NHẤT

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;

import java.time.LocalDateTime; // Phải dùng LocalDateTime mới khớp Interface
import java.util.ArrayList;
import java.util.List;

public class MockCollector implements Collector {

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> posts = new ArrayList<>();

        // Fake data 1
        SocialPostEntity post1 = new SocialPostEntity();
        post1.setPlatform("Facebook");
        post1.setContent("Cần cứu trợ gấp tại khu vực A");
        post1.setSentiment("Negative");
        post1.setDamageType("Flood");
        post1.setPostDate(LocalDateTime.now()); 
        posts.add(post1);

        // Fake data 2
        SocialPostEntity post2 = new SocialPostEntity();
        post2.setPlatform("X");
        post2.setContent("Mọi người bình tĩnh, nước đang rút");
        post2.setSentiment("Positive");
        post2.setDamageType("None");
        post2.setPostDate(LocalDateTime.now());
        posts.add(post2);

        System.out.println("--- MOCK DATA COLLECTED ---");
        return posts;
    }
}