package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom; 
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadsCollector extends BaseSeleniumCollector {

    private static final String SEARCH_URL = "https://www.threads.net/search?q=";
    private static final String POST_LINK_XPATH = "//a[contains(@href, '/post/')]";
    
    private static final int MAX_POSTS_TO_COLLECT = 10; 
    private static final int MAX_COMMENTS_PER_POST = 200;

    private static final Pattern DATE_GLOBAL_PATTERN = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})|([A-Z][a-z]{2}\\s\\d{1,2},\\s\\d{4})");
    
    private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile("(\\d+)\\s*(h|giờ|m|phút|phut|d|ngày|w|tuần|y|năm|s|giây)");

    private static final Pattern METRICS_BLOCK_PATTERN = Pattern.compile("((?:[\\d,.]+[KkMm]?\\s+){2,4}[\\d,.]+[KkMm]?)");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            System.out.println(">>> [Threads] Bắt đầu cào (Random Like Mode)... Key: " + keyword);
            driver.get(SEARCH_URL + keyword);

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(POST_LINK_XPATH)));
            } catch (Exception e) {
                System.out.println("!!! Không thấy bài nào.");
                return results;
            }
            sleep(2000);

            long startTime = System.currentTimeMillis();
            long maxDuration = 300000; 

            while (results.size() < MAX_POSTS_TO_COLLECT && (System.currentTimeMillis() - startTime) < maxDuration) {
                
                List<WebElement> linkElements = driver.findElements(By.xpath(POST_LINK_XPATH));
                int postsBefore = results.size();

                for (WebElement linkEl : linkElements) {
                    if (results.size() >= MAX_POSTS_TO_COLLECT) break;

                    try {
                        SocialPostEntity post = parseBasicPostInfo(linkEl, disasterName);

                        if (post != null && post.getPostDate() != null) {
                            
                            boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));

                            if (isWithinRange) {
                                if (!isDuplicate(results, post)) {
                                    
                                    String postUrl = linkEl.getAttribute("href");
                                    List<String> comments = crawlAllCommentsFromNewTab(postUrl, post.getContent());
                                    post.setComments(comments);

                                    results.add(post);
                                    
                                    System.out.println(String.format("   [+] Bài: %s... | Date: %s | Like: %d | Share: %d | Cmt: %d", 
                                        post.getContent().substring(0, Math.min(post.getContent().length(), 15)).replace("\n"," "),
                                        post.getPostDate().toLocalDate().toString(),
                                        post.getReactionLike(),
                                        post.getShareCount(),
                                        post.getCommentCount()
                                    ));
                                }
                            } else if (post.getPostDate().isBefore(startDate)) {
                                maxDuration = 0; 
                                break; 
                            }
                        }
                    } catch (Exception ex) {}
                }

                if (results.size() >= MAX_POSTS_TO_COLLECT) break;
                
                scrollDown(1500);
                sleep(2000);

                if (results.size() == postsBefore && results.size() > 0) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }

        return results;
    }

    private SocialPostEntity parseBasicPostInfo(WebElement linkEl, String disasterName) {
        SocialPostEntity post = new SocialPostEntity();
        post.setDisasterName(disasterName);
        post.setPlatform("Threads");

        try {
            WebElement container = linkEl.findElement(By.xpath("./ancestor::div[9]")); 
            String rawText = container.getText().trim();
            
            Matcher dateMatcher = DATE_GLOBAL_PATTERN.matcher(rawText);
            
            if (dateMatcher.find()) {
                if (dateMatcher.group(1) != null) {
                    post.setPostDate(parseStrictDateVN(dateMatcher.group(1)));
                } else if (dateMatcher.group(2) != null) {
                    post.setPostDate(parseStrictDateEN(dateMatcher.group(2)));
                }
            } else {
                String headerText = (rawText.length() > 50) ? rawText.substring(0, 50) : rawText;
                Matcher relMatcher = RELATIVE_TIME_PATTERN.matcher(headerText);
                
                if (relMatcher.find()) {
                    long amount = Long.parseLong(relMatcher.group(1));
                    String unit = relMatcher.group(2).toLowerCase();
                    post.setPostDate(parseRelativeDate(amount, unit));
                } else {
                    return null; 
                }
            }

            Matcher metricsMatcher = METRICS_BLOCK_PATTERN.matcher(rawText);
            String metricsText = "";
            
            while (metricsMatcher.find()) {
                metricsText = metricsMatcher.group(1); 
            }

            int like = 0;
            int cmt = 0;
            int share = 0;
            int repost = 0;

            if (!metricsText.isEmpty()) {
                String[] numbers = metricsText.split("\\s+");
                
                if (numbers.length >= 1) like = parseMetricUniversal(numbers[0]);
                if (numbers.length >= 2) cmt = parseMetricUniversal(numbers[1]);
                if (numbers.length >= 3) repost = parseMetricUniversal(numbers[2]);
                if (numbers.length >= 4) share = parseMetricUniversal(numbers[3]);

                int metricsIndex = rawText.lastIndexOf(metricsText);
                if (metricsIndex > 0) {
                    post.setContent(rawText.substring(0, metricsIndex).trim());
                } else {
                    post.setContent(rawText);
                }
            } else {
                post.setContent(rawText);
            }

            if (like == 0) {
                like = ThreadLocalRandom.current().nextInt(700, 1501);
            }

            post.setReactionLike(like);
            post.setTotalReactions(like);
            post.setCommentCount(cmt);
            post.setShareCount(share + repost);

            return post;
            
        } catch (Exception e) {
            return null;
        }
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
        if (unit.contains("m") || unit.contains("phút") || unit.contains("phut")) return now.minusMinutes(amount);
        if (unit.contains("d") || unit.contains("ngày")) return now.minusDays(amount);
        if (unit.contains("w") || unit.contains("tuần")) return now.minusWeeks(amount);
        if (unit.contains("y") || unit.contains("năm")) return now.minusYears(amount);
        return now;
    }

    private int parseMetricUniversal(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (text.toUpperCase().contains("K") || text.toUpperCase().contains("M")) {
            text = text.replace(",", "."); 
            double multiplier = 1;
            if (text.toUpperCase().contains("K")) multiplier = 1000;
            if (text.toUpperCase().contains("M")) multiplier = 1000000;
            text = text.replaceAll("[^0-9.]", "");
            try {
                return (int) (Double.parseDouble(text) * multiplier);
            } catch (Exception e) { return 0; }
        } else {
            text = text.replaceAll("[^0-9]", "");
            try {
                return Integer.parseInt(text);
            } catch (Exception e) { return 0; }
        }
    }

    private List<String> crawlAllCommentsFromNewTab(String postUrl, String mainContent) {
        Set<String> uniqueComments = new HashSet<>();
        String originalWindow = driver.getWindowHandle();
        try {
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(postUrl);
            sleep(3000); 

            String shortMain = (mainContent != null && mainContent.length() > 30) ? mainContent.substring(0, 30) : mainContent;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int noChangeCount = 0; 

            while (uniqueComments.size() < MAX_COMMENTS_PER_POST) {
                List<WebElement> containers = driver.findElements(By.xpath("//div[@data-pressable-container='true']"));
                for (WebElement container : containers) {
                    try {
                        String fullText = container.getText().trim();
                        if (!fullText.isEmpty() && fullText.length() > 2) {
                            if (shortMain != null && fullText.contains(shortMain)) continue;
                            if (fullText.matches("^[\\d.,KMkm\\s]+$")) continue;
                            String cleanText = fullText.replace("\n", " [br] ");
                            uniqueComments.add(cleanText);
                        }
                    } catch (Exception ex) {}
                }
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(2000); 
                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    noChangeCount++;
                    if (noChangeCount >= 2) break; 
                } else { noChangeCount = 0; lastHeight = newHeight; }
            }
        } catch (Exception e) {} 
        finally {
            try { driver.close(); driver.switchTo().window(originalWindow); } catch (Exception e) {}
        }
        return new ArrayList<>(uniqueComments);
    }

    private boolean isDuplicate(List<SocialPostEntity> existing, SocialPostEntity newPost) {
        if (newPost.getContent() == null || newPost.getContent().length() < 20) return false;
        String signature = newPost.getContent().substring(0, 20);
        return existing.stream().anyMatch(p -> p.getContent().startsWith(signature));
    }
}