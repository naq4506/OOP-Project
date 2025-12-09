package com.example.server.service.collector.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;

public class FacebookCollector extends BaseSeleniumCollector {

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDate startDate, LocalDate endDate) {
        initDriver();
        List<SocialPostEntity> results = new ArrayList<>();
        Actions actions = new Actions(driver);
        String originalWindow = driver.getWindowHandle();

        System.out.println(">>> [Facebook] Bắt đầu cào: " + keyword);

        try {
            String query = (keyword + " " + disasterName).trim();
            String searchUrl = "https://www.facebook.com/search/posts/?q=" + query.replace(" ", "%20");
            driver.get(searchUrl);
            sleep(5000);

            int targetPosts = 3; 
            int retryCount = 0;

            while (results.size() < targetPosts && retryCount < 20) {
                List<WebElement> buttons = driver.findElements(By.xpath("//div[@aria-label='Bình luận'] | //span[contains(text(), 'Bình luận')]"));
                WebElement targetBtn = null;

                for (WebElement btn : buttons) {
                    try {
                        if (btn.getDomAttribute("data-clicked") == null && btn.isDisplayed()) {
                            targetBtn = btn; break;
                        }
                    } catch (Exception e) {}
                }

                if (targetBtn == null) {
                    scrollDown(800);
                    retryCount++;
                    continue;
                }

                try {
                    js.executeScript("arguments[0].setAttribute('data-clicked', 'true');", targetBtn);
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", targetBtn);
                    sleep(1000);
                    js.executeScript("arguments[0].click();", targetBtn);
                    sleep(3000); 

                    if (!ensureSafePage(originalWindow)) continue;

                    WebElement container = getContainer(targetBtn);
                    String rawTextForStats = getTextViaJS(container);

                    // --- FIX UTF-8 ---
                    String cleanContent = fixUTF8(rawTextForStats);

                    // Debug log để check text trước khi preprocess / analyzer
                    System.out.println("=== RAW TEXT ===");
                    System.out.println(rawTextForStats);
                    System.out.println("=== FIXED UTF-8 TEXT ===");
                    System.out.println(cleanContent);

                    if (cleanContent.length() < 10) {
                        System.out.println("Nội dung quá ngắn -> Skip.");
                        continue;
                    }

                    SocialPostEntity post = new SocialPostEntity();
                    post.setContent(cleanContent);
                    post.setPlatform("Facebook");
                    post.setDisasterName(disasterName);

                    // --- xử lý comment ---
                    selectAllCommentsInContainer(container);
                    expandRepliesInContainer(container);

                    List<WebElement> cmts = container.findElements(By.xpath(".//div[@dir='auto'] | .//span[@lang]"));
                    for (WebElement cmt : cmts) {
                        try {
                            String t = fixUTF8(getTextViaJS(cmt)).trim();
                            if (t.isEmpty()) continue;
                            if (!isValidComment(t)) continue;
                            post.addComment(t);
                        } catch (StaleElementReferenceException ex) {}
                    }

                    post.removeDuplicateComments();
                    post.setCommentCount(post.getComments().size());

                    results.add(post);
                    System.out.println(" [LƯU " + results.size() + "/" + targetPosts + "] Len: " + cleanContent.length() + " | Cmt: " + post.getCommentCount());

                    scrollDown(700);
                    retryCount = 0;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    // --- FIX UTF-8 chuẩn ---
    private String fixUTF8(String s) {
        if (s == null) return null;
        // Facebook trả UTF-16 trong DOM, đọc Java String đã đúng, chỉ cần normalize
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
    }

    private String getTextViaJS(WebElement element) {
        try {
            String raw = (String) js.executeScript("return arguments[0].innerText;", element);
            return raw != null ? raw : "";
        } catch (Exception e) {
            return "";
        }
    }

    // --- Giữ nguyên các hàm helper như selectAllCommentsInContainer, expandRepliesInContainer, scrollDown, ensureSafePage, isValidComment ---
    // ... (copy từ code cũ)
    
    private WebElement getContainer(WebElement targetBtn) {
        WebElement container = null;
        try {
            List<WebElement> dialogs = driver.findElements(By.xpath("//div[@role='dialog']"));
            for (WebElement d : dialogs) {
                if (d.isDisplayed()) { container = d; break; }
            }
            if (container == null) container = targetBtn.findElement(By.xpath("./ancestor::div[@role='article']"));
        } catch (Exception e) {
            container = driver.findElement(By.tagName("body"));
        }
        return container;
    }

    
    private String simpleExtractContent(String fullText) {
        if (fullText == null) return "";
        
        String text = fullText.split("Tất cả bình luận")[0]
                              .split("Phù hợp nhất")[0]
                              .split("Most relevant")[0]
                              .split("Viết bình luận")[0];
        
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.matches(".*(Theo dõi|Tham gia|Follow|Sponsored|Được tài trợ|Thích|Trả lời|Chia sẻ|Có nội dung mới|Chưa đọc|Nhóm|Cộng đồng).*")) {
                continue;
            }
            // ko lấy thời gian
            if (line.matches("^\\d+\\s[h|m|d|giờ|phút|ngày].*")) {
                continue;
            }

            
            // dừng nếu gặp footer thống kê
            if (line.matches("^\\d.*(bình luận|lượt chia sẻ|comments|shares).*")) break;
            if (line.startsWith("Tất cả cảm xúc")) break;
            
            sb.append(line).append("\n");
        }
        
        return sb.toString().trim();
    }


    private void scrollModalDown(WebElement dialog) {
        try {
            js.executeScript(
                "var dialog = arguments[0];" +
                "var elements = dialog.querySelectorAll('*');" +
                "for (var i = 0; i < elements.length; i++) {" +
                "    var el = elements[i];" +
                "    if (el.scrollHeight > el.clientHeight && (window.getComputedStyle(el).overflowY === 'scroll' || window.getComputedStyle(el).overflowY === 'auto')) {" +
                "        el.scrollTop = el.scrollHeight;" +
                "    }" +
                "}", dialog);
        } catch (Exception e) {}
    }

    private void forceEscape(Actions actions) {
        try {
            actions.sendKeys(Keys.ESCAPE).perform();
            sleep(500);
            actions.sendKeys(Keys.ESCAPE).perform();
            sleep(2000);
        } catch (Exception e) {}
    }

    private void selectAllCommentsInContainer(WebElement container) {
        try {
            List<WebElement> filterBtns = container.findElements(By.xpath(".//span[contains(text(), 'Phù hợp nhất') or contains(text(), 'Most relevant') or contains(text(), 'Tất cả bình luận')]"));
            if (!filterBtns.isEmpty()) {
                WebElement btn = filterBtns.get(0);
                if (btn.getText().contains("Tất cả bình luận") || btn.getText().contains("All comments")) return;
                js.executeScript("arguments[0].click();", btn);
                sleep(2000); 
                List<WebElement> allOpts = driver.findElements(By.xpath("//span[contains(text(), 'Tất cả bình luận') or contains(text(), 'All comments')]"));
                for (WebElement opt : allOpts) {
                    if (opt.isDisplayed()) {
                        js.executeScript("arguments[0].click();", opt);
                        sleep(2500); 
                        break;
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void expandRepliesInContainer(WebElement container) {
        for(int i=0; i<3; i++) {
            try {
                List<WebElement> replyBtns = container.findElements(By.xpath(".//span[contains(text(), 'Xem') and contains(text(), 'phản hồi')]"));
                if(replyBtns.isEmpty()) break;
                for (WebElement btn : replyBtns) js.executeScript("arguments[0].click();", btn); 
                sleep(1000);
            } catch (Exception e) { break; }
        }
    }

    
    private boolean isValidComment(String text) {
        if (text.length() < 1) return false;
        String[] garbage = {"Xem thêm", "Viết bình luận", "Phản hồi", "Tất cả bình luận", "phù hợp nhất", "Thích", "Chia sẻ", "Gửi", "Theo dõi", "Tham gia", "Top comments", "Most relevant", "All comments", "Xem các bình luận trước", "Đang viết...", "Nhấn Enter để đăng", "Chỉnh sửa", "Xóa", "Ẩn", "Dịch", "Xem bản dịch", "Đoạn chat", "Cộng đồng", "Marketplace", "Có nội dung mới", "Chưa đọc"};
        for (String kw : garbage) if (text.equalsIgnoreCase(kw)) return false;
        if (text.matches("^\\d+\\s*(giờ|phút|ngày|tuần|năm|h|m|d|y).*$")) return false; 
        if (text.matches(".*([a-zA-Z0-9|]\\s+){3,}.*")) return false;
        return true;
    }

    private int parseReactionCount(String fullText) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("Tất cả cảm xúc:?\\s*([\\d.,]+[KkMm]?)");
            java.util.regex.Matcher m = p.matcher(fullText);
            if (m.find()) return parseNumber(m.group(1));
        } catch (Exception e) {}
        return 0;
    }

    private int parseStrictShareCount(String text) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+[.,]?\\d*[KkMm]?)\\s+(lượt chia sẻ|shares)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return parseNumber(m.group(1));
        } catch (Exception e) {}
        return 0;
    }

    private int parseNumber(String text) {
        if(text == null) return 0;
        int multiplier = 1;
        if (text.toUpperCase().contains("K")) multiplier = 1000;
        if (text.toUpperCase().contains("M")) multiplier = 1000000;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+([.,]\\d+)?)");
        java.util.regex.Matcher m = p.matcher(text.replace(",", "."));
        if (m.find()) return (int) (Double.parseDouble(m.group(1)) * multiplier);
        return 0;
    }
    private boolean ensureSafePage(String originalWindowHandle) {
        try {
            Set<String> handles = driver.getWindowHandles();
            if (handles.size() > 1) {
                for (String handle : handles) {
                    if (!handle.equals(originalWindowHandle)) {
                        driver.switchTo().window(handle);
                        System.out.println("Phát hiện Tab lạ -> Đóng Tab.");
                        driver.close();
                    }
                }
                driver.switchTo().window(originalWindowHandle); 
            }

            String currentUrl = driver.getCurrentUrl();
            if (!currentUrl.contains("facebook.com")) {
                System.out.println("Đang ở: " + currentUrl + " -> Bấm Back!");
                driver.navigate().back(); 
                sleep(3000);
                return false;
            }
            return true; 
        } catch (Exception e) {
            return false;
        }
    }
}