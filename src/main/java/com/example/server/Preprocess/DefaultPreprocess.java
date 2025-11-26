package com.example.server.Preprocess;

import java.util.List;

import com.example.server.model.SocialPostEntity;

public class DefaultPreprocess implements Preprocess {

    @Override
    public List<SocialPostEntity> clean(List<SocialPostEntity> posts) {
        for (SocialPostEntity post : posts) {
            if (post.getContent() != null) {
                post.setContent(cleanText(post.getContent()));
            }
            // có thể lọc comment trùng, remove spam, normalize unicode
            if (post.getComments() != null) {
                post.removeDuplicateComments();
                List<String> cleanedComments = post.getComments().stream()
                                                   .map(this::cleanText)
                                                   .toList();
                post.setComments(cleanedComments);
            }
        }
        return posts;
    }

    protected String cleanText(String text) {
        // ví dụ: loại bỏ emoji, ký tự đặc biệt, trim khoảng trắng
        return text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "").trim();
    }
}