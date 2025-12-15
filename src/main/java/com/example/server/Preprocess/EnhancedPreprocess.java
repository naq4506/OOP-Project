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

        posts = super.clean(posts);

        for (SocialPostEntity post : posts) {

            if (post.getContent() != null) {

                String original = post.getContent().trim();

                String normalized = normalizeText(original);
                post.setDamageType(mapDamageType(normalized));
                post.setReliefItem(mapReliefItem(normalized));

                normalized = normalizeHashtags(normalized);
                normalized = normalizeDates(normalized);

                post.setContent(normalized);

                post.setSentiment(analyzeSentiment(normalized));
            }

            if (post.getComments() != null) {

                List<String> cleanedComments = post.getComments().stream()
                        .map(this::normalizeText)
                        .map(this::normalizeHashtags)
                        .map(this::normalizeDates)
                        .toList();

                post.setComments(cleanedComments);

                List<String> sentimentList = cleanedComments.stream()
                        .map(this::analyzeSentiment)
                        .toList();

                post.setCommentSentiments(sentimentList);
            }
        }

        return posts;
    }


    private String normalizeText(String text) {
        if (text == null) return "";

        text = text.toLowerCase();

        text = text.replaceAll("[^\\p{L}\\p{N}\\s]", " ");

        return text.replaceAll("\\s+", " ").trim();
    }

    private String normalizeHashtags(String text) {
        if (text == null) return "";
        return text.replaceAll("[^#\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String normalizeDates(String text) {
        if (text == null) return "";

        Pattern p = Pattern.compile("\\d{1,4}[-/]\\d{1,2}[-/]\\d{1,4}");
        Matcher m = p.matcher(text);

        while (m.find()) {
            String dateStr = m.group();
            LocalDate parsed = tryParse(dateStr);
            if (parsed != null) {
                text = text.replace(dateStr, parsed.toString());
            }
        }

        return text;
    }

    private LocalDate tryParse(String str) {
        String[] fmts = {"d/M/yyyy", "d-M-yyyy", "yyyy/M/d", "yyyy-M-d"};

        for (String f : fmts) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(f));
            } catch (Exception ignored) {}
        }
        return null;
    }


    private String analyzeSentiment(String text) {
        if (text == null) return "neutral";

        int score = 0;
        for (String word : text.split("\\s+")) {
            score += sentimentDict.getOrDefault(word.trim(), 0);
        }

        return score > 0 ? "positive" :
               score < 0 ? "negative" :
               "neutral";
    }


    private String mapDamageType(String normalizedText) {
        if (normalizedText == null) return "Other";

        for (String key : damageTypeDict.keySet()) {
            if (normalizedText.contains(key.toLowerCase())) {
                return damageTypeDict.get(key);
            }
        }

        return "Other";
    }


    private String mapReliefItem(String normalizedText) {
        if (normalizedText == null) return "Other";

        for (String key : reliefItemDict.keySet()) {
            if (normalizedText.contains(key.toLowerCase())) {
                return reliefItemDict.get(key);
            }
        }

        return "Other";
    }
}
