package com.example.server.model;

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
    private String platform; //source của ông tôi đổi thành platform nhé
    private String disasterName;
    private List<String> keywords;
    private String sentiment;
    private String damageType;
    private String reliefItem;
    // trường mới cho cào
    private int totalReactions;
    private int shareCount;
    private int commentCount;
    private List<String> comments = new ArrayList<>(); 

    public SocialPostEntity() { }

    
    // thêm comment vào list
    public void addComment(String cmt) {
        if (this.comments == null) this.comments = new ArrayList<>();
        this.comments.add(cmt);
    }

    // xóa comment trùng
    public void removeDuplicateComments() {
        if (this.comments == null || this.comments.isEmpty()) return;
        Set<String> set = new LinkedHashSet<>(this.comments);
        this.comments.clear();
        this.comments.addAll(set);
    }

    // chuyển list comment thành chuỗi để lưu CSV
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

    @Override
    public String toString() {
        return "SocialPostEntity{" +
                "content='" + (content != null && content.length() > 20 ? content.substring(0, 20) + "..." : content) + '\'' +
                ", reacts=" + totalReactions +
                ", shares=" + shareCount +
                ", cmts=" + (comments != null ? comments.size() : 0) +
                '}';
    }
}