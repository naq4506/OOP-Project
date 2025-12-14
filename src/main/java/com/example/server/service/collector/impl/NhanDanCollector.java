package com.example.server.service.collector.impl;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.BaseSeleniumCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NhanDanCollector extends BaseSeleniumCollector {

    private static final String SEARCH_URL = "https://nhandan.vn/tim-kiem";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            // 1. Tạo URL tìm kiếm
            String searchPhrase = (keyword + " " + disasterName).trim();
            // Encode URL cẩn thận
            String encodedKeyword = URLEncoder.encode(searchPhrase, StandardCharsets.UTF_8);
            String url = SEARCH_URL + "/?q=" + encodedKeyword;

            System.out.println(">>> [NhanDan] Link tìm kiếm: " + url);
            driver.get(url);
            
            // Tăng thời gian chờ load trang một chút vì có popup login (như trong ảnh bạn gửi)
            sleep(4000); 

            // 2. Lấy danh sách link bài viết (SỬA SELECTOR TỐI ƯU HƠN)
            List<String> articleLinks = new ArrayList<>();
            
            // Selector mới: Bắt trực tiếp thẻ h3 tiêu đề, không quan tâm cha nó là div hay article
            List<WebElement> titleElements = driver.findElements(By.cssSelector("h3.story__heading a, .box-search-result .story__heading a"));

            for (WebElement el : titleElements) {
                try {
                    String link = el.getAttribute("href");
                    if (link != null && !link.isEmpty() && link.contains("nhandan.vn") && !articleLinks.contains(link)) {
                        articleLinks.add(link);
                    }
                } catch (Exception e) {
                    // Bỏ qua lỗi nhỏ khi getAttribute
                }
                if (articleLinks.size() >= 20) break; 
            }

            System.out.println(">>> [NhanDan] Tìm thấy " + articleLinks.size() + " bài viết. Bắt đầu xử lý...");

            if (articleLinks.isEmpty()) {
                System.out.println("!!! CẢNH BÁO: Không lấy được link nào. Có thể do Selector hoặc Web chưa load xong.");
                // In thử tiêu đề trang để debug xem có bị chặn không
                System.out.println("Page Title hiện tại: " + driver.getTitle());
            }

            String originalTab = driver.getWindowHandle();

            // 3. Xử lý từng bài (Logic Tab Mới)
            for (String link : articleLinks) {
                try {
                    driver.switchTo().newWindow(WindowType.TAB);
                    driver.get(link);
                    
                    SocialPostEntity post = parseArticle(link, disasterName);

                    if (post != null && post.getPostDate() != null) {
                        // Logic check ngày
                        boolean isAfterStart = !post.getPostDate().isBefore(startDate);
                        boolean isBeforeEnd = !post.getPostDate().isAfter(endDate);

                        if (isAfterStart && isBeforeEnd) {
                            results.add(post);
                            System.out.println(" [+] Đã lấy: " + (post.getContent().length() > 40 ? post.getContent().substring(0, 40) + "..." : post.getContent()));
                        }
                    }
                    
                    // Random sleep để giả lập người dùng đọc bài
                    sleep(1500); 

                } catch (Exception e) {
                    System.err.println(" (!) Lỗi xử lý link: " + link);
                } finally {
                    try {
                        driver.close();
                        driver.switchTo().window(originalTab);
                    } catch (Exception ex) { break; }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDriver();
        }

        return results;
    }

    private SocialPostEntity parseArticle(String url, String disasterName) {
        SocialPostEntity post = new SocialPostEntity();
        post.setPlatform("Nhan Dan");
        post.setDisasterName(disasterName);
        post.setReactionLike(0);
        post.setCommentCount(0);
        post.setShareCount(0);

        try {
            // --- Lấy Tiêu đề ---
            String title = "";
            try {
                // Thử nhiều selector cho chắc
                List<WebElement> h1s = driver.findElements(By.cssSelector("h1.article__title, h1.story__heading, h1.title-detail"));
                if (!h1s.isEmpty()) title = h1s.get(0).getText();
            } catch (Exception e) {}

            // --- Lấy Ngày đăng ---
            try {
                WebElement dateEl = driver.findElement(By.cssSelector(".box-date, .article__meta time, .author-time, .story__meta .time"));
                post.setPostDate(parseVietnameseDate(dateEl.getText()));
            } catch (Exception e) {
                post.setPostDate(LocalDateTime.now());
            }

            // --- Lấy Nội dung ---
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(title).append("\n\n");

            try {
                // Sapo
                WebElement sapoEl = driver.findElement(By.cssSelector(".article__sapo, .sapo, .story__summary"));
                contentBuilder.append(sapoEl.getText()).append("\n");
            } catch (Exception e) {}

            try {
                // Body
                WebElement bodyEl = driver.findElement(By.cssSelector(".detail-content-body, .article__body, .content-detail"));
                contentBuilder.append(bodyEl.getText());
            } catch (Exception e) {}

            post.setContent(contentBuilder.toString());
            return post;

        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseVietnameseDate(String dateText) {
        try {
            // Regex bắt ngày giờ: dd/MM/yyyy HH:mm
            // Nó sẽ bỏ qua chữ "Thứ ba, ngày..."
            Pattern p = Pattern.compile("(\\d{2}/\\d{2}/\\d{4}.*?\\d{2}:\\d{2})");
            Matcher m = p.matcher(dateText);
            if (m.find()) {
                String rawDate = m.group(1);
                String cleanDate = rawDate.replace("-", "").replace("ngày", "").replace("  ", " ").trim();
                return LocalDateTime.parse(cleanDate, DATE_FORMATTER);
            }
        } catch (Exception e) { }
        return LocalDateTime.now();
    }
}