package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockCollector implements Collector {

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDate startDate, LocalDate endDate) {
        List<SocialPostEntity> posts = new ArrayList<>();

        // Post giả lập từ Twitter
        posts.add(new SocialPostEntity(
        ));

        // Post giả lập từ Facebook
        posts.add(new SocialPostEntity(
        ));
        return posts;
    }
}
