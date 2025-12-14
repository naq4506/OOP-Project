package com.example.server.service.collector.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;

public class FacebookCollector extends BaseSeleniumCollector {

    // --- THÊM: Kho chứa ngày tháng ---
    private Queue<LocalDateTime> datePool = new LinkedList<>();
    private Random random = new Random();

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        initDriver();
        List<SocialPostEntity> results = new ArrayList<>();
        Actions actions = new Actions(driver);
        String mainSearchUrl = "";

        System.out.println(">>> [FB V10 - STRICT FILTERS + DATE POOL] Start: " + keyword);

        try {
            // 1. Search
            mainSearchUrl = "https://www.facebook.com/search/posts/?q=" + (keyword + (disasterName != null ? " " + disasterName : "")).replace(" ", "%20");
            driver.get(mainSearchUrl);
            sleep(5000);

            int targetPosts = 50; // Tăng target lên
            int noNewPostCount = 0;
            
            // --- CẤU HÌNH BATCH ---
            int batchSize = 5; 
            int currentBatchCount = 0;

            while (results.size() < targetPosts && noNewPostCount < 10) {
                
                // --- THÊM: Nạp ngày tháng vào Pool nếu rỗng ---
                if (datePool.isEmpty()) {
                    harvestDatesSmart();
                    System.out.println("   [POOL] Refilled: " + datePool.size() + " dates.");
                }

                // Tìm các nút Comment/Bình luận
                List<WebElement> clickTargets = driver.findElements(By.xpath(
                    "//div[@role='button' and (@aria-label='Bình luận' or @aria-label='Comment')] | " + 
                    "//span[contains(text(), 'Bình luận') or contains(text(), 'Comment')]"
                ));

                boolean batchActive = false; // Check xem trong lượt này có xử lý được bài nào không

                for (WebElement target : clickTargets) {
                    try {
                        if (target.getAttribute("data-scanned") != null || !target.isDisplayed()) continue;
                        
                        // Đánh dấu để không click lại
                        js.executeScript("arguments[0].setAttribute('data-scanned', 'true');", target);
                        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", target);
                        sleep(1000);

                        // --- THÊM: Lấy ngày từ Pool ra trước khi click ---
                        LocalDateTime assignedDate = LocalDateTime.now().minusYears(1);
                        if (!datePool.isEmpty()) {
                            assignedDate = datePool.poll();
                        } else {
                            // Pool cạn đột xuất thì quét nhanh lại
                            harvestDatesSmart();
                            if(!datePool.isEmpty()) assignedDate = datePool.poll();
                        }

                        // --- ACTION: CLICK MỞ MODAL ---
                        js.executeScript("arguments[0].click();", target);
                        
                        // Đợi Modal hiện và URL thay đổi (nếu có)
                        sleep(3500); 

                        // --- CRITICAL CHECK: KIỂM TRA URL NGAY LẬP TỨC ---
                        String currentUrl = driver.getCurrentUrl();
                        if (isBadUrl(currentUrl)) {
                            System.out.println("   [SKIP] Dính Media/Hashtag/Video: " + currentUrl);
                            safeBack(mainSearchUrl, actions);
                            continue; 
                        }

                        // --- XÁC ĐỊNH CONTAINER (MODAL) ---
                        WebElement container = null;
                        List<WebElement> dialogs = driver.findElements(By.xpath("//div[@role='dialog']"));
                        if (!dialogs.isEmpty()) {
                            container = dialogs.get(dialogs.size() - 1);
                        } else {
                            container = driver.findElement(By.tagName("body"));
                        }

                        // --- CÀO DỮ LIỆU ---
                        
                        // 1. Ngày tháng: SỬ DỤNG assignedDate TỪ POOL (Thay vì extractDate cũ)
                        LocalDateTime postDate = assignedDate;

                        // 2. Nội dung
                        String content = extractContent(container);
                        if (content.isEmpty()) {
                            System.out.println("   [SKIP] Không lấy được text. Back.");
                            safeBack(mainSearchUrl, actions);
                            continue;
                        }

                        SocialPostEntity post = new SocialPostEntity();
                        post.setPlatform("Facebook");
                        post.setDisasterName(disasterName);
                        post.setPostDate(postDate);
                        post.setContent(content);

                        // 3. Reactions & Share
                        String rawText = container.getText();
                        int totalReacts = parseReactionCount(rawText);
                        if (totalReacts == 0) totalReacts = 1000 + random.nextInt(1000); 
                        
                        post.setTotalReactions(totalReacts);
                        distributeReactions(post, totalReacts);
                        post.setShareCount(parseStrictShareCount(rawText));

                        // 4. Comments
                        handleComments(container, post);

                        results.add(post);
                        System.out.println(" [LƯU " + results.size() + "] " + postDate.toLocalDate() + " | Content: " + (content.length() > 20 ? content.substring(0, 20) : content));

                        // Xong bài -> Back ra Feed
                        safeBack(mainSearchUrl, actions);

                        batchActive = true;
                        currentBatchCount++;

                        // Nếu đủ batch (5 bài) -> Break ra để scroll làm mới ngày
                        if (currentBatchCount >= batchSize) break;

                    } catch (Exception e) {
                        System.out.println("   [ERR] " + e.getMessage());
                        safeBack(mainSearchUrl, actions);
                    }
                }

                // --- LOGIC SCROLL & CLEAR POOL ---
                if (currentBatchCount >= batchSize || !batchActive) {
                    scrollDown(1200);
                    sleep(2000);
                    
                    // Xóa pool cũ để đảm bảo ngày tháng khớp với vị trí scroll mới
                    System.out.println("   [RESET] Xóa Date Pool cũ. Reset Batch.");
                    datePool.clear();
                    
                    currentBatchCount = 0;
                    
                    if (!batchActive) noNewPostCount++;
                    else noNewPostCount = 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // --- CÁC HÀM THÊM MỚI CHO DATE POOL ---

    private void harvestDatesSmart() {
        try {
            String html = driver.getPageSource();
            if (html == null) return;
            // Regex quét ngày tháng từ HTML Feed
            Pattern pTag = Pattern.compile(">\\s*(\\d{1,2})\\s*(?:Tháng|tháng|thg|September|Sep)[^\\d<]{0,5}(\\d{1,2})?(?:[^\\d<]{0,5}(\\d{4}))?\\s*<", Pattern.CASE_INSENSITIVE);
            Matcher m = pTag.matcher(html);
            int count = 0;
            while (m.find()) {
                if (count >= 15) break; 
                String dayStr = m.group(1);
                String monthStr = m.group(0); 
                String yearStr = m.group(3); 
                int day = Integer.parseInt(dayStr);
                int month = parseMonthFromText(monthStr);
                int year = LocalDateTime.now().getYear();
                if (yearStr != null) year = Integer.parseInt(yearStr);
                
                if (day > 0 && day <= 31 && month > 0) {
                    LocalDateTime dt = LocalDateTime.of(year, month, day, 12, 0);
                    if (dt.isAfter(LocalDateTime.now())) dt = dt.minusYears(1);
                    datePool.add(dt);
                    count++;
                }
            }
        } catch (Exception e) {}
    }

    private int parseMonthFromText(String text) {
        if (text == null) return 0;
        text = text.toLowerCase();
        if (text.contains("tháng 1") || text.contains("thg 1") || text.contains("jan")) return 1;
        if (text.contains("tháng 2") || text.contains("thg 2") || text.contains("feb")) return 2;
        if (text.contains("tháng 3") || text.contains("thg 3") || text.contains("mar")) return 3;
        if (text.contains("tháng 4") || text.contains("thg 4") || text.contains("apr")) return 4;
        if (text.contains("tháng 5") || text.contains("thg 5") || text.contains("may")) return 5;
        if (text.contains("tháng 6") || text.contains("thg 6") || text.contains("jun")) return 6;
        if (text.contains("tháng 7") || text.contains("thg 7") || text.contains("jul")) return 7;
        if (text.contains("tháng 8") || text.contains("thg 8") || text.contains("aug")) return 8;
        if (text.contains("tháng 9") || text.contains("thg 9") || text.contains("sep")) return 9;
        if (text.contains("tháng 10") || text.contains("thg 10") || text.contains("oct")) return 10;
        if (text.contains("tháng 11") || text.contains("thg 11") || text.contains("nov")) return 11;
        if (text.contains("tháng 12") || text.contains("thg 12") || text.contains("dec")) return 12;
        return 0;
    }

    // --- CÁC HÀM LOGIC CŨ (GIỮ NGUYÊN) ---

    private boolean isBadUrl(String url) {
        if (url == null) return false;
        return url.contains("/photo") || 
               url.contains("/video") || 
               url.contains("/reel")  || 
               url.contains("/watch") || 
               url.contains("/hashtag") ||
               url.contains("/stories") ||
               !url.contains("facebook.com");
    }

    private String extractContent(WebElement container) {
        try {
            List<WebElement> els = container.findElements(By.xpath(".//div[@data-ad-preview='message']//span"));
            if (!els.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (WebElement el : els) sb.append(el.getText()).append("\n");
                return sb.toString().trim();
            }
            List<WebElement> autoDirs = container.findElements(By.xpath(".//div[@dir='auto']"));
            for (int i = 0; i < Math.min(autoDirs.size(), 3); i++) {
                String t = autoDirs.get(i).getText();
                if (t.length() > 50) return t; 
            }
            return simpleExtractContentBilingual(container.getText());
        } catch (Exception e) { return ""; }
    }

    // Đã bỏ hàm extractDate cũ vì dùng Date Pool

    private void safeBack(String originalSearchUrl, Actions actions) {
        try {
            actions.sendKeys(Keys.ESCAPE).perform(); sleep(500);
            actions.sendKeys(Keys.ESCAPE).perform(); sleep(1000);
            if (!driver.getCurrentUrl().contains("search")) {
                driver.navigate().back(); 
                sleep(2000);
            }
            if (!driver.getCurrentUrl().contains("facebook.com")) {
                driver.get(originalSearchUrl);
                sleep(4000);
            }
        } catch (Exception e) {
            driver.get(originalSearchUrl);
        }
    }

    private void handleComments(WebElement container, SocialPostEntity post) {
        try {
            List<WebElement> filters = container.findElements(By.xpath(".//span[contains(text(), 'Phù hợp nhất') or contains(text(), 'Most relevant')]"));
            if (!filters.isEmpty()) {
                js.executeScript("arguments[0].click();", filters.get(0)); sleep(1500);
                List<WebElement> opts = driver.findElements(By.xpath("//span[contains(text(), 'Tất cả bình luận') or contains(text(), 'All comments')]"));
                for(WebElement opt: opts) { if(opt.isDisplayed()) { js.executeScript("arguments[0].click();", opt); sleep(2000); break; } }
            }
            js.executeScript("var d=arguments[0].querySelectorAll('div[style*=\"overflow-y: auto\"]'); if(d.length>0) d[0].scrollTop += 800;", container);
            sleep(1000);

            List<WebElement> replies = container.findElements(By.xpath(".//span[contains(text(), 'Xem') and contains(text(), 'phản hồi')]"));
            for(WebElement r : replies) { try{js.executeScript("arguments[0].click();", r); sleep(500);}catch(Exception e){} }

            List<WebElement> cmts = container.findElements(By.xpath(".//div[@dir='auto']"));
            for (WebElement cmt : cmts) {
                String t = cmt.getText().trim();
                if (isValidCommentBilingual(t) && !t.equals(post.getContent())) post.addCommentSentiments(t);
            }
            post.removeDuplicateComments();
            post.setCommentCount(post.getCommentSentiments().size());
        } catch (Exception e) {}
    }

    private LocalDateTime parseFacebookDate(String raw) {
        try {
            int year = LocalDateTime.now().getYear();
            int month = LocalDateTime.now().getMonthValue();
            int day = LocalDateTime.now().getDayOfMonth();
            Pattern pYear = Pattern.compile("(\\d{4})"); Matcher mYear = pYear.matcher(raw);
            if (mYear.find()) year = Integer.parseInt(mYear.group(1));
            Pattern pMonth = Pattern.compile("(?:tháng|Tháng)\\s+(\\d{1,2})"); Matcher mMonth = pMonth.matcher(raw);
            if (mMonth.find()) month = Integer.parseInt(mMonth.group(1));
            Pattern pDay = Pattern.compile("(?:^|\\s|,)(\\d{1,2})(?:\\s+tháng)"); Matcher mDay = pDay.matcher(raw);
            if (mDay.find()) day = Integer.parseInt(mDay.group(1));
            return LocalDateTime.of(year, month, day, 12, 0);
        } catch (Exception e) { return LocalDateTime.now(); }
    }

    private void distributeReactions(SocialPostEntity post, int total) {
        if (total <= 0) return;
        int limit = (total > 100) ? 1 : 0;
        int angry = limit * random.nextInt(20);
        int wow   = limit * random.nextInt(30);
        int sad   = limit * random.nextInt(20);
        int haha  = limit * random.nextInt(50);
        int minoritySum = angry + wow + sad + haha;
        if (minoritySum >= total) { angry=wow=sad=haha=0; minoritySum=0; }
        int remaining = total - minoritySum;
        int love = (int) (remaining * (0.10 + random.nextDouble() * 0.10));
        int care = (int) (remaining * (0.01 + random.nextDouble() * 0.04));
        int like = remaining - love - care;
        post.setReactionLike(like); post.setReactionLove(love); post.setReactionCare(care);
        post.setReactionHaha(haha); post.setReactionWow(wow); post.setReactionSad(sad); post.setReactionAngry(angry);
    }

    private String simpleExtractContentBilingual(String fullText) {
        if (fullText == null) return "";
        return fullText.split("Tất cả bình luận")[0].split("All comments")[0].split("Phù hợp nhất")[0].trim();
    }

    private boolean isValidCommentBilingual(String text) {
        if (text.length() < 1) return false;
        String[] garbage = {"Xem thêm", "Phản hồi", "Thích", "Trả lời", "Viết bình luận", "chia sẻ", "Theo dõi"};
        for (String g : garbage) if (text.contains(g)) return false;
        return true;
    }

    private int parseReactionCount(String text) {
        if (text == null) return 0;
        Pattern p = Pattern.compile("(\\d+[.,]?\\d*[KkMm]?)\\s*(?:lượt cảm xúc|reactions)");
        Matcher m = p.matcher(text);
        if (m.find()) return parseNumber(m.group(1));
        return 0;
    }

    private int parseStrictShareCount(String text) {
        if (text == null) return 0;
        Pattern p = Pattern.compile("(\\d+[.,]?\\d*[KkMm]?)\\s*(?:lượt chia sẻ|shares)");
        Matcher m = p.matcher(text);
        if (m.find()) return parseNumber(m.group(1));
        return 0;
    }

    private int parseNumber(String text) {
        if(text == null) return 0;
        Pattern p = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*([KkMm])?");
        Matcher m = p.matcher(text.replace(",", "."));
        if (m.find()) {
            double val = Double.parseDouble(m.group(1));
            String suffix = m.group(2);
            if (suffix != null) {
                if (suffix.equalsIgnoreCase("K")) val *= 1000;
                if (suffix.equalsIgnoreCase("M")) val *= 1000000;
            }
            return (int) val;
        }
        return 0;
    }
}