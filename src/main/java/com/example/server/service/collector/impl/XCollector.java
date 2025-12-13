package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XCollector extends BaseSeleniumCollector {

    private static final String SEARCH_URL = "https://x.com/search?q=";
    private static final int MAX_POSTS = 5; // Số bài muốn lấy
    private static final int MAX_COMMENTS_PER_POST = 10; // Số comment muốn lấy mỗi bài

    // Regex bắt số (1.5K, 2M)
    private static final Pattern METRIC_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)([KkMmBbtT]?)");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            System.out.println(">>> [XCollector V3] Mode: Full Dịch + Scroll Comment + Fix Search Scroll. Key: " + keyword);
            
            String encodedKey = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            // src=typed_query&f=live để lấy bài mới nhất (hoặc f=top cho bài nổi bật)
            driver.get(SEARCH_URL + encodedKey + "&src=typed_query&f=top");
            sleep(5000); 

            long startTime = System.currentTimeMillis();
            Set<String> processedUrls = new HashSet<>(); 
            int noNewPostCount = 0; // Đếm số lần scroll mà không thấy bài mới

            while (results.size() < MAX_POSTS) {
                if (System.currentTimeMillis() - startTime > 900000) break; // Timeout 15 phút

                // 1. Quét lấy link bài viết trên màn hình hiện tại
                List<WebElement> tweetElements = driver.findElements(By.xpath("//article[@data-testid='tweet']//a[contains(@href, '/status/')]"));
                List<String> newUrlsInView = new ArrayList<>();
                
                for (WebElement el : tweetElements) {
                    String url = el.getAttribute("href");
                    if (url != null && !url.contains("/photo/") && !url.contains("/video/") && !processedUrls.contains(url)) {
                        newUrlsInView.add(url);
                    }
                }

                // 2. Logic Scroll thông minh hơn
                if (newUrlsInView.isEmpty()) {
                    noNewPostCount++;
                    System.out.println("   [Search] Không thấy bài mới, đang cuộn xuống... (" + noNewPostCount + ")");
                    
                    // Scroll mạnh xuống cuối
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    sleep(4000); // X load khá chậm, cần chờ lâu
                    
                    if (noNewPostCount >= 5) break; // Nếu 5 lần scroll mà không ra gì thì dừng
                    continue;
                } else {
                    noNewPostCount = 0; // Reset nếu tìm thấy bài
                }

                // 3. Duyệt từng bài tìm được
                for (String url : newUrlsInView) {
                    if (results.size() >= MAX_POSTS) break;
                    if (processedUrls.contains(url)) continue;

                    processedUrls.add(url); 
                    
                    try {
                        SocialPostEntity post = processDetailPost(url, disasterName);

                        if (post != null) {
                            boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));
                            
                            if (isWithinRange) {
                                results.add(post);
                                System.out.println(String.format(" [+] LẤY XONG (#%d): %s... | Cmt: %d | Like: %d", 
                                        results.size(),
                                        post.getContent().length() > 20 ? post.getContent().substring(0, 20).replace("\n", "") : "...",
                                        post.getCommentCount(),
                                        post.getReactionLike()));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi bài: " + url);
                    }
                }
                
                // Sau khi xử lý hết đống link hiện tại, lại scroll tiếp để tìm đợt link mới
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(3000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }

        return results;
    }

    // === XỬ LÝ CHI TIẾT ===
    private SocialPostEntity processDetailPost(String postUrl, String disasterName) {
        String originalWindow = driver.getWindowHandle();
        SocialPostEntity post = new SocialPostEntity();
        post.setDisasterName(disasterName);
        post.setPlatform("X");
        
        try {
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(postUrl);
            sleep(3000);

            WebElement mainTweet = driver.findElement(By.xpath("//article[@data-testid='tweet']"));
            
            // 1. Lấy Text gốc
            String originalContent = "";
            try {
                WebElement textEl = mainTweet.findElement(By.xpath(".//div[@data-testid='tweetText']"));
                originalContent = cleanText(textEl.getText());
            } catch (Exception e) { return null; }

            // 2. Lấy ngày
            try {
                WebElement timeEl = mainTweet.findElement(By.xpath(".//time"));
                String isoDate = timeEl.getAttribute("datetime");
                post.setPostDate(LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) { post.setPostDate(LocalDateTime.now()); }

            // 3. Lấy Metrics (Trước khi scroll comment)
            int replyCount = parseMetricFromTestId(mainTweet, "reply");
            int retweetCount = parseMetricFromTestId(mainTweet, "retweet");
            int likeCount = parseMetricFromTestId(mainTweet, "like");

            post.setCommentCount(replyCount);
            post.setShareCount(retweetCount);
            post.setReactionLike(likeCount);
            post.setTotalReactions(likeCount);

            // 4. Lấy Comment (Có Scroll)
            List<String> comments = crawlCommentsWithScroll();
            post.setCommentSentiments(comments);

            // 5. DỊCH THUẬT & GHI ĐÈ NỘI DUNG
            String translatedContent = translateViaSeleniumTab(originalContent);
            
            // Yêu cầu: Chỉ giữ bản dịch. Nếu dịch lỗi (rỗng) thì mới giữ gốc.
            if (!translatedContent.isEmpty() && !translatedContent.equals(originalContent)) {
                post.setContent(translatedContent);
            } else {
                post.setContent(originalContent);
            }

            return post;

        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (driver.getWindowHandles().size() > 1) {
                    driver.close();
                    driver.switchTo().window(originalWindow);
                }
            } catch (Exception e) {}
        }
    }

    // === HÀM LẤY COMMENT CÓ CUỘN TRANG ===
    private List<String> crawlCommentsWithScroll() {
        Set<String> uniqueComments = new LinkedHashSet<>();
        int attempts = 0;
        
        // Scroll tối đa 5 lần hoặc cho đến khi đủ comment
        while (uniqueComments.size() < MAX_COMMENTS_PER_POST && attempts < 5) {
            try {
                // Lấy tất cả comment đang hiện
                List<WebElement> replyEls = driver.findElements(By.xpath("//div[@data-testid='cellInnerDiv']//article[@data-testid='tweet']"));
                
                for (WebElement reply : replyEls) {
                    if (uniqueComments.size() >= MAX_COMMENTS_PER_POST) break;
                    try {
                        // Bỏ qua nếu là bài viết chính (đôi khi X render bài chính cũng là cellInnerDiv)
                        if (reply.getAttribute("tabindex").equals("-1")) continue; 

                        WebElement textEl = reply.findElement(By.xpath(".//div[@data-testid='tweetText']"));
                        String cmt = cleanText(textEl.getText());
                        if (cmt.length() > 0) uniqueComments.add(cmt);
                    } catch (Exception ex) {}
                }

                if (uniqueComments.size() >= MAX_COMMENTS_PER_POST) break;

                // Cuộn xuống để load thêm comment
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 600);");
                sleep(1500); 
                attempts++;
                
            } catch (Exception e) {
                break;
            }
        }
        return new ArrayList<>(uniqueComments);
    }

    // === MODULE DỊCH ===
    private String translateViaSeleniumTab(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        // Nếu là tiếng Việt rồi thì thôi
        if (text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) return text;

        String detailTab = driver.getWindowHandle();
        String result = "";

        try {
            driver.switchTo().newWindow(WindowType.TAB);
            String url = "https://translate.google.com/?sl=auto&tl=vi&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) + "&op=translate";
            driver.get(url);
            
            sleep(3000); // Chờ 3s để nhìn thấy tab

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement resultEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.ryNqvb")));
            result = resultEl.getText();

        } catch (Exception e) {
            System.err.println("Lỗi dịch: " + e.getMessage());
            return text; // Fallback về gốc nếu lỗi
        } finally {
            try {
                driver.close(); 
                driver.switchTo().window(detailTab);
            } catch (Exception e) {}
        }
        return result;
    }

    private int parseMetricFromTestId(WebElement parent, String testId) {
        try {
            WebElement btn = parent.findElement(By.xpath(".//div[@data-testid='" + testId + "'] | .//button[@data-testid='" + testId + "']"));
            String label = btn.getAttribute("aria-label");
            if (label != null) {
                Matcher m = METRIC_PATTERN.matcher(label);
                if (m.find()) return parseNumber(m.group(1) + (m.group(2) != null ? m.group(2) : ""));
            }
            return parseNumber(btn.getText());
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseNumber(String text) {
        if (text == null || text.isEmpty()) return 0;
        text = text.replace(",", ".");
        Matcher m = METRIC_PATTERN.matcher(text);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String suffix = m.group(2) != null ? m.group(2).toUpperCase() : "";
                if (suffix.equals("K")) val *= 1000;
                else if (suffix.equals("M")) val *= 1000000;
                else if (suffix.equals("B")) val *= 1000000000;
                return (int) val;
            } catch (Exception e) {}
        }
        return 0;
    }

    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("\"", "'").trim();
    }
}