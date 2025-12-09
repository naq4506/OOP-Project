package com.example.server.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SocialPost {
    private String content;
    private int totalReactions;
    private int commentCount;
    private int shareCount;
    private List<String> comments = new ArrayList<>();

    public SocialPost() {}

    public void addComment(String cmt) {
        this.comments.add(cmt);
    }

    public void removeDuplicateComments() {
        if (this.comments == null || this.comments.isEmpty()) return;
        Set<String> set = new LinkedHashSet<>(this.comments);
        this.comments.clear();
        this.comments.addAll(set);
    }

    public String getContent() 
    { 
    	return content; 
    }
    public void setContent(String content) 
    { 
    	this.content = content; 
    }

    public int getTotalReactions() 
    { 
    	return totalReactions; 
    }
    public void setTotalReactions(int totalReactions) 
    { 
    	this.totalReactions = totalReactions; 
    }

    public int getCommentCount() 
    { 
    	return commentCount; 
    }
    public void setCommentCount(int commentCount)
    { 
    	this.commentCount = commentCount; 
    }

    public int getShareCount() 
    { 
    	return shareCount; 
    }
    public void setShareCount(int shareCount) 
    { 
    	this.shareCount = shareCount; 
    }

    public List<String> getComments() 
    { 
    	return comments; 
    }

    public String getCommentsAsCsvString() {
        if (comments.isEmpty()) return "";
        return comments.stream().collect(Collectors.joining(" ||| "));
    }

    @Override
    public String toString() {
        return String.format("Post[Reacts=%d, Comments=%d, Shares=%d]", totalReactions, comments.size(), shareCount);
    }
}