package com.example.server.service.collector;

import com.example.server.model.SocialPostEntity;
import java.time.LocalDateTime;
import java.util.List;

public interface Collector {
	List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate);
}