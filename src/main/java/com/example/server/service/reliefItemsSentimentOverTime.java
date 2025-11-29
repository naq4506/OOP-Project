package com.example.server.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.*;
import com.example.server.service.JavaAnalyzer.SentimentChange;
import com.example.server.service.JavaAnalyzer.SentimentRatio;
import com.example.server.service.JavaAnalyzer.SentimentTrend;
import com.example.server.service.JavaAnalyzer.TrendDirection;

public class reliefItemsSentimentOverTime implements sentimentAnalysis {
	//Gom nhóm theo reliefItem và sắp xếp theo thời gian
	public static Map<String, TreeMap<LocalDateTime, String>> sentimentTimeline(List<SocialPostEntity> posts) {
	    Map<String, TreeMap<LocalDateTime, String>> timeline = new HashMap<>();
	    for (SocialPostEntity p : posts) {
	        String category = p.getReliefItem();
	        timeline.putIfAbsent(category, new TreeMap<>());
	        timeline.get(category).put(p.getPostDate(), p.getSentiment());
	    }
	    return timeline;
	}
	public static Map<String, Map<LocalDate, SentimentRatio>> dailySentimentRatios(List<SocialPostEntity> posts) {
	    Map<String, Map<LocalDate, SentimentRatio>> result = new HashMap<>();
	    for (SocialPostEntity p : posts) {
	        String category = p.getReliefItem();
	        LocalDate date = p.getPostDate().toLocalDate();
	        String sentiment = p.getSentiment().toLowerCase();
	        result.putIfAbsent(category, new HashMap<>());
	        Map<LocalDate, SentimentRatio> byDate = result.get(category);
	        byDate.putIfAbsent(date, new SentimentRatio());
	        SentimentRatio ratio = byDate.get(date);
	        switch (sentiment) {
	            case "positive" -> ratio.positive++;
	            case "negative" -> ratio.negative++;
	            case "neutral" -> ratio.neutral++;
	        }
	    }
	    result.forEach((cat, daily) -> daily.forEach((d, ratio) -> ratio.compute()));
	    return result;
	}
	// Xem sự thay đổi về tỉ lệ sentiment mỗi ngày.
	public static SentimentChange analyzeChange(Map<String, Map<LocalDate, SentimentRatio>> ratios,String category,LocalDate dayA,LocalDate dayB) {
	    SentimentRatio a = ratios.getOrDefault(category, Map.of()).get(dayA);
	    SentimentRatio b = ratios.getOrDefault(category, Map.of()).get(dayB);
	    if (a == null || b == null) return null;
	    SentimentChange change = new SentimentChange();
	    change.positiveDelta = b.positiveRatio - a.positiveRatio;
	    change.negativeDelta = b.negativeRatio - a.negativeRatio;
	    return change;
	}
	// Theo dõi các giai đoạn tăng giảm của sentiment với reliefItem qua tỉ lệ của positive sentiments
	public static List<SentimentTrend> analyzePeriod(Map<String, Map<LocalDate, SentimentRatio>> ratios,String category,LocalDate dayA,LocalDate dayB) {
	    Map<LocalDate, SentimentRatio> byDate = ratios.getOrDefault(category, Map.of());
	    // Gom các ngày giữa A and B
	    List<LocalDate> days = byDate.keySet().stream().filter(d -> !d.isBefore(dayA) && !d.isAfter(dayB)).sorted().toList();
	    if (days.size() < 2) return List.of();
	    List<SentimentTrend> trends = new ArrayList<>();
	    LocalDate start = days.get(0);
	    LocalDate prev = start;
	    double prevRatio = byDate.get(prev).positiveNegativeRatio;
	    TrendDirection currentDir = TrendDirection.STABLE;
	    for (int i = 1; i < days.size(); i++) {
	        LocalDate today = days.get(i);
	        double todayRatio = byDate.get(today).positiveNegativeRatio;
	        TrendDirection newDir;
	        if (todayRatio > prevRatio) newDir = TrendDirection.RISE;
	        else if (todayRatio < prevRatio) newDir = TrendDirection.FALL;
	        else newDir = TrendDirection.STABLE;
	        // Xu hướng thay đổi → kết thúc xu hướng trước
	        if (currentDir != newDir && currentDir != TrendDirection.STABLE) {
	            trends.add(new SentimentTrend(start, prev, currentDir));
	            start = prev;
	        }
	        // Cập nhập hướng xu hướng
	        if (currentDir == TrendDirection.STABLE) currentDir = newDir;
	        prev = today;
	        prevPos = todayPos;
	    }
	    trends.add(new SentimentTrend(start, prev, currentDir));
	    return trends;
	}
}
