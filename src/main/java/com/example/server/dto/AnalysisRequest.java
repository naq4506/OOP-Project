package com.example.server.dto;

import java.util.List;

public class AnalysisRequest {

    private String disasterName;
    private String startDate;
    private String endDate;
    private String keyword;
    private String analysisType;
    private List<String> platforms;

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

    public String getAnalysisType() { 
        return analysisType; 
    }
    public void setAnalysisType(String analysisType) { 
        this.analysisType = analysisType; 
    }

    public List<String> getPlatforms() {
        return platforms;
    }
}
