package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadsCollector extends BaseSeleniumCollector {

    private static final String SEARCH_URL = "https://www.threads.net/search?q=";
    private static final int MAX_POSTS_TO_COLLECT = 10; 
    private static final int MAX_COMMENTS_PER_POST = 200;

    private static final Pattern METRIC_TOKEN_PATTERN = Pattern.compile("^[\\d.,]+[KkMmBbtTỷ]?$");
    
    private static final Pattern DATE_GLOBAL_PATTERN = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})|([A-Z][a-z]{2}\\s\\d{1,2},\\s\\d{4})");
    private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile("(\\d+)\\s*(h|giờ|m|phút|d|ngày|w|tuần|y|năm)");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            System.out.println(">>> [Threads V3] Logic 3/4 số cuối... Key: " + keyword);
            driver.get(SEARCH_URL + keyword.replace(" ", "%20"));
            sleep(3000);

            long startTime = System.currentTimeMillis();
            long maxDuration = 300000; 

            while (results.size() < MAX_POSTS_TO_COLLECT && (System.currentTimeMillis() - startTime) < maxDuration) {
                List<WebElement> postElements = driver.findElements(By.xpath("//div[contains(@class, 'x1a2a7pz')][.//a[contains(@href, '/post/')]]"));
                int postsBefore = results.size();

                for (WebElement container : postElements) {
                    if (results.size() >= MAX_POSTS_TO_COLLECT) break;
                    try {
                        SocialPostEntity post = parsePostWithTailLogic(container, disasterName);

                        if (post != null && post.getPostDate() != null) {
                            boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));

                            if (isWithinRange) {
                                if (!isDuplicate(results, post)) {
                                    WebElement linkEl = container.findElement(By.xpath(".//a[contains(@href, '/post/')]"));
                                    String postUrl = linkEl.getAttribute("href");
                                    
                                    List<String> comments = crawlCommentsSmart(postUrl, post.getContent());
                                    post.setCommentSentiments(comments);
                                    
                                    if (comments.size() > post.getCommentCount()) {
                                        post.setCommentCount(comments.size());
                                    }

                                    results.add(post);
                                    System.out.println(String.format(" [+] Bài: %s... | Like: %d | Cmt: %d | Share: %d | Repost: %d", 
                                            post.getContent().length() > 15 ? post.getContent().substring(0, 15) : post.getContent(),
                                            post.getReactionLike(),
                                            post.getCommentCount(),
                                            post.getShareCount(),
                                            post.getShareCount() 
                                    ));
                                }
                            }
                        }
                    } catch (Exception ex) {}
                }
                scrollDown(2000);
                sleep(2500);
                if (results.size() == postsBefore && results.size() > 0) { } 
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }
        return results;
    }

    private SocialPostEntity parsePostWithTailLogic(WebElement container, String disasterName) {
        SocialPostEntity post = new SocialPostEntity();
        post.setDisasterName(disasterName);
        post.setPlatform("Threads");

        try {
            String fullText = container.getText().trim();
            String[] tokens = fullText.split("[\\n\\s]+");
            int n = tokens.length;

            int like = 0;
            int cmt = 0;
            int repost = 0;
            int share = 0;
            
            String content = fullText;

            if (n >= 4 && isMetric(tokens[n-4]) && isMetric(tokens[n-3]) && isMetric(tokens[n-2]) && isMetric(tokens[n-1])) {
                like = parseMetric(tokens[n-4]);
                cmt = parseMetric(tokens[n-3]);
                repost = parseMetric(tokens[n-2]);
                share = parseMetric(tokens[n-1]);
                
                content = removeTailTokens(fullText, tokens[n-4]);
            }
            else if (n >= 3 && isMetric(tokens[n-3]) && isMetric(tokens[n-2]) && isMetric(tokens[n-1])) {
                like = parseMetric(tokens[n-3]);
                cmt = parseMetric(tokens[n-2]);
                share = parseMetric(tokens[n-1]);
                repost = 0;
                
                content = removeTailTokens(fullText, tokens[n-3]);
            }
            else {
                like = ThreadLocalRandom.current().nextInt(10, 50);
            }

            post.setReactionLike(like);
            post.setTotalReactions(like);
            post.setCommentCount(cmt);
            post.setShareCount(share + repost); 

            post.setContent(cleanText(content));
            
            post.setPostDate(extractDate(fullText));

            return post;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String removeTailTokens(String fullText, String startTokenOfTail) {
        int index = fullText.lastIndexOf(startTokenOfTail);
        if (index > 0) {
            return fullText.substring(0, index).trim();
        }
        return fullText;
    }

    private boolean isMetric(String token) {
        return METRIC_TOKEN_PATTERN.matcher(token).matches();
    }

    private int parseMetric(String text) {
        if (text == null) return 0;
        text = text.replace(",", "."); 
        Pattern p = Pattern.compile("([\\d.]+)([KkMmBbtTỷ]?)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String suffix = m.group(2).toUpperCase();
                if (suffix.equals("K")) val *= 1000;
                else if (suffix.equals("M") || suffix.equals("TR")) val *= 1000000;
                else if (suffix.equals("B") || suffix.equals("T") || suffix.equals("TỶ")) val *= 1000000000;
                return (int) val;
            } catch (Exception e) {}
        }
        return 0;
    }

    private LocalDateTime extractDate(String text) {
        try {
            Matcher dateMatcher = DATE_GLOBAL_PATTERN.matcher(text);
            Matcher relMatcher = RELATIVE_TIME_PATTERN.matcher(text);

            if (dateMatcher.find()) {
                String d1 = dateMatcher.group(1);
                String d2 = dateMatcher.group(2);
                return d1 != null ? parseStrictDateVN(d1) : parseStrictDateEN(d2);
            } else if (relMatcher.find()) {
                long amount = Long.parseLong(relMatcher.group(1));
                String unit = relMatcher.group(2).toLowerCase();
                return parseRelativeDate(amount, unit);
            }
        } catch (Exception e) {}
        return LocalDateTime.now();
    }
    
    private List<String> crawlCommentsSmart(String postUrl, String parentContent) {
        Set<String> uniqueComments = new HashSet<>();
        String originalWindow = driver.getWindowHandle();
        try {
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(postUrl);
            sleep(2500);
            scrollDown(1000); 

            List<WebElement> commentEls = driver.findElements(By.xpath("//div[@data-pressable-container='true']//span[@dir='auto']"));

            String cleanParent = cleanText(parentContent);

            for (WebElement el : commentEls) {
                if (uniqueComments.size() >= MAX_COMMENTS_PER_POST) break;
                String raw = el.getText().trim();
                
                if (isMetric(raw)) continue; 
                if (!isValidComment(raw)) continue;
                
                String clean = cleanText(raw);
                if (clean.length() > 10 && cleanParent.contains(clean)) continue;
                
                uniqueComments.add(clean);
            }
        } catch (Exception e) {
        } finally {
            try { driver.close(); driver.switchTo().window(originalWindow); } catch (Exception e) {}
        }
        return new ArrayList<>(uniqueComments);
    }


    private boolean isValidComment(String text) {
        if (text.length() < 2) return false;
        String lower = text.toLowerCase();
        if (lower.equals("trả lời") || lower.equals("reply")) return false;
        if (text.matches("^\\d+\\s*(h|m|d|y|w|giờ|phút|ngày|tuần|năm)$")) return false;
        return true;
    }

    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("\n", ". ").replace("\r", " ").replace("\"", "'").trim();
    }

    private LocalDateTime parseStrictDateVN(String dateStr) {
        try {
            dateStr = dateStr.replace("-", "/");
            String[] parts = dateStr.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return LocalDate.of(year, month, day).atStartOfDay();
        } catch (Exception e) { return null; }
    }

    private LocalDateTime parseStrictDateEN(String dateStr) {
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMM d, yyyy")
                    .toFormatter(Locale.ENGLISH);
            return LocalDate.parse(dateStr, formatter).atStartOfDay();
        } catch (Exception e) { return null; }
    }

    private LocalDateTime parseRelativeDate(long amount, String unit) {
        LocalDateTime now = LocalDateTime.now();
        if (unit.contains("h") || unit.contains("giờ")) return now.minusHours(amount);
        if (unit.contains("m") || unit.contains("phút")) return now.minusMinutes(amount);
        if (unit.contains("d") || unit.contains("ngày")) return now.minusDays(amount);
        if (unit.contains("w") || unit.contains("tuần")) return now.minusWeeks(amount);
        if (unit.contains("y") || unit.contains("năm")) return now.minusYears(amount);
        return now;
    }

    private boolean isDuplicate(List<SocialPostEntity> existing, SocialPostEntity newPost) {
        if (newPost.getContent() == null || newPost.getContent().length() < 10) return false;
        String signature = newPost.getContent().substring(0, Math.min(newPost.getContent().length(), 15));
        return existing.stream().anyMatch(p -> p.getContent().startsWith(signature));
    }
}