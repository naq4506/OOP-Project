package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
    private static final int MAX_POSTS = 5; 
    private static final int MAX_COMMENTS_PER_POST = 10; 

    private static final Pattern METRIC_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)([KkMmBbtT]?)");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            System.out.println(">>> [XCollector V7] Clean Mode: Kill all extra tabs. Key: " + keyword);
            
            // LƯU GIỮ CÁI TAB GỐC (Tab Search)
            String mainTabHandle = driver.getWindowHandle();

            String encodedKey = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            driver.get(SEARCH_URL + encodedKey + "&src=typed_query&f=top");
            sleep(5000); 

            long startTime = System.currentTimeMillis();
            Set<String> processedUrls = new HashSet<>(); 
            int noNewPostCount = 0; 

            while (results.size() < MAX_POSTS) {
                if (System.currentTimeMillis() - startTime > 900000) break; 

                // Luôn đảm bảo đang đứng ở Tab Gốc trước khi quét
                driver.switchTo().window(mainTabHandle);

                // 1. Quét link
                List<WebElement> tweetElements = driver.findElements(By.xpath("//article[@data-testid='tweet']//a[contains(@href, '/status/') and not(contains(@href, '/photo/')) and not(contains(@href, '/video/'))]"));
                List<String> newUrlsInView = new ArrayList<>();
                for (WebElement el : tweetElements) {
                    String url = el.getAttribute("href");
                    if (url != null && !processedUrls.contains(url)) {
                        newUrlsInView.add(url);
                    }
                }

                // 2. Logic Scroll
                if (newUrlsInView.isEmpty()) {
                    noNewPostCount++;
                    System.out.println("   [Search] Không thấy bài mới (" + noNewPostCount + "), cuộn...");
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    sleep(4000); 
                    if (noNewPostCount >= 5) break; 
                    continue;
                } else {
                    noNewPostCount = 0; 
                }

                // 3. Duyệt bài
                for (String url : newUrlsInView) {
                    if (results.size() >= MAX_POSTS) break;
                    if (processedUrls.contains(url)) continue;

                    processedUrls.add(url); 
                    
                    try {
                        // Truyền mainTabHandle vào để biết đường mà quay về
                        SocialPostEntity post = processDetailPost(url, disasterName, mainTabHandle);

                        if (post != null) {
                            boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));
                            
                            if (isWithinRange) {
                                results.add(post);
                                System.out.println(String.format(" [+] LẤY XONG (#%d): %s... | Cmt: %d", 
                                        results.size(),
                                        post.getContent().length() > 20 ? post.getContent().substring(0, 20).replace("\n", "") : "...",
                                        post.getCommentCount()));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("   [Err] Lỗi xử lý bài: " + url);
                    } finally {
                        // CHỐT CHẶN CUỐI CÙNG: Dọn sạch mọi tab thừa sau mỗi bài
                        closeAllTabsExcept(mainTabHandle);
                    }
                }
                
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

    // === HÀM XỬ LÝ CHI TIẾT ===
    private SocialPostEntity processDetailPost(String postUrl, String disasterName, String mainTabHandle) {
        try {
            // Mở tab mới
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(postUrl);
            sleep(3500); 

            // 1. URL GUARD
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("x.com/home") || !currentUrl.contains("/status/")) {
                System.out.println("   [SKIP] Link lỗi/Redirect Home: " + currentUrl);
                return null; 
            }

            // 2. Đóng Popup
            closeBloatware();

            // 3. Lấy dữ liệu
            SocialPostEntity post = new SocialPostEntity();
            post.setDisasterName(disasterName);
            post.setPlatform("X");

            WebElement mainTweet = driver.findElement(By.xpath("//article[@data-testid='tweet']"));
            
            // Text gốc
            String originalContent = "";
            try {
                WebElement textEl = mainTweet.findElement(By.xpath(".//div[@data-testid='tweetText']"));
                originalContent = cleanText(textEl.getText());
            } catch (Exception e) { return null; }

            // Ngày
            try {
                WebElement timeEl = mainTweet.findElement(By.xpath(".//time"));
                String isoDate = timeEl.getAttribute("datetime");
                post.setPostDate(LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) { post.setPostDate(LocalDateTime.now()); }

            // Metrics
            int replyCount = parseMetricFromTestId(mainTweet, "reply");
            int retweetCount = parseMetricFromTestId(mainTweet, "retweet");
            int likeCount = parseMetricFromTestId(mainTweet, "like");

            post.setCommentCount(replyCount);
            post.setShareCount(retweetCount);
            post.setReactionLike(likeCount);
            post.setTotalReactions(likeCount);

            // Comments
            List<String> comments = crawlCommentsWithScroll();
            post.setCommentSentiments(comments);

            // Dịch thuật (Hàm này có thể mở thêm tab nữa)
            String translatedContent = translateViaSeleniumTab(originalContent);
            if (!translatedContent.isEmpty() && !translatedContent.equals(originalContent)) {
                post.setContent(translatedContent);
            } else {
                post.setContent(originalContent);
            }

            return post;

        } catch (Exception e) {
            return null;
        } 
        // Không cần finally đóng tab ở đây nữa, vì đã có chốt chặn ở vòng lặp main rồi
    }

    // === HÀM QUAN TRỌNG NHẤT: ĐÓNG TẤT CẢ TAB NGOẠI TRỪ TAB GỐC ===
    private void closeAllTabsExcept(String originalHandle) {
        try {
            Set<String> allHandles = driver.getWindowHandles();
            for (String handle : allHandles) {
                if (!handle.equals(originalHandle)) {
                    // Nếu không phải tab gốc thì chuyển sang và đóng
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
            // Cuối cùng quay về tab gốc
            driver.switchTo().window(originalHandle);
        } catch (Exception e) {
            // Phòng hờ lỗi, cố quay về tab gốc
            try { driver.switchTo().window(originalHandle); } catch (Exception ex) {}
        }
    }

    // ... (Các hàm phụ trợ bên dưới giữ nguyên) ...

    private List<String> crawlCommentsWithScroll() {
        Set<String> uniqueComments = new LinkedHashSet<>();
        int attempts = 0;
        
        while (uniqueComments.size() < MAX_COMMENTS_PER_POST && attempts < 5) {
            try {
                List<WebElement> replyEls = driver.findElements(By.xpath("//div[@data-testid='cellInnerDiv']//article[@data-testid='tweet']"));
                for (WebElement reply : replyEls) {
                    if (uniqueComments.size() >= MAX_COMMENTS_PER_POST) break;
                    try {
                        if (reply.getAttribute("tabindex").equals("-1")) continue; 
                        WebElement textEl = reply.findElement(By.xpath(".//div[@data-testid='tweetText']"));
                        String cmt = cleanText(textEl.getText());
                        if (cmt.length() > 0) uniqueComments.add(cmt);
                    } catch (Exception ex) {}
                }
                if (uniqueComments.size() >= MAX_COMMENTS_PER_POST) break;
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 600);");
                sleep(1500); 
                attempts++;
            } catch (Exception e) { break; }
        }
        return new ArrayList<>(uniqueComments);
    }

    private void closeBloatware() {
        try {
            List<WebElement> buttons = driver.findElements(By.xpath(
                "//span[contains(text(), 'Dismiss')] | " + 
                "//div[@role='button']//span[contains(text(), 'Dismiss')] | " +
                "//span[contains(text(), 'Got it')]"
            ));

            if (!buttons.isEmpty()) {
                WebElement btn = buttons.get(0);
                if (btn.isDisplayed()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                    sleep(500); 
                }
            }
        } catch (Exception e) {}
    }

    private String translateViaSeleniumTab(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        if (text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) return text;

        String currentTab = driver.getWindowHandle();
        String result = "";

        try {
            driver.switchTo().newWindow(WindowType.TAB);
            String url = "https://translate.google.com/?sl=auto&tl=vi&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) + "&op=translate";
            driver.get(url);
            sleep(2500);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
            WebElement resultEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.ryNqvb")));
            result = resultEl.getText();

        } catch (Exception e) {
            return text; 
        } finally {
            // Hàm này đóng tab của chính nó luôn cho gọn, 
            // nhưng hàm closeAllTabsExcept bên ngoài sẽ bao thầu hết nếu sót.
            try {
                if(driver.getWindowHandles().size() > 1) {
                    driver.close();
                    driver.switchTo().window(currentTab);
                }
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
        } catch (Exception e) { return 0; }
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