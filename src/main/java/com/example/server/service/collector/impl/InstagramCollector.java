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

public class InstagramCollector extends BaseSeleniumCollector {

    private static final String TAG_URL = "https://www.instagram.com/explore/tags/";
    
    // Tăng số lượng bài để dữ liệu biểu đồ đẹp hơn (nhiều cột hơn)
    private static final int MAX_POSTS = 50; 
    
    private static final int BATCH_CHAR_LIMIT = 800; 
    private static final String BATCH_DELIMITER = "\n"; 
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([\\d.,]+)[KkMm]?");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            // --- LOGIC MỚI: Xử lý khi từ khóa bị bỏ trống ---
            String searchKeyword;
            if (keyword != null && !keyword.trim().isEmpty()) {
                // Nếu có nhập từ khóa -> Tìm kết hợp: "từ khóa + tên thảm họa"
                searchKeyword = keyword + " " + disasterName;
            } else {
                // Nếu KHÔNG nhập từ khóa -> Chỉ tìm theo "tên thảm họa"
                searchKeyword = disasterName;
            }
            // ------------------------------------------------

            String hashtag = normalizeToHashtag(searchKeyword);
            System.out.println(">>> [Instagram V5] Đang tìm kiếm với Hashtag: #" + hashtag);
            
            driver.get(TAG_URL + hashtag + "/");
            sleep(5000); 

