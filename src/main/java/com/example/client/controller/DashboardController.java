package com.example.client.controller;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.*;

public class DashboardController {

    @FXML private TextField txtDisasterName;
    @FXML private TextField txtKeyword;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ToggleGroup platformGroup;
    @FXML private Label statusLabel;
    @FXML private Button btnStart;
    @FXML private Label titleLabel; 
    @FXML private VBox chartContainer;

    private final ClientController clientController = new ClientController("http://localhost:8080"); 
    private String currentAnalysisType;

    public void setAnalysisType(String type) { this.currentAnalysisType = type; }
    public void setDashboardTitle(String title) { if (titleLabel != null) titleLabel.setText(title); }
    
    private String getPlatformCode(String label) {
        if (label == null) return "facebook";
        String lower = label.toLowerCase();
        // Đã xóa Reuters và AP News
        if (lower.contains("facebook")) return "facebook";
        if (lower.contains("instagram")) return "instagram";
        if (lower.contains("threads")) return "threads";
        if (lower.contains("x") || lower.contains("twitter")) return "x";
        if (lower.contains("dân trí")) return "dantri";
        if (lower.contains("nhân dân")) return "nhandan";
        return "facebook"; // Mặc định
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

        Object dataObj = allResults.get(analysisType);
        
        if (dataObj == null && !allResults.isEmpty()) {
            dataObj = allResults.values().iterator().next();
        }

        if (dataObj instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
            
            if ("SENTIMENT".equals(analysisType) || "SENTIMENT_TREND".equals(analysisType)) {
                drawSentimentChart(dataMap);
            } else {
                drawGenericChart(dataMap, analysisType);
            }
        } else {
            chartContainer.getChildren().add(new Label("Định dạng dữ liệu không hỗ trợ vẽ biểu đồ."));
        }
    }

    private void drawSentimentChart(Map<String, Object> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Ngày");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Số lượng phản hồi");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Diễn biến Tâm lý Cộng đồng");
        barChart.setAnimated(true);
        // Để biểu đồ tự giãn theo container
        barChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        XYChart.Series<String, Number> seriesPos = new XYChart.Series<>();
        seriesPos.setName("Tích cực 😊");
        
        XYChart.Series<String, Number> seriesNeg = new XYChart.Series<>();
        seriesNeg.setName("Tiêu cực 😡");
        
        XYChart.Series<String, Number> seriesNeu = new XYChart.Series<>();
        seriesNeu.setName("Trung lập 😐");

        TreeMap<String, Object> sortedData = new TreeMap<>(data);

        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
            String date = entry.getKey();
            if (entry.getValue() instanceof Map) {
                Map<String, Number> stats = (Map<String, Number>) entry.getValue();
                Number pos = stats.getOrDefault("positive", 0);
                Number neg = stats.getOrDefault("negative", 0);
                Number neu = stats.getOrDefault("neutral", 0);
                
                seriesPos.getData().add(new XYChart.Data<>(date, pos));
                seriesNeg.getData().add(new XYChart.Data<>(date, neg));
                seriesNeu.getData().add(new XYChart.Data<>(date, neu));
            }
        }

        barChart.getData().addAll(seriesNeg, seriesNeu, seriesPos);
        
        // Thêm chart vào container và cho nó mọc (grow) hết cỡ
        VBox.setVgrow(barChart, javafx.scene.layout.Priority.ALWAYS);
        chartContainer.getChildren().add(barChart);
    }
    
    private void drawGenericChart(Map<String, Object> data, String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(title);
        barChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Dữ liệu");
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Number) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), (Number) entry.getValue()));
            }
        }
        
        barChart.getData().add(series);
        VBox.setVgrow(barChart, javafx.scene.layout.Priority.ALWAYS);
        chartContainer.getChildren().add(barChart);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}