package com.example.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class HomeController {

    @FXML
    protected void onExploreClick(ActionEvent event) throws IOException {
        // Chuyển sang màn hình Menu
        Parent menuView = FXMLLoader.load(getClass().getResource("/com/example/client/view/menu.fxml"));
        Scene menuScene = new Scene(menuView, 800, 600);
        
        // Lấy Stage hiện tại từ sự kiện nút bấm
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(menuScene);
        window.show();
    }
}