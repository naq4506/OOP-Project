package com.example.server.service.analyzer;

import java.util.List;
import com.example.server.model.SocialPostEntity;
import com.example.server.dto.AnalysisResponse;

public interface Analyzer<T> {
    AnalysisResponse<T> analyze(List<SocialPostEntity> posts);
}
