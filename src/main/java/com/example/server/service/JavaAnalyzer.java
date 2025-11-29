package com.example.server.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import com.example.server.model.SocialPostEntity;
public class JavaAnalyzer {
	//Xác định mức độ hài lòng của quần chúng qua ratio của positve và negative sentiment.
	public static Map<String, Map<String, Double>> publicSatisfaction(List<SocialPostEntity> posts) {
	    // 1: Gom nhóm theo reliefItem.
	    Map<String, Map<String, Integer>> counts = new HashMap<>();
	    for (SocialPostEntity p : posts) {
	    	String relief = p.getReliefItem();
	    	String senti = p.getSentiment();
	    	if (relief == null || senti == null) continue;
	        counts.putIfAbsent(relief, new HashMap<>());
	        Map<String, Integer> c = counts.get(relief);
	        c.put(p.sentiment, c.getOrDefault(senti, 0) + 1);
	    }
	    // 2: Đánh giá theo ratio. 
	    Map<String, Map<String, Double>> result = new HashMap<>();
	    for (String relief : counts.keySet()) {
	        Map<String, Integer> c = counts.get(relief);
	        int positive = c.getOrDefault("positive", 0);
	        int neutral = c.getOrDefault("neutral", 0);
	        int negative = c.getOrDefault("negative", 0);
	        int total = positive + neutral + negative;
	        Map<String, Double> percentages = new HashMap<>();
	        if (total == 0) {
	            percentages.put("positive", 0.0);
	            percentages.put("negative", 0.0);
	            percentages.put("ratio", 0.0);
	        } else {
	            percentages.put("positive", positive / total);
	            percentages.put("negative", negative / total);
	            percentages.put("ratio", positive / negative);
	        }

	        result.put(relief, percentages);
	    }
	    return result;
	}
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
	public static class SentimentRatio {
	    int positive = 0;
	    int negative = 0;
	    int neutral = 0;
	    double positiveRatio;
	    double negativeRatio;
	    public void compute() {
	        int total = positive + negative + neutral;
	        if (total == 0) return;
	        positiveRatio = (double) positive / total;
	        negativeRatio = (double) negative / total;
	    }
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
	// Object sự biến thiên của sentiment
	public static class SentimentChange {
	    double positiveDelta;
	    double negativeDelta;
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
	    double prevPos = byDate.get(prev).positiveRatio;
	    TrendDirection currentDir = TrendDirection.STABLE;
	    for (int i = 1; i < days.size(); i++) {
	        LocalDate today = days.get(i);
	        double todayPos = byDate.get(today).positiveRatio;
	        TrendDirection newDir;
	        if (todayPos > prevPos) newDir = TrendDirection.RISE;
	        else if (todayPos < prevPos) newDir = TrendDirection.FALL;
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
	public enum TrendDirection { RISE, FALL, STABLE }
	// Object xu hướng.
	public static class SentimentTrend {
	    public LocalDate start;
	    public LocalDate end;
	    public TrendDirection direction;
	    public SentimentTrend(LocalDate s, LocalDate e, TrendDirection d) {
	        this.start = s;
	        this.end = e;
	        this.direction = d;
	    }
	}
}
