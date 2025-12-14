package com.example.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import java.io.IOException;

public class MenuController {

    /**
     * Điều hướng đến Dashboard và thiết lập loại phân tích.
     * @param event MouseEvent
     * @param analysisType Loại phân tích (SENTIMENT, DAMAGE, RELIEF, RELIEF_TIMELINE)
     * @param title Tiêu đề hiển thị trên Dashboard
     */
    private void navigateToDashboard(MouseEvent event, String analysisType, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/view/dashboard.fxml"));
        Parent dashboardView = loader.load();

        // Lấy controller của Dashboard để truyền dữ liệu
        DashboardController controller = loader.getController();
        controller.setAnalysisType(analysisType); // Dùng hàm đã sửa để set Type 
        // Title sẽ được set tự động trong DashboardController sau khi set AnalysisType

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(new Scene(dashboardView, 800, 600));
        window.show();
    }

    // Các hàm xử lý sự kiện click
    
    @FXML 
    void onProblem1Click(MouseEvent event) throws IOException {
        // Bài toán 1: Theo dõi tâm lý công chúng theo thời gian
        navigateToDashboard(event, "SENTIMENT", "Theo dõi Tâm lý Công chúng theo Thời gian");
    }

    @FXML 
    void onProblem2Click(MouseEvent event) throws IOException {
        // Bài toán 2: Xác định mức độ và loại thiệt hại phổ biến nhất
        navigateToDashboard(event, "DAMAGE", "Đánh giá Thiệt hại Phổ biến");
    }

    @FXML 
    void onProblem3Click(MouseEvent event) throws IOException {
        // Bài toán 3: Xác định mức độ hài lòng và không hài lòng của công chúng (tổng hợp)
        navigateToDashboard(event, "RELIEF", "Đánh giá Hài lòng Cứu trợ (Tổng quan)");
    }

    @FXML 
    void onProblem4Click(MouseEvent event) throws IOException {
        // Bài toán 4: Theo dõi tâm lý theo từng loại hàng cứu trợ (timeline)
        navigateToDashboard(event, "RELIEF_TIMELINE", "Phân tích Hàng cứu trợ theo Thời gian");
    }
}