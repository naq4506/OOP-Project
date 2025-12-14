package com.example.server.service;

import com.example.server.Preprocess.Preprocess;
import com.example.server.dto.AnalysisRequest;
import com.example.server.dto.AnalysisResponse;
import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.CollectorFactory;
import com.example.server.service.analyzer.Analyzer;
import com.example.server.service.analyzer.AnalyzerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class DisasterService {

    private final Preprocess preprocess;
    private final AnalyzerFactory analyzerFactory;

    public DisasterService(Preprocess preprocess, AnalyzerFactory analyzerFactory) {
        this.preprocess = preprocess;
        this.analyzerFactory = analyzerFactory;
    }

    // ---------------------------------------------------------
    // XỬ LÝ CHÍNH CHO CLIENT MỚI (Dùng analysisType)
    // ---------------------------------------------------------
    public AnalysisResponse<Map<String, Object>> processAllAnalysis(AnalysisRequest request) {
        
        // 1. Thu thập dữ liệu (Crawl) & Tiền xử lý (Clean/Normalize)
        System.out.println(">>> [Service] Bắt đầu thu thập dữ liệu...");
        List<SocialPostEntity> allPosts = collectAndPreprocess(request);
        System.out.println(">>> [Service] Thu thập xong: " + allPosts.size() + " bài viết.");

        Map<String, Object> resultMap = new HashMap<>();
        
        // 2. Xác định loại phân tích từ Request
        String analysisTypeStr = request.getAnalysisType();
        if (analysisTypeStr == null) {
            return AnalysisResponse.error("Analysis Type is null");
        }

        try {
            // Chuyển String sang Enum (VD: "SENTIMENT" -> AnalyzerType.SENTIMENT)
            AnalyzerFactory.AnalyzerType type = AnalyzerFactory.AnalyzerType.valueOf(analysisTypeStr.toUpperCase());
            
            // Lấy Analyzer tương ứng từ Factory
            // Ép kiểu về Analyzer<Object> để chạy chung
            @SuppressWarnings("unchecked")
            Analyzer<Object> analyzer = (Analyzer<Object>) analyzerFactory.getAnalyzer(type);
            
            if (analyzer != null) {
                // Chạy phân tích
                AnalysisResponse<Object> resp = analyzer.analyze(allPosts);
                
                if (resp.isSuccess()) {
                    // Đưa kết quả vào Map với Key là tên loại phân tích (để Client lấy ra)
                    resultMap.put(type.name(), resp.getData());
                } else {
                    return AnalysisResponse.error("Analyzer Error: " + resp.getErrorMessage());
                }
            } else {
                return AnalysisResponse.error("Không tìm thấy Analyzer cho loại: " + type);
            }

        } catch (IllegalArgumentException e) {
            return AnalysisResponse.error("Loại phân tích không hợp lệ: " + analysisTypeStr);
        } catch (Exception e) {
            e.printStackTrace();
            return AnalysisResponse.error("Lỗi trong quá trình phân tích: " + e.getMessage());
        }

        return AnalysisResponse.success(resultMap);
    }

    // ---------------------------------------------------------
    // LOGIC THU THẬP & TIỀN XỬ LÝ (GIỮ NGUYÊN)
    // ---------------------------------------------------------
    private List<SocialPostEntity> collectAndPreprocess(AnalysisRequest request) {
        List<SocialPostEntity> allPosts = new ArrayList<>();

        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        String disasterName = request.getDisasterName();
        String keyword = request.getKeyword() != null ? request.getKeyword() : "";

        if (request.getPlatforms() != null) {
            for (String platform : request.getPlatforms()) {
                Collector collector = CollectorFactory.getCollector(platform);
                if (collector == null) continue;

                // Gọi Collector
                List<SocialPostEntity> posts = collector.collect(
                    disasterName,
                    keyword,
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59)
                );
                
                // Gọi Preprocess (Làm sạch & Gán nhãn)
                if (preprocess != null) {
                    posts = preprocess.clean(posts);
                }
                
                allPosts.addAll(posts);
            }
        }
        return allPosts;
    }

    // (Giữ hàm cũ này nếu cần tương thích ngược, không ảnh hưởng)
    public <T> AnalysisResponse<T> processSingleAnalysis(AnalysisRequest request, AnalyzerFactory.AnalyzerType defaultType) {
        List<SocialPostEntity> allPosts = collectAndPreprocess(request);
        @SuppressWarnings("unchecked")
        Analyzer<T> analyzer = (Analyzer<T>) analyzerFactory.getAnalyzer(defaultType);
        if (analyzer == null) return AnalysisResponse.error("Analyzer not found");
        return analyzer.analyze(allPosts);
    }
}