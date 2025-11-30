package com.example.client.dto;

import java.util.List;
public class ClientRequest {
        private String disasterName;
    private String startDate;
    private String endDate;
    private String keyword;

    // NEW: user can choose multiple platforms
    private List<String> platforms;

    // NEW: user can choose multiple analyzers
    private List<String> analyzers;

    // (Optional) If you still want single analyze mode
    private String analysisType;

    public String getDisasterName() {
        return disasterName;
    }
    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getPlatforms() {
        return platforms;
    }
    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public List<String> getAnalyzers() {
        return analyzers;
    }
    public void setAnalyzers(List<String> analyzers) {
        this.analyzers = analyzers;
    }

    public String getAnalysisType() {
        return analysisType;
    }
    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }
}
