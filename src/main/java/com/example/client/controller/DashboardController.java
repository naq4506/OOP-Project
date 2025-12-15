package com.example.client.controller;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.*;

public class DashboardController {

    @FXML private TextField txtDisasterName;
    @FXML private TextField txtKeyword;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ToggleGroup platformGroup; // Giữ nguyên để khớp với FXML hiện tại 
    @FXML private Label statusLabel;
    @FXML private Button btnStart;
    @FXML private Label titleLabel;
    @FXML private VBox chartContainer;

    // Cấu hình Client kết nối tới Server (Port 8080)
    private final ClientController clientController = new ClientController("http://localhost:8080");
    private String currentAnalysisType = "RELIEF"; // Mặc định

    public void setAnalysisType(String type) { this.currentAnalysisType = type; }
    public void setDashboardTitle(String title) { if (titleLabel != null) titleLabel.setText(title); }

    private String getPlatformCode(String label) {
        if (label == null) return "facebook";
        String lower = label.toLowerCase();
        if (lower.contains("facebook")) return "facebook";
        if (lower.contains("instagram")) return "instagram";
        if (lower.contains("threads")) return "threads";
        if (lower.contains("x") || lower.contains("twitter")) return "x";
        if (lower.contains("dân trí")) return "dantri";
        if (lower.contains("nhân dân")) return "nhandan";
        return "facebook";
    }

    @FXML
    protected void onStartCrawl() {
        String disasterName = txtDisasterName.getText().trim();
        String keyword = txtKeyword.getText().trim();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (disasterName.isEmpty() || start == null || end == null) {
            showAlert("Thiếu thông tin", "Vui lòng nhập Tên thảm họa và Thời gian!");
            return;
        }

        RadioButton selectedRadio = (RadioButton) platformGroup.getSelectedToggle();
        String label = (selectedRadio != null) ? selectedRadio.getText() : "Facebook";
        String platformCode = getPlatformCode(label);

        ClientRequest request = new ClientRequest();
        request.setDisasterName(disasterName);
        request.setKeyword(keyword);
        request.setStartDate(start.toString());
        request.setEndDate(end.toString());
        request.setPlatforms(Collections.singletonList(platformCode));
        request.setAnalysisType(currentAnalysisType);

        btnStart.setDisable(true);
        statusLabel.setText("⏳ Đang phân tích dữ liệu từ " + label + "...");
        chartContainer.getChildren().clear();

        new Thread(() -> {
            try {
                ClientResponse<Map<String, Object>> response = clientController.sendAnalysis(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        statusLabel.setText("✅ Hoàn tất.");
                        showAnalysisResults(response.getData(), currentAnalysisType);
                    } else {
                        statusLabel.setText("❌ Lỗi Server: " + response.getErrorMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("❌ Lỗi kết nối: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> btnStart.setDisable(false));
            }
        }).start();
    }

    private void showAnalysisResults(Map<String, Object> allResults, String analysisType) {
        chartContainer.getChildren().clear();

        if (allResults == null || allResults.isEmpty()) {
            chartContainer.getChildren().add(new Label("Không có dữ liệu để hiển thị."));
            return;
        }

        // Lấy dữ liệu theo Key phân tích
        Object dataObj = allResults.get(analysisType);

        // Fail-safe: Nếu không tìm thấy key, lấy giá trị đầu tiên
        if (dataObj == null && !allResults.isEmpty()) {
            dataObj = allResults.values().iterator().next();
        }

        if (dataObj instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) dataObj;

            // Bóc tách dữ liệu nếu bị bọc trong "counts"
            if (dataMap.containsKey("counts") && dataMap.get("counts") instanceof Map) {
                System.out.println("⚠️ Đang bóc tách dữ liệu từ key 'counts'...");
                dataMap = (Map<String, Object>) dataMap.get("counts");
            }

            System.out.println("📊 Dữ liệu vẽ biểu đồ: " + dataMap);

            if ("SENTIMENT".equals(analysisType) || "SENTIMENT_TREND".equals(analysisType)) {
                drawSentimentChart(dataMap);
            } else {
                drawGenericChart(dataMap, getChartTitle(analysisType));
            }
        } else {
            chartContainer.getChildren().add(new Label("Định dạng dữ liệu không hỗ trợ vẽ biểu đồ."));
        }
    }

    private String getChartTitle(String type) {
        if ("RELIEF".equals(type)) return "Nhu cầu Cứu trợ & Hậu cần";
        if ("DAMAGE".equals(type)) return "Thống kê Thiệt hại";
        return "Kết quả phân tích";
    }

    // --- BIỂU ĐỒ 1: SENTIMENT ---
    private void drawSentimentChart(Map<String, Object> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Thời gian");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Số lượng");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Phân tích Cảm xúc");
        barChart.setAnimated(true);
        barChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        XYChart.Series<String, Number> seriesPos = new XYChart.Series<>(); seriesPos.setName("Tích cực");
        XYChart.Series<String, Number> seriesNeg = new XYChart.Series<>(); seriesNeg.setName("Tiêu cực");
        XYChart.Series<String, Number> seriesNeu = new XYChart.Series<>(); seriesNeu.setName("Trung lập");

        TreeMap<String, Object> sortedData = new TreeMap<>(data);

        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
            String date = entry.getKey();
            if (entry.getValue() instanceof Map) {
                // Cast an toàn sang Map<String, Object>
                Map<?, ?> rawStats = (Map<?, ?>) entry.getValue();
                
                // Helper lấy số an toàn
                Number pos = getNumberSafe(rawStats.get("positive"));
                Number neg = getNumberSafe(rawStats.get("negative"));
                Number neu = getNumberSafe(rawStats.get("neutral"));

                seriesPos.getData().add(new XYChart.Data<>(date, pos));
                seriesNeg.getData().add(new XYChart.Data<>(date, neg));
                seriesNeu.getData().add(new XYChart.Data<>(date, neu));
            }
        }

        barChart.getData().addAll(seriesNeg, seriesNeu, seriesPos);
        VBox.setVgrow(barChart, Priority.ALWAYS);
        chartContainer.getChildren().add(barChart);
    }

