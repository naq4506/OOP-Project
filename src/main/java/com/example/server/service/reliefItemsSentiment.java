package com.example.server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class reliefItemsSentiment implements sentimentAnalysis {
	//Xác định mức độ hài lòng của quần chúng qua ratio của positve và negative sentiment.
	public static Map<String, SentimentRatio> publicSatisfaction(List<SocialPostEntity> posts) {
	    // 1: Gom nhóm theo reliefItem.
	    Map<String, SentimentRatio> result = new HashMap<>();
	    for (SocialPostEntity p : posts) {
	    	String relief = p.getReliefItem();
	    	String senti = p.getSentiment();
	    	if (relief == null || senti == null) continue;
	        result.putIfAbsent(relief, new SentimentRatio());
	        SentimentRatio sr = result.get(relief);
	        switch (senti.toLowerCase()) {
	            case "positive": sr.positive++; break;
	            case "negative": sr.negative++; break;
	            case "neutral":  sr.neutral++;  break;
	        }
	    }
	    // 2: Đánh giá theo ratio. 
	    for (SentimentRatio sr : result.values()) {
	        sr.compute();
	    }
	    return result;
	}

}
