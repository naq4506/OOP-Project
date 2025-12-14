package com.example.client.controller;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    // --- FXML UI Elements ---
    @FXML private TextField txtDisasterName;
    @FXML private TextField txtKeyword;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ToggleGroup platformGroup;
    @FXML private Label statusLabel;
    @FXML private Button btnStart;
    @FXML private Label titleLabel; 
    @FXML private VBox chartContainer;

    // --- Dữ liệu và Service ---
    private final ClientController clientController = new ClientController("http://localhost:8080"); 
    private String currentAnalysisType;

    public void setAnalysisType(String type) {
        this.currentAnalysisType = type;
        setDashboardTitle(mapAnalysisTypeToTitle(type));
    }
    
    public void setDashboardTitle(String title) {
        if (titleLabel != null) titleLabel.setText(title);
    }
    
    private String mapAnalysisTypeToTitle(String type) {
        return switch (type) {
            case "SENTIMENT" -> "Theo dõi Tâm lý Công chúng theo Thời gian";
            case "DAMAGE" -> "Đánh giá Thiệt hại Phổ biến";
            case "RELIEF" -> "Đánh giá Hài lòng Cứu trợ (Tổng quan)";
            case "RELIEF_TIMELINE" -> "Phân tích Hàng cứu trợ theo Thời gian";
            default -> "Dashboard Phân tích Tổng hợp";
        };
    }
    
    private String getPlatformCode(String label) {
        if (label.contains("Facebook")) return "facebook";
        if (label.contains("X") || label.contains("Twitter")) return "x";
        if (label.contains("Instagram")) return "instagram";
        return "mock";
    }

    @FXML
    protected void onStartCrawl() {
        String disasterName = txtDisasterName.getText().trim();
        String keyword = txtKeyword.getText().trim();
        LocalDate localStartDate = dpStartDate.getValue();
        LocalDate localEndDate = dpEndDate.getValue();

        if (disasterName.isEmpty() || localStartDate == null || localEndDate == null) {
            showAlert("Thiếu thông tin", "Vui lòng nhập Tên thảm họa và chọn Khoảng thời gian!");
            return;
        }
        
        RadioButton selectedRadio = (RadioButton) platformGroup.getSelectedToggle();
        String platformCode = getPlatformCode(selectedRadio != null ? selectedRadio.getText() : "mock");

        if (currentAnalysisType == null || currentAnalysisType.isEmpty()) {
             showAlert("Lỗi", "Không xác định được loại phân tích.");
             return;
        }

        ClientRequest request = new ClientRequest();
        request.setDisasterName(disasterName);
        request.setKeyword(keyword);
        request.setStartDate(localStartDate.toString());
        request.setEndDate(localEndDate.toString());
        request.setPlatforms(java.util.Arrays.asList(platformCode)); 
        request.setAnalysisType(currentAnalysisType);

        btnStart.setDisable(true);
        statusLabel.setText("Đang gửi yêu cầu thu thập và phân tích tới Server...");
        chartContainer.getChildren().clear(); // Xóa biểu đồ cũ

        new Thread(() -> {
            try {
                ClientResponse<Map<String, Object>> response = clientController.sendAnalysis(request);
                
                if (response.isSuccess()) {
                    updateStatus("✅ Phân tích hoàn tất. Đang hiển thị kết quả...");
                    final Map<String, Object> results = response.getData();
                    Platform.runLater(() -> showAnalysisResults(results, currentAnalysisType));
                } else {
                    updateStatus("❌ Lỗi Server: " + response.getErrorMessage());
                }

            } catch (Exception e) {
                e.printStackTrace();
                updateStatus("❌ Lỗi kết nối Client: " + e.getMessage());
            } finally {
                Platform.runLater(() -> btnStart.setDisable(false));
            }
        }).start();
    }
    
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // =========================================================
    // XỬ LÝ HIỂN THỊ KẾT QUẢ & CHECK LỖI KHÔNG TÌM THẤY BÀI
    // =========================================================

    private void showAnalysisResults(Map<String, Object> allResults, String analysisType) {
        chartContainer.getChildren().clear();

        // 1. Check nếu Server trả về null
        if (allResults == null || allResults.isEmpty()) {
            showEmptyDataMessage("Server trả về dữ liệu rỗng.");
            return;
        }

        // 2. Lấy kết quả cụ thể
        Object rawResult = allResults.get(analysisType.toUpperCase());

        // 3. Check nếu bài toán cụ thể không có dữ liệu
        if (rawResult == null) {
            showEmptyDataMessage("Không tìm thấy dữ liệu cho bài toán: " + analysisType);
            return;
        }
        
        if (rawResult instanceof Map && ((Map<?,?>)rawResult).isEmpty()) {
            showEmptyDataMessage("Rất tiếc! Không tìm thấy bài viết nào phù hợp với từ khóa và thời gian bạn chọn.\n\nHãy thử lại với từ khóa phổ biến hơn hoặc khoảng thời gian rộng hơn.");
            return;
        }

        // 4. Nếu có dữ liệu -> Vẽ biểu đồ
        try {
            switch (analysisType.toUpperCase()) {
                case "SENTIMENT":
                    drawLineChartSentiment((Map<String, Map<String, Integer>>) rawResult);
                    break;
                case "DAMAGE":
                    drawBarChartDamage((Map<String, Integer>) rawResult);
                    break;
                case "RELIEF":
                    drawStackedBarChartRelief((Map<String, Map<String, Integer>>) rawResult);
                    break;
                case "RELIEF_TIMELINE":
                    drawLineChartReliefTimeline((Map<String, Map<String, Map<String, Integer>>>) rawResult);
                    break;
            }
        } catch (Exception e) {
             showAlert("Lỗi Vẽ biểu đồ", "Dữ liệu server trả về không đúng định dạng: " + e.getMessage());
             e.printStackTrace();
        }
    }

    private void showEmptyDataMessage(String message) {
        Label icon = new Label("❌");
        icon.setFont(Font.font("System", 40));
        
        Label msg = new Label(message);
        msg.setFont(Font.font("System", FontWeight.BOLD, 16));
        msg.setStyle("-fx-text-fill: #e74c3c;");
        msg.setWrapText(true);
        msg.setAlignment(Pos.CENTER);
        
        VBox emptyBox = new VBox(10, icon, msg);
        emptyBox.setAlignment(Pos.CENTER);
        
        chartContainer.getChildren().add(emptyBox);
        updateStatus("⚠️ Hoàn tất nhưng không có dữ liệu.");
    }

    // --- CÁC HÀM VẼ BIỂU ĐỒ ---

    private void drawBarChartDamage(Map<String, Integer> data) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Thống kê loại thiệt hại");
        xAxis.setLabel("Loại Thiệt hại");
        yAxis.setLabel("Số lượng");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bài đăng");
        data.forEach((type, count) -> series.getData().add(new XYChart.Data<>(type, count)));

        barChart.getData().add(series);
        chartContainer.getChildren().add(barChart);
    }
    
    private void drawLineChartSentiment(Map<String, Map<String, Integer>> timelineData) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Diễn biến tâm lý");
        xAxis.setLabel("Ngày");
        yAxis.setLabel("Số lượng");
        
        List<String> sortedDates = new ArrayList<>(timelineData.keySet());
        Collections.sort(sortedDates);
        
        XYChart.Series<String, Number> pos = new XYChart.Series<>(); pos.setName("Tích cực");
        XYChart.Series<String, Number> neg = new XYChart.Series<>(); neg.setName("Tiêu cực");

        for (String d : sortedDates) {
            Map<String, Integer> counts = timelineData.get(d);
            String dateLabel = d.length() > 5 ? d.substring(5) : d;
            pos.getData().add(new XYChart.Data<>(dateLabel, counts.getOrDefault("positive", 0)));
            neg.getData().add(new XYChart.Data<>(dateLabel, counts.getOrDefault("negative", 0)));
        }
        lineChart.getData().addAll(pos, neg);
        chartContainer.getChildren().add(lineChart);
    }

    private void drawStackedBarChartRelief(Map<String, Map<String, Integer>> reliefData) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        StackedBarChart<String, Number> barChart = new StackedBarChart<>(xAxis, yAxis);
        barChart.setTitle("Mức độ hài lòng về cứu trợ");
        xAxis.setLabel("Hạng mục");
        yAxis.setLabel("Số lượng");

        XYChart.Series<String, Number> pos = new XYChart.Series<>(); pos.setName("Tích cực");
        XYChart.Series<String, Number> neu = new XYChart.Series<>(); neu.setName("Trung lập");
        XYChart.Series<String, Number> neg = new XYChart.Series<>(); neg.setName("Tiêu cực");

        reliefData.forEach((item, counts) -> {
            pos.getData().add(new XYChart.Data<>(item, counts.getOrDefault("positive", 0)));
            neu.getData().add(new XYChart.Data<>(item, counts.getOrDefault("neutral", 0)));
            neg.getData().add(new XYChart.Data<>(item, counts.getOrDefault("negative", 0)));
        });

        barChart.getData().addAll(neg, neu, pos);
        chartContainer.getChildren().add(barChart);
    }
    
    private void drawLineChartReliefTimeline(Map<String, Map<String, Map<String, Integer>>> data) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Diễn biến cứu trợ theo thời gian");
        xAxis.setLabel("Ngày");
        
        List<String> allDates = data.values().stream().flatMap(m -> m.keySet().stream()).distinct().sorted().toList();

        data.forEach((item, dailyStats) -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(item);
            for (String d : allDates) {
                int total = dailyStats.getOrDefault(d, Collections.emptyMap()).values().stream().mapToInt(Integer::intValue).sum();
                String dateLabel = d.length() > 5 ? d.substring(5) : d;
                series.getData().add(new XYChart.Data<>(dateLabel, total));
            }
            lineChart.getData().add(series);
        });
        chartContainer.getChildren().add(lineChart);
    }
}