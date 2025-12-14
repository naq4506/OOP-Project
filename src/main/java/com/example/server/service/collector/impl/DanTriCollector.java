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

public class DanTriCollector extends BaseSeleniumCollector {

    private static final String BASE_URL = "https://dantri.com.vn/tim-kiem"; 
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public List<SocialPostEntity> collect(String disasterName, String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        List<SocialPostEntity> results = new ArrayList<>();
        initDriver();

        try {
            // 1. Tạo và truy cập link tìm kiếm
            String searchPhrase = (keyword + " " + disasterName).trim();
            String encodedKeyword = URLEncoder.encode(searchPhrase, StandardCharsets.UTF_8);
            String url = BASE_URL + "/" + encodedKeyword + ".htm";

            System.out.println(">>> [DanTri] Link tìm kiếm: " + url);
            driver.get(url);
            sleep(3000); 

            // 2. Lấy danh sách link (Chỉ lấy link text string, không giữ Element để tránh lỗi Stale)
            List<String> articleLinks = new ArrayList<>();
            List<WebElement> titleElements = driver.findElements(By.cssSelector("h3.article-title a, h3.news-item__title a"));

            for (WebElement el : titleElements) {
                String link = el.getAttribute("href");
                if (link != null && !link.isEmpty() && link.contains("dantri.com.vn") && !articleLinks.contains(link)) {
                    articleLinks.add(link);
                }
                // Giới hạn số lượng bài để test nếu cần
                if (articleLinks.size() >= 20) break; 
            }

            System.out.println(">>> [DanTri] Tìm thấy " + articleLinks.size() + " bài viết. Bắt đầu xử lý từng tab...");

            // Lưu lại ID của tab gốc (Tab danh sách tìm kiếm)
            String originalTab = driver.getWindowHandle();

            // 3. Duyệt từng link theo cơ chế Tab Mới
            for (String link : articleLinks) {
                try {
                    // Mở tab mới
                    driver.switchTo().newWindow(WindowType.TAB);
                    // Truy cập bài viết
                    driver.get(link);
                    
                    // Parse dữ liệu
                    SocialPostEntity post = parseArticle(link, disasterName);

                    // Kiểm tra và lưu kết quả
                    if (post != null && post.getPostDate() != null) {
                        if (!post.getPostDate().isBefore(startDate) && !post.getPostDate().isAfter(endDate)) {
                            results.add(post);
                            System.out.println(" [+] Đã lấy: " + (post.getContent().length() > 40 ? post.getContent().substring(0, 40) + "..." : post.getContent()));
                        } else {
                            // System.out.println(" [-] Skip: Ngoài khoảng thời gian");
                        }
                    }

                    // Nghỉ xíu cho đỡ bị chặn
                    sleep(1000);

                } catch (Exception e) {
                    System.err.println(" (!) Lỗi xử lý link: " + link);
                    e.printStackTrace();
                } finally {
                    // QUAN TRỌNG: Đóng tab hiện tại (bài viết) và quay về tab gốc
                    try {
                        driver.close(); // Đóng tab bài viết
                        driver.switchTo().window(originalTab); // Quay về tab tìm kiếm
                    } catch (Exception ex) {
                        System.err.println("Lỗi đóng tab, force break để tránh treo.");
                        break;
                    }
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
        post.setPlatform("Dan Tri");
        post.setDisasterName(disasterName);
        post.setReactionLike(0);
        post.setCommentCount(0);
        post.setShareCount(0);

        try {
            // Lấy Tiêu đề
            String title = "";
            try {
                title = driver.findElement(By.cssSelector("h1.title-page, h1.e-magazine__title")).getText();
            } catch (Exception e) { 
                // Không lấy được tiêu đề coi như lỗi layout -> return null để skip
                return null; 
            } 

            // Lấy Ngày đăng
            try {
                WebElement dateEl = driver.findElement(By.cssSelector(".author-time, .date"));
                post.setPostDate(parseVietnameseDate(dateEl.getText()));
            } catch (Exception e) {
                post.setPostDate(LocalDateTime.now());
            }

            // Lấy Nội dung
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(title).append("\n\n");

            try {
                WebElement sapoEl = driver.findElement(By.cssSelector(".singular-sapo"));
                contentBuilder.append(sapoEl.getText()).append("\n");
            } catch (Exception e) {}

            try {
                WebElement bodyEl = driver.findElement(By.cssSelector(".singular-content, .e-magazine__body"));
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
            Pattern p = Pattern.compile("(\\d{2}/\\d{2}/\\d{4}\\s*-\\s*\\d{2}:\\d{2})|(\\d{2}/\\d{2}/\\d{4}\\s*\\d{2}:\\d{2})");
            Matcher m = p.matcher(dateText);
            if (m.find()) {
                String cleanDate = m.group(0).replace("-", "").replace("  ", " ").trim();
                return LocalDateTime.parse(cleanDate, DATE_FORMATTER);
            }
        } catch (Exception e) { }
        return LocalDateTime.now();
    }
}