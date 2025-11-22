package com.example.server.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TestCrawler {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");

        System.out.println(">>> [INIT] Đang kết nối vào Chrome Bot...");
        
        try {
            WebDriver driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            
            System.out.println(">>> [SUCCESS] Đã kết nối thành công!");

            String searchUrl = "https://www.facebook.com/search/posts/?q=bao+yagi";
            
            System.out.println(">>> [NAVIGATE] Đang chuyển hướng sang trang tìm kiếm 'Bão Yagi'...");
            driver.get(searchUrl);
            
            System.out.println(">>> Đang chờ tải kết quả...");
            Thread.sleep(5000); 

            
            System.out.println(">>> [ACTION] Bắt đầu thu thập dữ liệu...");

            Set<String> collectedPosts = new LinkedHashSet<>();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            int targetPosts = 20; 
            int maxAttempts = 50; 
            int attempts = 0;

            while (collectedPosts.size() < targetPosts && attempts < maxAttempts) {
                attempts++;
                System.out.print("\r>>> Quét lần " + attempts + " | Đã thu được: " + collectedPosts.size() + "/" + targetPosts + " bài...");

                try {
                    List<WebElement> seeMoreButtons = driver.findElements(By.xpath("//div[text()='Xem thêm'] | //span[text()='Xem thêm']"));
                    for (WebElement btn : seeMoreButtons) {
                        try { js.executeScript("arguments[0].click();", btn); } catch (Exception e) {}
                    }
                    Thread.sleep(500);
                } catch (Exception e) {}

                try {
                    String fullText = "";
                    try {
                        fullText = driver.findElement(By.xpath("//div[@role='main']")).getText();
                    } catch (Exception e) {
                        fullText = driver.findElement(By.tagName("body")).getText();
                    }

                    String[] rawParts = fullText.split("Chia sẻ");

                    for (String part : rawParts) {
                        String cleanPart = formatText(part);
                        if (cleanPart.length() > 50) {
                            collectedPosts.add(cleanPart);
                        }
                    }
                } catch (Exception e) {}

                js.executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(2000);
            }

            System.out.println("\n\n>>> [SAVING] Đang lưu dữ liệu ra file...");
            
            String folderName = "data";
            File directory = new File(folderName);
            if (!directory.exists()){
                directory.mkdir();
            }

            String fileName = folderName + "/data_bao_yagi.csv";
            
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
                
                writer.write('\ufeff'); 
                writer.write("STT,Nội Dung Bài Viết\n");

                int count = 0;
                for (String post : collectedPosts) {
                    count++;
                    String csvSafeContent = post.replace("\"", "\"\""); 
                    writer.write(count + ",\"" + csvSafeContent + "\"\n");
                    
                    System.out.println("--- Đã lưu bài " + count + " ---");
                    if (count >= targetPosts) break;
                }
                
                System.out.println("==================================================");
                System.out.println(">>> [DONE] ĐÃ LƯU THÀNH CÔNG FILE: " + fileName);
                System.out.println("==================================================");
                
            } catch (Exception e) {
                System.err.println(">>> LỖI KHI GHI FILE: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatText(String raw) {
        if (raw == null) return "";
        String text = raw.replace("\n", " | ");
        text = text.replaceAll("\\s([a-z0-9]\\s){3,}", " "); 
        text = text.replace("Thích | Bình luận", "")
                   .replace("Tất cả cảm xúc:", "")
                   .replace("Viết bình luận...", "")
                   .replace("| Theo dõi", "")
                   .replace("Đang hoạt động", "")
                   .replace("Xem thêm", "");
        return text.trim().replaceAll("\\s+", " ");
    }
}