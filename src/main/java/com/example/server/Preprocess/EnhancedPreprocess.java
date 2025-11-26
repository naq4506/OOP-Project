package com.example.server.Preprocess;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.server.dictionary.DictionaryLoader;
import com.example.server.model.SocialPostEntity;

public class EnhancedPreprocess extends DefaultPreprocess {

    private final DictionaryLoader loader = new DictionaryLoader();
    private final Map<String, Integer> sentimentDict = loader.loadSentiment();
    private final Map<String, String> damageTypeDict = loader.loadDamageType();
    private final Map<String, String> reliefItemDict = loader.loadReliefItem();

    @Override
    public List<SocialPostEntity> clean(List<SocialPostEntity> posts) {
        // Bước 1: clean cơ bản từ DefaultPreprocess
        posts = super.clean(posts);

        // Bước 2: clean nâng cao
        for (SocialPostEntity post : posts) {
            if (post.getContent() != null) {
                // chuẩn hóa hashtag
                post.setContent(normalizeHashtags(post.getContent()));
                // chuẩn hóa ngày tháng trong content
                post.setContent(normalizeDates(post.getContent()));
                // gán sentiment
                post.setSentiment(analyzeSentiment(post.getContent()));
                // map loại thiệt hại
                post.setDamageType(mapDamageType(post.getContent()));
                // map loại hàng cứu trợ
                post.setReliefItem(mapReliefItem(post.getContent()));
            }

            // clean comment nâng cao
            if (post.getComments() != null) {
                List<String> cleanedComments = post.getComments().stream()
                        .map(this::cleanText)
                        .map(this::normalizeHashtags)
                        .map(this::normalizeDates)
                        .toList();
                post.setComments(cleanedComments);

                // thêm sentiment cho comment (nếu cần)
                // ví dụ: post.setCommentSentiments(...);
            }
        }

        return posts;
    }

    // --- Bước nâng cao ---
    private String normalizeHashtags(String text) {
        // chuyển tất cả hashtag về lowercase, loại khoảng trắng dư
        return text.replaceAll("[^\\p{L}\\p{N}#\\p{P}\\p{Z}]", "").trim().toLowerCase();
    }

    private String normalizeDates(String text) {
        String[] formats = {"d/M/yyyy", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String fmt : formats) {
            try {
                DateTimeFormatter parser = DateTimeFormatter.ofPattern(fmt);
                Pattern p = Pattern.compile("\\d{1,4}[-/]\\d{1,2}[-/]\\d{1,4}");
                Matcher m = p.matcher(text);
                while (m.find()) {
                    String dateStr = m.group();
                    LocalDate date = LocalDate.parse(dateStr, parser);
                    text = text.replace(dateStr, date.toString());
                }
            } catch (Exception e) {
                // format không khớp → bỏ qua
            }
        }
        return text;
    }

private String analyzeSentiment(String text) {
    String[] words = text.split("\\s+");
    int score = 0;
    for (String word : words) {
        score += sentimentDict.getOrDefault(word, 0);
    }
    if (score > 0) return "positive";
    if (score < 0) return "negative";
    return "neutral";
}

private String mapDamageType(String text) {
    for (String key : damageTypeDict.keySet()) {
        if (text.contains(key)) return damageTypeDict.get(key);
    }
    return "Khác";
}

private String mapReliefItem(String text) {
    for (String key : reliefItemDict.keySet()) {
        if (text.contains(key)) return reliefItemDict.get(key);
    }
    return "Khác";
}

}
