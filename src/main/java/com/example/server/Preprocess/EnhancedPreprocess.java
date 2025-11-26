package com.example.server.Preprocess;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.server.model.SocialPostEntity;


public class EnhancedPreprocess extends DefaultPreprocess {

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
        // ví dụ dummy: có thể gọi model sentiment
        if (text.contains("tích cực") || text.contains("vui") || text.contains("hy vọng")) return "positive";
        if (text.contains("tiêu cực") || text.contains("lo lắng") || text.contains("thiệt hại")) return "negative";
        return "neutral";
    }

    private String mapDamageType(String text) {
        // ví dụ: map keywords sang loại thiệt hại
        if (text.contains("nhà") || text.contains("tòa nhà")) return "Nhà cửa/tòa nhà";
        if (text.contains("cơ sở hạ tầng")) return "Cơ sở hạ tầng";
        if (text.contains("người")) return "Người bị ảnh hưởng";
        if (text.contains("tài sản cá nhân")) return "Tài sản cá nhân";
        if (text.contains("kinh tế") || text.contains("hoạt động sản xuất")) return "Gián đoạn hoạt động kinh tế";
        return "Khác";
    }

    private String mapReliefItem(String text) {
        // ví dụ: map keywords sang loại hàng cứu trợ
        if (text.contains("thực phẩm")) return "Thực phẩm";
        if (text.contains("nhà ở")) return "Nhà ở";
        if (text.contains("giao thông")) return "Giao thông";
        if (text.contains("y tế")) return "Y tế";
        if (text.contains("tiền mặt")) return "Tiền mặt";
        return "Khác";
    }
}

