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

    // ------------------------
    // SINGLE ANALYSIS (USING analysisType OR analyzers)
    // ------------------------
    public <T> AnalysisResponse<T> processSingleAnalysis(AnalysisRequest request, AnalyzerFactory.AnalyzerType defaultType) {
        List<SocialPostEntity> allPosts = collectAndPreprocess(request);

        // Nếu client dùng analysisType
        AnalyzerFactory.AnalyzerType type = defaultType;
        if (request.getAnalysisType() != null && !request.getAnalysisType().isEmpty()) {
            try {
                type = AnalyzerFactory.AnalyzerType.valueOf(request.getAnalysisType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return AnalysisResponse.error("Invalid analysisType: " + request.getAnalysisType());
            }
        }

        Analyzer<T> analyzer = analyzerFactory.getAnalyzer(type);
        if (analyzer == null) {
            return AnalysisResponse.error("Analyzer not found for type: " + type);
        }

        try {
            return analyzer.analyze(allPosts);
        } catch (Exception e) {
            return AnalysisResponse.error("Analysis error: " + e.getMessage());
        }
    }

    // ------------------------
    // FULL ANALYSIS (SELECTED TYPES)
    // ------------------------
    public AnalysisResponse<Map<String, Object>> processAllAnalysis(AnalysisRequest request) {
        List<SocialPostEntity> allPosts = collectAndPreprocess(request);
        Map<String, Object> resultMap = new HashMap<>();

        // Lấy danh sách analyzer client muốn chạy
        List<String> requestedAnalyzers = new ArrayList<>();
        if (request.getAnalysisType() != null && !request.getAnalysisType().isEmpty()) {
            requestedAnalyzers.add(request.getAnalysisType());
        } else if (request.getAnalysisType() != null && !request.getAnalysisType().isEmpty()) {
            requestedAnalyzers.add(request.getAnalysisType());
        } else {
            // Nếu không có gì, mặc định chạy tất cả
            for (AnalyzerFactory.AnalyzerType type : AnalyzerFactory.AnalyzerType.values()) {
                requestedAnalyzers.add(type.name());
            }
        }

        for (String analyzerName : requestedAnalyzers) {
            AnalyzerFactory.AnalyzerType type;
            try {
                type = AnalyzerFactory.AnalyzerType.valueOf(analyzerName.toUpperCase());
            } catch (IllegalArgumentException e) {
                resultMap.put(analyzerName, "Invalid analyzer type");
                continue;
            }

            Analyzer<Object> analyzer = analyzerFactory.getAnalyzer(type);
            if (analyzer != null) {
                AnalysisResponse<Object> resp = analyzer.analyze(allPosts);
                if (resp.isSuccess()) {
                    resultMap.put(type.name(), resp.getData());
                } else {
                    resultMap.put(type.name(), "Error: " + resp.getErrorMessage());
                }
            } else {
                resultMap.put(type.name(), "Analyzer not found");
            }
        }

        return AnalysisResponse.success(resultMap);
    }

    // ------------------------
    // COLLECT AND PREPROCESS
    // ------------------------
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

                List<SocialPostEntity> posts = collector.collect(
                    disasterName,
                    keyword,
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59)
                );
                if (preprocess != null) {
                    posts = preprocess.clean(posts);
                }
                allPosts.addAll(posts);
            }
        }

        return allPosts;
    }
}
