package com.example.server;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.CollectorFactory;
import com.example.server.util.DataExporter;

import java.time.LocalDate; // Giữ lại nếu cần, nhưng nên dùng LocalDateTime cho thống nhất
import java.time.LocalDateTime; // THÊM IMPORT NÀY
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunCrawler {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("                   Result"); 
        System.out.println("==========================================\n");

        final String DISASTER_NAME = "Bão Yagi";
        final String SINGLE_KEYWORD = "bão yagi";
        
        final LocalDateTime START_DATE = LocalDate.of(2024, 9, 1).atStartOfDay(); 
        final LocalDateTime END_DATE = LocalDate.of(2024, 9, 30).atStartOfDay();  
        
        try {
            Collector bot = CollectorFactory.getCollector("facebook");
            
            System.out.println("\n--------------------------------------------------");
            System.out.println(">>> BẮT ĐẦU TỪ KHÓA: " + SINGLE_KEYWORD.toUpperCase());
            System.out.println("--------------------------------------------------");
            
            List<SocialPostEntity> allResults = bot.collect(
                DISASTER_NAME, 
                SINGLE_KEYWORD, 
                START_DATE,
                END_DATE    
            );
            
            System.out.println(">>> Đã xong từ khóa '" + SINGLE_KEYWORD + "'. Tổng thu thập: " + allResults.size() + " bài.");
            
            List<SocialPostEntity> finalUniqueResults = deduplicate(allResults);

            if (!finalUniqueResults.isEmpty()) {
                System.out.println("\nDonee! Tổng thu được: " + finalUniqueResults.size() + " bài viết duy nhất.");
                
                DataExporter.saveToCsv(finalUniqueResults, "data/data_final.csv");
                DataExporter.saveToTxtReport(finalUniqueResults, "data/data_final.txt");
            } else {
                System.out.println("\n Không tìm thấy bài viết nào.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n>>> Chương trình kết thúc.");
        }
    }
    
    private static List<SocialPostEntity> deduplicate(List<SocialPostEntity> list) {
        List<SocialPostEntity> uniqueList = new ArrayList<>();
        Set<String> contentHashes = new HashSet<>();
        
        for (SocialPostEntity p : list) {
            String signature = (p.getContent().length() > 50) ? p.getContent().substring(0, 50) : p.getContent();
            
            if (!contentHashes.contains(signature)) {
                contentHashes.add(signature);
                uniqueList.add(p);
            }
        }
        return uniqueList;
    }
}