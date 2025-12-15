package com.example.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class MenuController {

    private void navigateToDashboard(MouseEvent event, String analysisType, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/view/dashboard.fxml"));
        Parent dashboardView = loader.load();

        DashboardController controller = loader.getController();
        controller.setAnalysisType(analysisType);
        controller.setDashboardTitle(title);

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        
        Scene scene = new Scene(dashboardView);
        stage.setScene(scene);
        stage.setMaximized(false); 
        stage.setMaximized(true); 
        // ------------------------------------------------
        
        stage.show();
    }

    @FXML 
    void onProblem1Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "SENTIMENT", "1. Theo dõi Tâm lý Công chúng");
    }

    @FXML 
    void onProblem2Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "DAMAGE", "2. Đánh giá Thiệt hại (Damage Assessment)");
    }

    @FXML 
    void onProblem3Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "RELIEF", "3. Hài lòng cứu trợ (Relief Satisfaction)");
    }

    @FXML 
    void onProblem4Click(MouseEvent event) throws IOException {
        navigateToDashboard(event, "RELIEF_TIMELINE", "4. Diễn biến hàng hóa (Relief Timeline)");
    }
}