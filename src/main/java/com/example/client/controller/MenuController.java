package com.example.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import java.io.IOException;

public class MenuController {

    private void navigateToDashboard(MouseEvent event, String analysisType, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/view/dashboard.fxml"));
        Parent dashboardView = loader.load();

        // Lấy controller của Dashboard để truyền dữ liệu
        DashboardController controller = loader.getController();
        controller.setAnalysisType(analysisType); // Cần thêm hàm này bên DashboardController
        controller.setDashboardTitle(title);      // Cần thêm hàm này bên DashboardController

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(new Scene(dashboardView, 800, 600));
        window.show();
    }

    @FXML void onProblem1Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "SENTIMENT", "Theo dõi Tâm lý Công chúng");
    }

    @FXML void onProblem2Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "DAMAGE", "Đánh giá Thiệt hại");
    }

    @FXML void onProblem3Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "RELIEF", "Đánh giá Hài lòng Cứu trợ");
    }

    @FXML void onProblem4Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "RELIEF_TIMELINE", "Phân tích Hàng cứu trợ theo Thời gian");
    }
}