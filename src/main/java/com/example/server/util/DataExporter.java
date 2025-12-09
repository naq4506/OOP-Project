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
            
            
            writer.write("STT,NgayDang,Nội Dung,ReactTong,Share,CommentCount,Like,Love,Haha,Wow,Sad,Angry,Comments\n");
            
            int stt = 1;
            for (SocialPostEntity p : posts) {
                String content = p.getContent() != null ? p.getContent().replace("\"", "\"\"").replace("\n", " ") : "";
                String cmts = p.getCommentsAsCsvString().replace("\"", "\"\"");
                
                String dateStr = p.getPostDate() != null ? p.getPostDate().toLocalDate().toString() : ""; 
                
                
                writer.write(String.format("%d,%s,\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,\"%s\"\n", 
                        stt++, 
                        dateStr,
                        content, 
                        p.getTotalReactions(), 
                        p.getShareCount(), 
                        p.getCommentCount(),
                        p.getReactionLike(),
                        p.getReactionLove(),
                        p.getReactionHaha(),
                        p.getReactionWow(),
                        p.getReactionSad(),
                        p.getReactionAngry(),
                        cmts));
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
                writer.write("BÀI VIẾT #" + idx + " (Ngày: " + (p.getPostDate() != null ? p.getPostDate().toLocalDate().toString() : "N/A") + "):\n");
                
                writer.write(p.getContent() + "\n"); 
                
                writer.write("\n[TỔNG QUAN] React: " + p.getTotalReactions() + ", Comments: " + p.getCommentCount() + ", Shares: " + p.getShareCount() + "\n");
                writer.write("[REACTIONS CHI TIẾT] Like: " + p.getReactionLike() +
                             ", Sad: " + p.getReactionSad()  + "\n");

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