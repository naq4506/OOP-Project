package com.example.server.dto;

import java.util.List;

public class AnalysisRequest {

    private String disasterName;
    private String startDate;
    private String endDate;
    private String keyword;
    private String analysisType;
    
    // Thêm 2 biến này để Controller không bị lỗi
    private List<String> platforms;
    private List<String> analyzers;

    public String getDisasterName() { return disasterName; }
    public void setDisasterName(String disasterName) { this.disasterName = disasterName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }

    // --- Getter/Setter cho Platforms và Analyzers (QUAN TRỌNG) ---
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

    public List<String> getAnalyzers() { return analyzers; }
    public void setAnalyzers(List<String> analyzers) { this.analyzers = analyzers; }
}