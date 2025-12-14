package com.example.client.controller;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    @FXML private ToggleGroup platformGroup; // Dùng cho RadioButton Platforms
    @FXML private Label statusLabel;
    @FXML private Button btnStart;
    @FXML private Label titleLabel; 
    @FXML private VBox chartContainer; // Thêm VBox hoặc AnchorPane để chứa Chart

    // --- Dữ liệu và Service ---
    private final ClientController clientController = new ClientController("http://localhost:8080"); 
    private String currentAnalysisType;

    /**
     * Set loại phân tích được chọn từ MenuController.
     */
    public void setAnalysisType(String type) {
        this.currentAnalysisType = type;
        System.out.println("Đã chọn chế độ phân tích: " + type);
        setDashboardTitle(mapAnalysisTypeToTitle(type));
    }
    
    /**
     * Cập nhật tiêu đề trên Dashboard.
     */
    public void setDashboardTitle(String title) {
        if (titleLabel != null) {
             titleLabel.setText(title);
        }
    }
    
    // --- Business Logic Mapping ---
    
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

    // --- Main Crawl & Analysis Logic ---

    @FXML
    protected void onStartCrawl() {
        // 1. Lấy và Validate dữ liệu Form (cơ bản)
        String disasterName = txtDisasterName.getText().trim();
        String keyword = txtKeyword.getText().trim();
        LocalDate localStartDate = dpStartDate.getValue();
        LocalDate localEndDate = dpEndDate.getValue();

        if (disasterName.isEmpty() || keyword.isEmpty() || localStartDate == null || localEndDate == null) {
            showAlert("Thiếu thông tin", "Vui lòng nhập đầy đủ tên thảm họa, từ khóa và ngày tháng!");
            return;
        }
        
        RadioButton selectedRadio = (RadioButton) platformGroup.getSelectedToggle();
        if (selectedRadio == null) {
             showAlert("Chưa chọn nền tảng", "Vui lòng chọn một nền tảng để quét!");
             return;
        }
        
        String platformCode = getPlatformCode(selectedRadio.getText());

        if (currentAnalysisType == null || currentAnalysisType.isEmpty()) {
             showAlert("Lỗi", "Không xác định được loại phân tích từ Menu.");
             return;
        }

        // 2. Gói dữ liệu vào Request DTO
        ClientRequest request = new ClientRequest();
        request.setDisasterName(disasterName);
        request.setKeyword(keyword);
        request.setStartDate(localStartDate.toString());
        request.setEndDate(localEndDate.toString());
        request.setPlatforms(java.util.Arrays.asList(platformCode)); 
        request.setAnalysisType(currentAnalysisType); // Chỉ chạy loại đang được chọn

        // 3. Cập nhật giao diện và chạy trong luồng nền
        btnStart.setDisable(true);
        statusLabel.setText("Đang gửi yêu cầu thu thập và phân tích tới Server...");
        chartContainer.getChildren().clear();

        new Thread(() -> {
            ClientResponse<Map<String, Object>> response;
            try {
                // GỌI SERVER API 
                response = clientController.sendAnalysis(request);
                
                if (response.isSuccess()) {
                    updateStatus("✅ Phân tích hoàn tất. Đang hiển thị kết quả...");
                    // CHUYỂN QUA HIỂN THỊ KẾT QUẢ TRÊN UI THREAD
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
    
    // --- UI Helper Functions ---
    
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
    // HÀM HIỂN THỊ KẾT QUẢ DƯỚI DẠNG BIỂU ĐỒ
    // =========================================================

    private void showAnalysisResults(Map<String, Object> allResults, String analysisType) {
        
        // Lấy kết quả cụ thể cho loại phân tích đã chọn (Key là tên Enum, vd: "SENTIMENT")
        Object rawResult = allResults.get(analysisType.toUpperCase());

        if (rawResult == null) {
             showAlert("Lỗi", "Không tìm thấy kết quả cho loại phân tích: " + analysisType);
             return;
        }

        try {
            switch (analysisType.toUpperCase()) {
                case "SENTIMENT":
                    // Bài toán 1: Sentiment theo thời gian (Map<LocalDate, SentimentStats>)
                    // Ep kieu tu Map<String, Map<String, Map<String, Integer>>> (từ Server) -> Map<String, Map<String, Integer>>
                    Map<String, Map<String, Integer>> sentimentTimeline = (Map<String, Map<String, Integer>>) rawResult;
                    drawLineChartSentiment(sentimentTimeline);
                    break;
                
                case "DAMAGE":
                    // Bài toán 2: Damage type (DamageStats -> Map<String, Integer>)
                    Map<String, Integer> damageCounts = (Map<String, Integer>) rawResult;
                    drawBarChartDamage(damageCounts);
                    break;
                    
                case "RELIEF":
                    // Bài toán 3: Relief item sentiment (Map<String, ReliefStats> -> Map<String, Map<String, Integer>>)
                    Map<String, Map<String, Integer>> reliefSentiment = (Map<String, Map<String, Integer>>) rawResult;
                    drawStackedBarChartRelief(reliefSentiment);
                    break;

                case "RELIEF_TIMELINE":
                    // Bài toán 4: Relief item timeline (Map<String, Map<LocalDate, ReliefStats>> -> Map<String, Map<String, Map<String, Integer>>>)
                    Map<String, Map<String, Map<String, Integer>>> reliefTimeline = (Map<String, Map<String, Map<String, Integer>>>) rawResult;
                    drawLineChartReliefTimeline(reliefTimeline);
                    break;
            }
        } catch (ClassCastException e) {
             showAlert("Lỗi Phân tích dữ liệu", "Lỗi kiểu dữ liệu từ Server. Vui lòng kiểm tra Server Log. Chi tiết: " + e.getMessage());
             e.printStackTrace();
        } catch (Exception e) {
             showAlert("Lỗi Vẽ biểu đồ", "Lỗi khi vẽ biểu đồ. Chi tiết: " + e.getMessage());
             e.printStackTrace();
        }
    }


    // =========================================================
    // HÀM VẼ BIỂU ĐỒ (DÙNG TRONG showAnalysisResults)
    // =========================================================

    /**
     * Bài toán 2: Bar Chart cho Damage Type.
     */
    private void drawBarChartDamage(Map<String, Integer> data) {
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(mapAnalysisTypeToTitle("DAMAGE"));

        xAxis.setLabel("Loại Thiệt hại");
        yAxis.setLabel("Số lượng bài đăng");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng bài đăng");

        // Thêm dữ liệu
        data.forEach((type, count) -> {
            series.getData().add(new XYChart.Data<>(type, count));
        });

        barChart.getData().add(series);
        AnchorPane.setLeftAnchor(barChart, 0.0);
        AnchorPane.setRightAnchor(barChart, 0.0);
        chartContainer.getChildren().add(barChart);
    }
    
    /**
     * Bài toán 1: Line Chart cho Sentiment Timeline (Tổng quan).
     */
    private void drawLineChartSentiment(Map<String, Map<String, Integer>> timelineData) {
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(mapAnalysisTypeToTitle("SENTIMENT"));
        
        xAxis.setLabel("Ngày (MM-DD)");
        yAxis.setLabel("Số lượng bài đăng/comments");
        
        List<String> sortedDates = new ArrayList<>(timelineData.keySet());
        Collections.sort(sortedDates);
        
        XYChart.Series<String, Number> positiveSeries = new XYChart.Series<>();
        positiveSeries.setName("Tích cực");
        
        XYChart.Series<String, Number> negativeSeries = new XYChart.Series<>();
        negativeSeries.setName("Tiêu cực");

        for (String dateStr : sortedDates) {
            Map<String, Integer> sentimentCounts = timelineData.get(dateStr);
            String dateKey = dateStr.substring(5); // Format thành MM-DD cho ngắn

            positiveSeries.getData().add(new XYChart.Data<>(dateKey, sentimentCounts.getOrDefault("positive", 0)));
            negativeSeries.getData().add(new XYChart.Data<>(dateKey, sentimentCounts.getOrDefault("negative", 0)));
        }

        lineChart.getData().addAll(positiveSeries, negativeSeries);
        AnchorPane.setLeftAnchor(lineChart, 0.0);
        AnchorPane.setRightAnchor(lineChart, 0.0);
        chartContainer.getChildren().add(lineChart);
    }

    /**
     * Bài toán 3: Stacked Bar Chart cho Relief Item Sentiment.
     */
    private void drawStackedBarChartRelief(Map<String, Map<String, Integer>> reliefData) {
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        StackedBarChart<String, Number> barChart = new StackedBarChart<>(xAxis, yAxis);
        barChart.setTitle(mapAnalysisTypeToTitle("RELIEF"));
        
        xAxis.setLabel("Loại Hàng cứu trợ");
        yAxis.setLabel("Tổng số lượng tâm lý");

        // Khởi tạo Series cho 3 loại tâm lý
        XYChart.Series<String, Number> positiveSeries = new XYChart.Series<>();
        positiveSeries.setName("Tích cực");
        
        XYChart.Series<String, Number> neutralSeries = new XYChart.Series<>();
        neutralSeries.setName("Trung lập");
        
        XYChart.Series<String, Number> negativeSeries = new XYChart.Series<>();
        negativeSeries.setName("Tiêu cực");
        

        // Thêm dữ liệu
        reliefData.forEach((item, counts) -> {
            positiveSeries.getData().add(new XYChart.Data<>(item, counts.getOrDefault("positive", 0)));
            neutralSeries.getData().add(new XYChart.Data<>(item, counts.getOrDefault("neutral", 0)));
            negativeSeries.getData().add(new XYChart.Data<>(item, counts.getOrDefault("negative", 0)));
        });

        // Vẽ Negative ở dưới (cần thiết nếu muốn thể hiện sự không hài lòng), Positive ở trên
        barChart.getData().addAll(negativeSeries, neutralSeries, positiveSeries);
        AnchorPane.setLeftAnchor(barChart, 0.0);
        AnchorPane.setRightAnchor(barChart, 0.0);
        chartContainer.getChildren().add(barChart);
    }
    
    /**
     * Bài toán 4: Line Chart cho Relief Item Timeline (Phức tạp hơn, vẽ nhiều đường).
     */
    private void drawLineChartReliefTimeline(Map<String, Map<String, Map<String, Integer>>> reliefTimelineData) {
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(mapAnalysisTypeToTitle("RELIEF_TIMELINE"));

        xAxis.setLabel("Ngày (MM-DD)");
        yAxis.setLabel("Số lượng tâm lý theo ngày");
        
        // 1. Tìm tất cả các ngày duy nhất và sắp xếp
        List<String> allDates = reliefTimelineData.values().stream()
                                  .flatMap(dailyMap -> dailyMap.keySet().stream())
                                  .distinct()
                                  .collect(Collectors.toList());
        Collections.sort(allDates);

        // 2. Tạo Series cho mỗi sự kết hợp (ReliefItem + Sentiment)
        List<XYChart.Series<String, Number>> seriesList = new ArrayList<>();
        
        reliefTimelineData.forEach((item, dailyStats) -> {
            // Positive Series
            XYChart.Series<String, Number> posSeries = new XYChart.Series<>();
            posSeries.setName(item + " (Tích cực)");
            // Negative Series
            XYChart.Series<String, Number> negSeries = new XYChart.Series<>();
            negSeries.setName(item + " (Tiêu cực)");

            for (String dateStr : allDates) {
                Map<String, Integer> counts = dailyStats.getOrDefault(dateStr, Collections.emptyMap());
                String dateKey = dateStr.substring(5); // MM-DD

                posSeries.getData().add(new XYChart.Data<>(dateKey, counts.getOrDefault("positive", 0)));
                negSeries.getData().add(new XYChart.Data<>(dateKey, counts.getOrDefault("negative", 0)));
            }
            
            seriesList.add(posSeries);
            seriesList.add(negSeries);
        });
        
        lineChart.getData().addAll(seriesList);
        AnchorPane.setLeftAnchor(lineChart, 0.0);
        AnchorPane.setRightAnchor(lineChart, 0.0);
        chartContainer.getChildren().add(lineChart);
    }
}