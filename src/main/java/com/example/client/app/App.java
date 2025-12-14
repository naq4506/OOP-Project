package com.example.client.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/client/view/home.fxml"));
        scene = new Scene(fxmlLoader.load(), 1024, 768); // Tăng kích thước mặc định
        
        stage.setTitle("Humanitarian Logistics Crawler");
        stage.setScene(scene);
        
        // --- THÊM DÒNG NÀY ĐỂ FULL MÀN HÌNH ---
        stage.setMaximized(true); 
        // -------------------------------------
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}