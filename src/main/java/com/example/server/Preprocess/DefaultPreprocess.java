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
        if (text == null) return "";
        return text.trim();  
    }
}
