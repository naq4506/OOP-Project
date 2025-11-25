package com.example.server.util;

import com.example.server.model.SocialPostEntity;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class DataExporter {

    public static void saveToCsv(List<SocialPostEntity> posts, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.write("STT,Nội Dung,React,CommentCount,Share,Comments\n");
            int stt = 1;
            for (SocialPostEntity p : posts) {
                String content = p.getContent() != null ? p.getContent().replace("\"", "\"\"").replace("\n", " ") : "";
                String cmts = p.getCommentsAsCsvString().replace("\"", "\"\"");
                writer.write(String.format("%d,\"%s\",%d,%d,%d,\"%s\"\n", 
                        stt++, content, p.getTotalReactions(), p.getCommentCount(), p.getShareCount(), cmts));
            }
            System.out.println(">>> [Exporter] Đã lưu CSV: " + filePath);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveToTxtReport(List<SocialPostEntity> posts, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write("=== BÁO CÁO DỮ LIỆU CHI TIẾT (FULL) ===\n");
            writer.write("Ngày xuất: " + LocalDate.now() + "\n");
            writer.write("Tổng số bài: " + posts.size() + "\n\n");
            
            int idx = 1;
            for (SocialPostEntity p : posts) {
                writer.write("--------------------------------------------------\n");
                writer.write("BÀI VIẾT #" + idx + ":\n");
                
                writer.write(p.getContent() + "\n"); 
                
                writer.write("\n[THỐNG KÊ] " + p.getTotalReactions() + " react, " + p.getCommentCount() + " comments, " + p.getShareCount() + " shares\n");
                writer.write("[DANH SÁCH COMMENT]:\n");
                
                int cmtIdx = 1;
                if (p.getComments().isEmpty()) {
                    writer.write("   (Không có comment)\n");
                } else {
                    for (String cmt : p.getComments()) {
                        writer.write("   " + cmtIdx++ + ". " + cmt.replace("\n", " ") + "\n");
                    }
                }
                writer.write("\n");
                idx++;
            }
            System.out.println(">>> [Exporter] Đã lưu Report: " + filePath);
        } catch (Exception e) { e.printStackTrace(); }
    }
}