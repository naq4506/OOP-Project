package com.example.server;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.CollectorFactory;
import com.example.server.util.DataExporter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RunCrawler {

    public static void main(String[] args) {
        System.out.println("================= START X CRAWLER ===================");

        final String DISASTER_NAME = " ";
        final String SINGLE_KEYWORD = "typhoon yagi"; 
        
        final LocalDateTime START_DATE = LocalDate.of(2024, 1, 1).atStartOfDay(); 
        final LocalDateTime END_DATE = LocalDateTime.now();  
        
        try {
            Collector bot = CollectorFactory.getCollector("Reuters");
            
            if (bot == null) {
                System.err.println("Chưa config bot 'instagram' trong Factory!");
                return;
            }

            List<SocialPostEntity> results = bot.collect(
                DISASTER_NAME, 
                SINGLE_KEYWORD, 
                START_DATE,
                END_DATE    
            );
            
            System.out.println(">>> Tổng bài lấy được: " + results.size());
            
            if (!results.isEmpty()) {
                DataExporter.saveToCsv(results, "data/Reuters_data.csv");
                DataExporter.saveToTxtReport(results, "data/Reuters_data.txt");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}