    // --- BIỂU ĐỒ 2: GENERIC (Dùng Đệ quy để xử lý Map lồng nhau) ---
    private void drawGenericChart(Map<String, Object> data, String title) {
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Danh mục");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Số lượng");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(title);
        barChart.setAnimated(true);
        barChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tổng hợp");

        // Sắp xếp key
        TreeMap<String, Object> sortedData = new TreeMap<>(data);

        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
            String category = entry.getKey();
            Object value = entry.getValue();

            // Tính tổng đệ quy (để xử lý vụ lồng nhau nhiều lớp)
            double totalCount = calculateTotalRecursively(value);

            if (totalCount > 0) {
                series.getData().add(new XYChart.Data<>(category, totalCount));
            }
        }

        barChart.getData().add(series);
        VBox.setVgrow(barChart, Priority.ALWAYS);
        chartContainer.getChildren().add(barChart);
    }

    // --- HÀM PHỤ TRỢ: TÍNH TỔNG ĐỆ QUY ---
    // Hàm này sẽ đào sâu vào mọi ngóc ngách của Map để tìm số và cộng lại
    private double calculateTotalRecursively(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } 
        else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (Exception e) { return 0; }
        } 
        else if (value instanceof Map) {
            double sum = 0;
            Map<?, ?> map = (Map<?, ?>) value;
            for (Object v : map.values()) {
                sum += calculateTotalRecursively(v);
            }
            return sum;
        }
        return 0;
    }

    // Helper lấy số từ object an toàn, tránh NullPointerException
    private Number getNumberSafe(Object obj) {
        if (obj instanceof Number) return (Number) obj;
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch(Exception e) {}
        }
        return 0;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}