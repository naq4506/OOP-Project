package com.example.server.Preprocess;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.server.dictionary.DictionaryLoader;
import com.example.server.model.SocialPostEntity;

@Component
public class EnhancedPreprocess extends DefaultPreprocess {

    private final DictionaryLoader loader = new DictionaryLoader();
    private final Map<String, Integer> sentimentDict = loader.loadSentiment();
    private final Map<String, String> damageTypeDict = loader.loadDamageType();
    private final Map<String, String> reliefItemDict = loader.loadReliefItem();

    @Override
    public List<SocialPostEntity> clean(List<SocialPostEntity> posts) {
        // Step 1: basic cleaning
        posts = super.clean(posts);

        // Step 2: advanced cleaning
        for (SocialPostEntity post : posts) {
            if (post.getContent() != null) {
                // --- Giữ content gốc để map ReliefItem ---
                String originalContent = post.getContent();
                post.setReliefItem(mapReliefItem(originalContent));
                post.setDamageType(mapDamageType(originalContent));

                // --- Normalize content chỉ để sentiment ---
                String normalizedContent = normalizeHashtags(originalContent);
                normalizedContent = normalizeDates(normalizedContent);
                post.setContent(normalizedContent);
                post.setSentiment(analyzeSentiment(normalizedContent));
            }

            // advanced comment cleaning
            if (post.getComments() != null) {
                List<String> cleanedComments = post.getComments().stream()
                        .map(this::cleanText)
                        .map(this::normalizeHashtags)
                        .map(this::normalizeDates)
                        .toList();
                post.setComments(cleanedComments);

                List<String> commentSentiments = cleanedComments.stream()
                        .map(this::analyzeSentiment)
                        .toList();
                post.setCommentSentiments(commentSentiments);
            }
        }

        return posts;
    }

    // --- Advanced helpers ---
    private String normalizeHashtags(String text) {
        if (text == null) return "";
        // chỉ loại ký tự đặc biệt, lowercase
        return text.replaceAll("[^\\p{L}\\p{N}#\\p{P}\\p{Z}]", "").trim().toLowerCase();
    }

    private String normalizeDates(String text) {
        if (text == null) return "";
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
                // ignore
            }
        }
        return text;
    }

    private String analyzeSentiment(String text) {
        if (text == null) return "neutral";
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
        if (text == null) return "Other";
        String lowerText = text.toLowerCase();
        for (String key : damageTypeDict.keySet()) {
            if (lowerText.contains(key.toLowerCase())) return damageTypeDict.get(key);
        }
        return "Other";
    }

    private String mapReliefItem(String text) {
        if (text == null) return "Other";
        String lowerText = text.toLowerCase();
        for (String key : reliefItemDict.keySet()) {
            if (lowerText.contains(key.toLowerCase())) return reliefItemDict.get(key);
        }
        return "Other";
    }
}
