package com.example.server;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.CollectorFactory;
import com.example.server.util.DataExporter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunCrawler {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   DEMO CRAWLER: CÀO ĐA TỪ KHÓA (10 BÀI)");
        System.out.println("==========================================\n");

        try {
            Collector bot = CollectorFactory.getCollector("facebook");
            
            List<String> keywords = Arrays.asList(
                "bão yagi thiệt hại về người",
                "bão yagi thiệt hại cơ sở vật chất",
                "bão yagi cứu trợ",
                "bão yagi cường độ",
                "bão yagi số người bị ảnh hưởng"
            );

            List<SocialPostEntity> allResults = new ArrayList<>();
            
            for (String kw : keywords) {
                System.out.println("\n--------------------------------------------------");
                System.out.println(">>> BẮT ĐẦU TỪ KHÓA: " + kw.toUpperCase());
                System.out.println("--------------------------------------------------");
                
                List<SocialPostEntity> batch = bot.collect(
                    "Bão Yagi", 
                    kw, 
                    LocalDate.of(2024, 9, 1), 
                    LocalDate.of(2024, 9, 30)
                );
                
                allResults.addAll(batch);
                
                System.out.println(">>> Đã xong từ khóa '" + kw + "'. Tổng hiện tại: " + allResults.size() + " bài.");
                
                Thread.sleep(3000);
            }

            List<SocialPostEntity> finalUniqueResults = deduplicate(allResults);

            if (!finalUniqueResults.isEmpty()) {
                System.out.println("\nDonee! Tổng thu được: " + finalUniqueResults.size() + " bài viết duy nhất.");
                
                DataExporter.saveToCsv(finalUniqueResults, "data/ket_qua_tong_hop_10_bai.csv");
                DataExporter.saveToTxtReport(finalUniqueResults, "data/bao_cao_tong_hop_10_bai.txt");
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