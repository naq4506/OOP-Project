package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XCollector extends BaseSeleniumCollector {

    // URL tìm kiếm: f=top (Nổi bật), src=typed_query
    private static final String SEARCH_URL = "https://x.com/search?q=";
    private static final int MAX_POSTS = 3;
    
    // Regex bắt số (1.5K, 2M)
    private static final Pattern METRIC_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)([KkMmBbtT]?)");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            System.out.println(">>> [XCollector] Mode: Cào + Google Dịch (Anh -> Việt). Key: " + keyword);
            
            // X thích tìm kiếm bằng tiếng Anh hoặc từ khóa không dấu
            // Nhưng cứ search theo input của user
            String encodedKey = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            driver.get(SEARCH_URL + encodedKey + "&src=typed_query&f=top");
            sleep(5000); // Chờ X load

            long startTime = System.currentTimeMillis();
            
            while (results.size() < MAX_POSTS) {
                if (System.currentTimeMillis() - startTime > 400000) break; // Timeout 6-7 phút vì phải dịch

                // Lấy tất cả các tweet đang hiển thị
                // X dùng data-testid="tweet" để định danh
                List<WebElement> tweets = driver.findElements(By.xpath("//article[@data-testid='tweet']"));

                int postsBefore = results.size();

                for (WebElement tweet : tweets) {
                    if (results.size() >= MAX_POSTS) break;

                    try {
                        SocialPostEntity post = parseTweetRaw(tweet, disasterName);

                        if (post != null && post.getPostDate() != null) {
                            
                            // Check ngày
                            boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));
                            
                            if (isWithinRange) {
                                if (!isDuplicate(results, post)) {
                                    
                                    // --- BƯỚC DỊCH THUẬT ---
                                    // Chỉ dịch nếu có nội dung và không phải tiếng Việt sẵn
                                    if (post.getContent() != null && !post.getContent().isEmpty()) {
                                        System.out.print("   [~] Đang dịch bài dài " + post.getContent().length() + " ký tự...");
                                        String vnText = translateEngToViet(post.getContent());
                                        
                                        if (!vnText.isEmpty()) {
                                            // Lưu format: [Dịch] Nội dung... \n [Gốc] Original...
                                            post.setContent("[Dịch] " + vnText + "\n\n-----------------\n[Gốc X] " + post.getContent());
                                            System.out.println(" Xong!");
                                        } else {
                                            System.out.println(" Lỗi dịch -> Dùng gốc.");
                                        }
                                    }
                                    // -----------------------

                                    results.add(post);
                                    System.out.println(String.format(" [+] X #%d: %s... | Like: %d", 
                                            results.size(),
                                            post.getContent().substring(0, Math.min(post.getContent().length(), 20)).replace("\n", " "),
                                            post.getReactionLike()));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // e.printStackTrace(); 
                    }
                }

                // Scroll xuống
                driver.findElement(By.tagName("body")).sendKeys(Keys.PAGE_DOWN);
                sleep(3000);
                
                // Nếu không thêm được bài nào sau khi scroll -> Scroll thêm phát nữa mạnh hơn
                if (results.size() == postsBefore && results.size() > 0) {
                     driver.findElement(By.tagName("body")).sendKeys(Keys.END);
                     sleep(4000);
                     // Nếu vẫn không có bài mới -> Break
                     if (results.size() == postsBefore) break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }

        return results;
    }

    // --- LOGIC DỊCH GOOGLE (TAB SWITCHING) ---
    private String translateEngToViet(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String originalTab = driver.getWindowHandle();
        String result = "";

        try {
            // 1. Mở Tab mới
            driver.switchTo().newWindow(WindowType.TAB);
            
            // 2. Gọi URL Google Dịch (Auto -> Vietnamese)
            // URLEncoder quan trọng để xử lý ký tự đặc biệt trong tweet
            String url = "https://translate.google.com/?sl=auto&tl=vi&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) + "&op=translate";
            driver.get(url);

            // 3. Chờ kết quả hiện ra
            // Google có class "ryNqvb" hoặc span[jsname='W297wb'] chứa kết quả
            try {
                WebElement resBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[@jsname='W297wb'] | //span[contains(@class, 'ryNqvb')]")));
                result = resBox.getText();
            } catch (Exception e) {
                // Thử cách khác nếu selector trên chết
                try {
                     List<WebElement> spans = driver.findElements(By.xpath("//div[@data-language='vi']//span"));
                     StringBuilder sb = new StringBuilder();
                     for(WebElement s : spans) sb.append(s.getText());
                     result = sb.toString();
                } catch (Exception ex) {}
            }
            
            // Random sleep để Google không chặn IP vì request quá nhanh
            sleep(1500 + ThreadLocalRandom.current().nextInt(1000));

        } catch (Exception e) {
            System.out.println("Lỗi dịch: " + e.getMessage());
        } finally {
            // 4. Đóng tab dịch, quay về tab X
            try {
                driver.close();
                driver.switchTo().window(originalTab);
            } catch (Exception e) {}
        }
        return result;
    }

    private SocialPostEntity parseTweetRaw(WebElement tweet, String disasterName) {
        SocialPostEntity post = new SocialPostEntity();
        post.setDisasterName(disasterName);
        post.setPlatform("X");

        try {
            // 1. Parse Content
            try {
                WebElement textEl = tweet.findElement(By.xpath(".//div[@data-testid='tweetText']"));
                post.setContent(cleanText(textEl.getText()));
            } catch (Exception e) {
                // Tweet chỉ có ảnh/video mà không có text
                return null; 
            }

            // 2. Parse Date (X dùng thẻ <time>)
            try {
                WebElement timeEl = tweet.findElement(By.xpath(".//time"));
                String isoDate = timeEl.getAttribute("datetime");
                if (isoDate != null) {
                    LocalDateTime dt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME);
                    post.setPostDate(dt);
                }
            } catch (Exception e) {
                post.setPostDate(LocalDateTime.now());
            }

            // 3. Parse Metrics (Reply, Retweet, Like)
            // X hiển thị metrics trong group role='group'
            // data-testid="reply", "retweet", "like"
            post.setCommentCount(parseMetricFromTestId(tweet, "reply"));
            int reposts = parseMetricFromTestId(tweet, "retweet");
            post.setReactionLike(parseMetricFromTestId(tweet, "like"));
            
            // X: Share thường tính là Repost + Bookmark (hoặc View nếu muốn)
            // Ở đây gán Share = Repost
            post.setShareCount(reposts);
            post.setTotalReactions(post.getReactionLike());

            return post;

        } catch (Exception e) {
            return null;
        }
    }

    private int parseMetricFromTestId(WebElement parent, String testId) {
        try {
            // Tìm nút theo data-testid (ví dụ: data-testid="like")
            WebElement btn = parent.findElement(By.xpath(".//div[@data-testid='" + testId + "']"));
            
            // Số thường nằm trong aria-label (VD: "123 likes") hoặc text hiển thị
            String label = btn.getAttribute("aria-label");
            if (label != null && METRIC_PATTERN.matcher(label).find()) {
                 // Nếu aria-label ngon (VD: "50 likes")
                 return parseNumber(label);
            }
            
            // Nếu không, lấy text hiển thị (VD: "1.2K")
            String text = btn.getText();
            return parseNumber(text);
            
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseNumber(String text) {
        if (text == null) return 0;
        text = text.replace(",", "."); // 1,5 -> 1.5
        Matcher m = METRIC_PATTERN.matcher(text);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String suffix = m.group(2).toUpperCase();
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

    private boolean isDuplicate(List<SocialPostEntity> existing, SocialPostEntity newPost) {
        if (newPost.getContent() == null || newPost.getContent().length() < 10) return false;
        // Check trùng dựa trên nội dung gốc (vì nội dung dịch có thể khác nhau xíu)
        // Nhưng ở đây ta check đoạn đầu
        String signature = newPost.getContent().substring(0, Math.min(newPost.getContent().length(), 15));
        return existing.stream().anyMatch(p -> p.getContent().startsWith(signature));
    }
}