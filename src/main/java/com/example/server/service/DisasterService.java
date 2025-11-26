package com.example.server.service;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.FacebookCollector;

import java.time.LocalDate;
import java.util.List;

public class DisasterService {

    private Collector facebookCollector;

    public DisasterService() {
        // Khởi tạo Collector, sau này có thể mở rộng thêm Twitter, YouTube
        this.facebookCollector = new FacebookCollector();
    }

    /**
     * Lấy dữ liệu thô từ Facebook dựa trên thông tin thảm họa và từ khóa
     */
    public List<SocialPostEntity> fetchFacebookPosts(String disasterName, String keyword,
                                                     LocalDate startDate, LocalDate endDate) {
        return facebookCollector.collect(disasterName, keyword, startDate, endDate);
    }

    /**
     * Lấy tất cả dữ liệu từ các nguồn
     */
    public List<SocialPostEntity> fetchAllPosts(String disasterName, String keyword,
                                                LocalDate startDate, LocalDate endDate) {
        // Hiện tại chỉ có Facebook, sau này thêm Twitter, YouTube...
        return fetchFacebookPosts(disasterName, keyword, startDate, endDate);
    }
}

