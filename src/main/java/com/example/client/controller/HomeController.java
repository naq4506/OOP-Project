package com.example.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {

    @FXML
    protected void onExploreClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/view/menu.fxml"));
        Parent menuView = loader.load();

        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        
        // Chuyển Scene
        stage.setScene(new Scene(menuView));
        
        // --- FIX LỖI MẤT FULL MÀN HÌNH ---
        stage.setMaximized(false); // Reset trạng thái
        stage.setMaximized(true);  // Ép phóng to lại ngay lập tức
        // ---------------------------------
        
        stage.show();
    }
}