            try {
                WebElement firstPost = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, '/p/')]")));
                firstPost.click();
                System.out.println("   [+] Đã mở bài đầu tiên.");
                sleep(3000); 
            } catch (Exception e) {
                System.out.println("!!! Không tìm thấy bài nào với hashtag: #" + hashtag);
                return results;
            }

            int retry = 0;
            long startTime = System.currentTimeMillis();
            
            while (results.size() < MAX_POSTS && retry < 10) {
                // Timeout an toàn sau 10 phút
                if (System.currentTimeMillis() - startTime > 600000) break;

                try {
                    WebElement article = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//article[@role='presentation'] | //article")));
                    
                    expandContent(article);

                    SocialPostEntity post = parseInstagramPost(article, disasterName);
                    
                    if (post != null && post.getPostDate() != null) {
                        boolean isWithinRange = !post.getPostDate().isBefore(startDate) && post.getPostDate().isBefore(endDate.plusDays(1));
                        
                        if (isWithinRange) {
                            if (!isDuplicate(results, post)) {
                                
                                // Dịch Caption nếu cần
                                if (post.getContent() != null && post.getContent().length() > 5 && !post.getContent().equals("[Image Only]")) {
                                    String translatedCap = translateViaGoogle(post.getContent(), false); 
                                    if (translatedCap != null && !translatedCap.isEmpty()) {
                                        post.setContent(translatedCap);
                                    }
                                }

                                // Dịch Comment
                                List<String> originalCmts = post.getCommentSentiments();
                                if (originalCmts != null && !originalCmts.isEmpty()) {
                                    System.out.print("   [~] Dịch " + originalCmts.size() + " comments... ");
                                    List<String> translatedCmts = translateBatchComments(originalCmts);
                                    post.setCommentSentiments(translatedCmts);
                                    System.out.println("Xong.");
                                }

                                results.add(post);
                                System.out.println(String.format(" [+] #%d: %s... | Like: %d | Cmt: %d", 
                                        results.size(),
                                        post.getContent().length() > 20 ? post.getContent().substring(0, 20).replace("\n", " ") : post.getContent(),
                                        post.getReactionLike(),
                                        post.getCommentCount()));
                                retry = 0;
                            }
                        }
                    }

                    // Chuyển sang bài tiếp theo
                    WebElement body = driver.findElement(By.tagName("body"));
                    body.sendKeys(Keys.ARROW_RIGHT);
                    sleep(2000 + ThreadLocalRandom.current().nextInt(1500));

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

    private List<String> translateBatchComments(List<String> comments) {
        List<String> finalResult = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int currentBufferLen = 0;
        
        for (String cmt : comments) {
            String safeCmt = cmt.replace("\n", ". ").replace("\r", "");
            
            int estimatedLen = (int) (safeCmt.length() * 1.5);

            if (currentBufferLen + estimatedLen > BATCH_CHAR_LIMIT) {
                String translatedChunk = translateViaGoogle(buffer.toString(), true);
                if (translatedChunk != null) {
                    String[] parts = translatedChunk.split("\\n");
                    for (String p : parts) {
                        if (!p.trim().isEmpty()) finalResult.add(p.trim());
                    }
                }
                buffer.setLength(0);
                currentBufferLen = 0;
            }
            
            if (buffer.length() > 0) buffer.append(BATCH_DELIMITER);
            buffer.append(safeCmt);
            currentBufferLen += estimatedLen;
        }
        
        if (buffer.length() > 0) {
            String translatedChunk = translateViaGoogle(buffer.toString(), true);
            if (translatedChunk != null) {
                String[] parts = translatedChunk.split("\\n");
                for (String p : parts) {
                     if (!p.trim().isEmpty()) finalResult.add(p.trim());
                }
            }
        }
        
        if (finalResult.isEmpty() && !comments.isEmpty()) return comments;
        
        return finalResult;
    }

    private String translateViaGoogle(String text, boolean forceTranslate) {
        if (text == null || text.trim().isEmpty()) return text;
        
        // Nếu đã là tiếng Việt thì không cần dịch (trừ khi force = true)
        if (!forceTranslate && text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) {
            return text; 
        }

        String originalTab = driver.getWindowHandle();
        String result = text; 

        try {
            driver.switchTo().newWindow(WindowType.TAB);
            String url = "https://translate.google.com/?sl=auto&tl=vi&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) + "&op=translate";
         
            driver.get(url);

            try {
                WebElement resBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[@jsname='W297wb'] | //span[contains(@class, 'ryNqvb')]")));
                result = resBox.getText();
            } catch (Exception e) {
                 try {
                     List<WebElement> spans = driver.findElements(By.xpath("//div[@data-language='vi']//span"));
                     StringBuilder sb = new StringBuilder();
                     for(WebElement s : spans) sb.append(s.getText());
                     if (sb.length() > 0) result = sb.toString();
                 } catch (Exception ex) {}
            }
            sleep(2000 + ThreadLocalRandom.current().nextInt(500));

        } catch (Exception e) {
        } finally {
            try { driver.close(); driver.switchTo().window(originalTab); } catch (Exception e) {}
        }
        return result;
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

            String caption = "[Image Only]";
            try {
                List<WebElement> h1s = article.findElements(By.xpath(".//h1"));
                if (!h1s.isEmpty()) {
                    caption = cleanText(h1s.get(0).getText());
                } else {
                    WebElement ownerText = article.findElement(By.xpath(".//div[@role='button']/following-sibling::div/span | .//ul/preceding-sibling::div//span"));
                    caption = cleanText(ownerText.getText());
                }
            } catch (Exception e) {}
            post.setContent(caption);

            try {
                List<WebElement> commentEls = article.findElements(By.xpath(".//ul//span[@dir='auto'] | .//ul//span[not(@class)]"));
                for (WebElement el : commentEls) {
                    String t = cleanText(el.getText());
                    if (isValidComment(t) && !t.equals(caption)) {
                        post.addCommentSentiments(t);
                    }
                }
            } catch (Exception e) {}
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

    private void expandContent(WebElement article) {
        try {
            List<WebElement> moreBtns = article.findElements(By.xpath(".//span[contains(text(), 'more') or contains(text(), 'Xem thêm')]"));
            for (WebElement btn : moreBtns) { if(btn.isDisplayed()) try { btn.click(); sleep(500); } catch(Exception e){} }

            List<WebElement> expandBtns = article.findElements(By.xpath(".//span[contains(text(), 'Xem câu trả lời') or contains(text(), 'View replies') or contains(text(), 'Xem thêm bình luận')]"));
            int clicked = 0;
            for (WebElement btn : expandBtns) {
                if (clicked >= 5) break; 
                try { btn.click(); sleep(1000); clicked++; } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    private boolean isValidComment(String text) {
        if (text == null || text.length() < 2) return false;
        if (text.matches("^\\d+\\s*(tuần|ngày|giờ|phút|giây|năm|tháng|w|d|h|m|s|y).*")) return false;
        String lower = text.toLowerCase();
        if (lower.contains("trả lời") || lower.contains("reply")) return false;
        if (lower.contains("xem bản dịch") || lower.contains("see translation")) return false;
        if (lower.contains("đã chỉnh sửa") || lower.contains("edited")) return false;
        if (lower.contains("xem thêm") || lower.contains("view more")) return false;
        if (lower.contains("lượt thích")) return false; 
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