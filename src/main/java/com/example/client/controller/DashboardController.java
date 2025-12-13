package com.example.client.controller;

import com.example.server.model.SocialPostEntity;
import com.example.server.service.collector.Collector;
import com.example.server.service.collector.CollectorFactory;
import com.example.server.util.DataExporter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardController {

    @FXML private TextField txtDisasterName;
    @FXML private TextField txtKeyword;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    
    // Thay CheckBox bằng ToggleGroup cho RadioButton
    @FXML private ToggleGroup platformGroup;
    
    @FXML private Label statusLabel;
    @FXML private Button btnStart;

    @FXML
    protected void onStartCrawl() {
        // 1. Lấy dữ liệu từ Form
        String disasterName = txtDisasterName.getText().trim();
        String keyword = txtKeyword.getText().trim();
        LocalDate localStartDate = dpStartDate.getValue();
        LocalDate localEndDate = dpEndDate.getValue();

        // 2. Validate cơ bản
        if (disasterName.isEmpty() || keyword.isEmpty() || localStartDate == null || localEndDate == null) {
            showAlert("Thiếu thông tin", "Vui lòng nhập đầy đủ tên thảm họa, từ khóa và ngày tháng!");
            return;
        }

        // 3. Lấy Platform được chọn từ RadioButton
        RadioButton selectedRadio = (RadioButton) platformGroup.getSelectedToggle();
        if (selectedRadio == null) {
            showAlert("Chưa chọn nền tảng", "Vui lòng chọn một nền tảng để quét!");
            return;
        }
        String platformLabel = selectedRadio.getText(); // Lấy text hiển thị (VD: "X (Twitter)")
        
        // Map text hiển thị sang code mà Factory hiểu (facebook, x, instagram, mock)
        String platformCode = getPlatformCode(platformLabel);

        // 4. Chuyển đổi ngày sang LocalDateTime (Backend cần cái này)
        LocalDateTime startDate = localStartDate.atStartOfDay();
        LocalDateTime endDate = localEndDate.atTime(23, 59, 59);

        // 5. Cập nhật giao diện
        btnStart.setDisable(true); // Khóa nút để tránh bấm nhiều lần
        statusLabel.setText("Đang khởi động crawler cho " + platformLabel + "...");

        // 6. Chạy Crawler trong luồng riêng (Thread) để không bị đơ giao diện
        new Thread(() -> {
            try {
                // --- LOGIC GỌI CRAWLER (Giống hệt RunCrawler) ---
                Collector bot = CollectorFactory.getCollector(platformCode);
                
                if (bot == null) {
                    updateStatus("Lỗi: Chưa hỗ trợ nền tảng " + platformCode);
                    return;
                }

                updateStatus("Đang quét dữ liệu... (Vui lòng chờ)");
                
                List<SocialPostEntity> results = bot.collect(
                    disasterName, 
                    keyword, 
                    startDate, 
                    endDate
                );

                // Lưu file
                if (!results.isEmpty()) {
                    String fileName = "data/" + platformCode + "_data";
                    DataExporter.saveToCsv(results, fileName + ".csv");
                    DataExporter.saveToTxtReport(results, fileName + ".txt");
                    
                    updateStatus("✅ Hoàn tất! Lấy được " + results.size() + " bài. Đã lưu file.");
                } else {
                    updateStatus("⚠️ Hoàn tất nhưng không tìm thấy bài viết nào.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                updateStatus("❌ Lỗi Crawler: " + e.getMessage());
            } finally {
                // Mở lại nút bấm
                Platform.runLater(() -> btnStart.setDisable(false));
            }
        }).start();
    }

    // Hàm chuyển đổi Text trên giao diện thành Code cho Factory
    private String getPlatformCode(String label) {
        if (label.contains("Facebook")) return "facebook";
        if (label.contains("X") || label.contains("Twitter")) return "x";
        if (label.contains("Instagram")) return "instagram";
        return "mock"; // Mặc định là test
    }

    // Hàm cập nhật Label an toàn từ luồng khác
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private String currentAnalysisType;

    @FXML private Label titleLabel; // Ông nhớ đặt fx:id="titleLabel" cho cái Label to đùng ở file dashboard.fxml nhé

    public void setAnalysisType(String type) {
        this.currentAnalysisType = type;
        System.out.println("Đã chọn chế độ phân tích: " + type);
    }

    public void setDashboardTitle(String title) {
        // Nếu trong fxml chưa có fx:id thì bỏ dòng này đi, còn nếu có thì set text cho đẹp
        // if (titleLabel != null) titleLabel.setText(title); 
    }
}