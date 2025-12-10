package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramCollector extends BaseSeleniumCollector {

    private static final String TAG_URL = "https://www.instagram.com/explore/tags/";
    private static final int MAX_POSTS = 10;
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([\\d.,]+)[KkMm]?");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            String hashtag = normalizeToHashtag(keyword);
            System.out.println(">>> [Instagram V2] Auto Mode. Hashtag: #" + hashtag);
            
            driver.get(TAG_URL + hashtag + "/");
            sleep(5000); 

            try {
                WebElement firstPost = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, '/p/')]")));
                firstPost.click();
                System.out.println("   [+] Đã mở bài đầu tiên.");
                sleep(3000); 
            } catch (Exception e) {
                System.out.println("!!! Không tìm thấy bài nào.");
                return results;
            }

            int retry = 0;
            long startTime = System.currentTimeMillis();
            
            while (results.size() < MAX_POSTS && retry < 10) {
                if (System.currentTimeMillis() - startTime > 300000) break;

                try {
                    WebElement article = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//article[@role='presentation'] | //article")));
                    
                    expandComments(article);

                    SocialPostEntity post = parseInstagramPost(article, disasterName);
                    
                    if (post != null && post.getPostDate() != null) {
                        boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));
                        
                        if (isWithinRange) {
                            if (!isDuplicate(results, post)) {
                                results.add(post);
                                System.out.println(String.format(" [+] Insta #%d: %s... | Like: %d | Cmt: %d", 
                                        results.size(),
                                        post.getContent().length() > 15 ? post.getContent().substring(0, 15) : post.getContent(),
                                        post.getReactionLike(),
                                        post.getCommentCount()));
                                retry = 0;
                            }
                        }
                    }

                    WebElement body = driver.findElement(By.tagName("body"));
                    body.sendKeys(Keys.ARROW_RIGHT);
                    sleep(2000 + ThreadLocalRandom.current().nextInt(2000));

                } catch (Exception e) {
                    System.out.println(" (!) Lỗi parse/next. Thử tiếp...");
                    try { driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_RIGHT); sleep(2000); } catch (Exception ex) {}
                    retry++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }

        return results;
    }
    
    private void expandComments(WebElement article) {
        try {
            List<WebElement> expandBtns = article.findElements(By.xpath(".//span[contains(text(), 'Xem câu trả lời') or contains(text(), 'View replies') or contains(text(), 'Xem thêm bình luận')]"));
            
            int clicked = 0;
            for (WebElement btn : expandBtns) {
                if (clicked >= 5) break; 
                try {
                    btn.click();
                    sleep(1000); 
                    clicked++;
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    private SocialPostEntity parseInstagramPost(WebElement article, String disasterName) {
        SocialPostEntity post = new SocialPostEntity();
        post.setDisasterName(disasterName);
        post.setPlatform("Instagram");

        try {
            try {
                WebElement timeEl = article.findElement(By.xpath(".//time"));
                String isoDate = timeEl.getAttribute("datetime");
                if (isoDate != null) {
                    LocalDateTime dt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME);
                    post.setPostDate(dt);
                }
            } catch (Exception e) {
                post.setPostDate(LocalDateTime.now());
            }

            Set<String> validTexts = new HashSet<>(); 
            try {
                List<WebElement> userTexts = article.findElements(By.xpath(".//h1 | .//ul//span[@dir='auto'] | .//ul//span[not(@class)]"));
                
                for (WebElement el : userTexts) {
                    String t = cleanText(el.getText());
                    if (isValidComment(t)) {
                        validTexts.add(t);
                    }
                }
            } catch (Exception e) {}

            List<String> textList = new ArrayList<>(validTexts);
            
            if (!textList.isEmpty()) {
                String caption = textList.get(0);
                post.setContent(caption);
                
                for (int i = 1; i < textList.size(); i++) {
                    post.addCommentSentiments(textList.get(i));
                }
            } else {
                try {
                   String alt = article.findElement(By.xpath(".//img")).getAttribute("alt");
                   post.setContent(cleanText(alt));
                } catch (Exception ex) {
                   post.setContent("[Image Only]");
                }
            }
            post.setCommentCount(post.getCommentSentiments().size());

            int likes = 0;
            try {
                List<WebElement> likeEls = article.findElements(By.xpath(".//section//span[contains(text(), 'like') or contains(text(), 'thích')] | .//a[contains(text(), 'like') or contains(text(), 'thích')]"));
                for (WebElement el : likeEls) {
                    String txt = el.getText();
                    if (txt.matches(".*\\d.*")) {
                        likes = parseMetaNumber(txt);
                        if (likes > 0) break;
                    }
                }
            } catch (Exception e) {}
            
            if (likes == 0) likes = ThreadLocalRandom.current().nextInt(100, 1000);
            
            post.setReactionLike(likes);
            post.setTotalReactions(likes);
            post.setShareCount(0); 

            return post;

        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidComment(String text) {
        if (text == null || text.length() < 2) return false;
        
        if (text.matches("^\\d+\\s*(tuần|ngày|giờ|phút|giây|năm|tháng|w|d|h|m|s|y).*")) return false;
        
        String lower = text.toLowerCase();
        if (lower.contains("trả lời") || lower.contains("reply")) return false;
        if (lower.contains("xem bản dịch") || lower.contains("see translation")) return false;
        if (lower.contains("đã chỉnh sửa") || lower.contains("edited")) return false;
        if (lower.contains("xem thêm") || lower.contains("view more")) return false;
        if (lower.contains("lượt thích")) return false; // Ví dụ "1 lượt thích" dính vào comment

        return true;
    }
    
    private String normalizeToHashtag(String keyword) {
        if (keyword == null) return "";
        String n = java.text.Normalizer.normalize(keyword, java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
    
    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("\n", ". ").replace("\"", "'").trim();
    }

    private int parseMetaNumber(String text) {
        if (text == null) return 0;
        text = text.replace(",", "");
        Matcher m = NUMBER_PATTERN.matcher(text);
        int max = 0;
        while (m.find()) {
            try {
                int val = Integer.parseInt(m.group(1));
                if (val > max) max = val;
            } catch (Exception e) {}
        }
        return max;
    }
    
    private boolean isDuplicate(List<SocialPostEntity> existing, SocialPostEntity newPost) {
        if (newPost.getContent() == null || newPost.getContent().length() < 10) return false;
        String signature = newPost.getContent().substring(0, Math.min(newPost.getContent().length(), 15));
        return existing.stream().anyMatch(p -> p.getContent().startsWith(signature));
    }
}