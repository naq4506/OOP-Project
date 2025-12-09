package com.example.server.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SocialPostEntity {
    private Long id;
    private String content;
    private LocalDateTime postDate;
    private String platform; 
    private String disasterName;
    private List<String> keywords;
    private String sentiment;
    private String damageType;
    private String reliefItem;
    private int totalReactions;
    private int shareCount;
    private int commentCount;
    private List<String> commentSentiments;
    
    private int reactionLike = 0;
    private int reactionLove = 0;
    private int reactionHaha = 0;
    private int reactionWow = 0;
    private int reactionSad = 0;
    private int reactionAngry = 0;
    private int reactionCare = 0; 
   
    private List<String> comments = new ArrayList<>(); 

    public SocialPostEntity() { }

    
    public void addComment(String cmt) {
        if (this.comments == null) this.comments = new ArrayList<>();
        this.comments.add(cmt);
    }

    public void removeDuplicateComments() {
        if (this.comments == null || this.comments.isEmpty()) return;
        Set<String> set = new LinkedHashSet<>(this.comments);
        this.comments.clear();
        this.comments.addAll(set);
    }

    public String getCommentsAsCsvString() {
        if (comments == null || comments.isEmpty()) return "";
        return comments.stream().collect(Collectors.joining(" ||| "));
    }

    public void setPlatform(String platform) 
    {
        this.platform = platform;
    }
    
    public String getPlatform() {
        return this.platform;
    }


    public int getTotalReactions() 
    { 
    	return totalReactions; 
    }
    public void setTotalReactions(int totalReactions) 
    { 
    	this.totalReactions = totalReactions; 
    }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) 
    { 
    	this.shareCount = shareCount; 
    }

    public int getCommentCount() 
    { 
    	return commentCount; 
    }
    public void setCommentCount(int commentCount) 
    { 
    	this.commentCount = commentCount; 
    }

    public List<String> getComments() 
    { 
    	return comments; 
    }
    public void setComments(List<String> comments) 
    { 
    	this.comments = comments; 
    }

    public Long getId() 
    { 
    	return id; 
    }
    public void setId(Long id) 
    { 
    	this.id = id; 
    }
    public String getContent() 
    { 
    	return content; 
    }
    public void setContent(String content) 
    { 
    	this.content = content; 
    }
    public LocalDateTime getPostDate()
    { 
    	return postDate; 
    }
    public void setPostDate(LocalDateTime postDate) 
    { 
    	this.postDate = postDate; 
    }
    public String getDisasterName() 
    { 
    	return disasterName; 
    }
    public void setDisasterName(String disasterName) 
    { 
    	this.disasterName = disasterName; 
    }
    public List<String> getKeywords() 
    { 
    	return keywords; 
    }
    public void setKeywords(List<String> keywords) 
    { 
    	this.keywords = keywords; 
    }
    public String getSentiment() 
    { 
    	return sentiment; 
    }
    public void setSentiment(String sentiment) 
    { 
    	this.sentiment = sentiment; 
    }
    public String getDamageType() 
    { 
    	return damageType; 
    }
    public void setDamageType(String damageType) 
    { 
    	this.damageType = damageType; 
    }
    public String getReliefItem() 
    { 
    	return reliefItem; 
    }
    public void setReliefItem(String reliefItem) 
    { 
    	this.reliefItem = reliefItem; 
    }

    public int getReactionLike() { return reactionLike; }
    public void setReactionLike(int reactionLike) { this.reactionLike = reactionLike; }

    public int getReactionLove() { return reactionLove; }
    public void setReactionLove(int reactionLove) { this.reactionLove = reactionLove; }

    public int getReactionHaha() { return reactionHaha; }
    public void setReactionHaha(int reactionHaha) { this.reactionHaha = reactionHaha; }

    public int getReactionWow() { return reactionWow; }
    public void setReactionWow(int reactionWow) { this.reactionWow = reactionWow; }

    public int getReactionSad() { return reactionSad; }
    public void setReactionSad(int reactionSad) { this.reactionSad = reactionSad; }

    public int getReactionAngry() { return reactionAngry; }
    public void setReactionAngry(int reactionAngry) { this.reactionAngry = reactionAngry; }
    
    public int getReactionCare() { return reactionCare; }
    public void setReactionCare(int reactionCare) { this.reactionCare = reactionCare; }

    public List<String> getCommentSentiments() {
        return commentSentiments;
    }

    public void setCommentSentiments(List<String> sentiments) {
        this.commentSentiments = sentiments;
    }

    @Override
    public String toString() {
        return "SocialPostEntity{" +
                "content='" + (content != null && content.length() > 20 ? content.substring(0, 20) + "..." : content) + '\'' +
                ", date=" + postDate +
                ", likes=" + reactionLike +
                ", angry=" + reactionAngry +
                ", shares=" + shareCount +
                ", cmts=" + (comments != null ? comments.size() : 0) +
                '}';
    }
}