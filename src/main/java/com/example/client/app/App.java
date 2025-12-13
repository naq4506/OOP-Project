package com.example.client.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Load file giao diện từ resources
    	// Dấu "/" ở đầu cực kỳ quan trọng, nó báo cho Java tìm từ thư mục gốc của resources
    	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/client/view/home.fxml"));
    	scene = new Scene(fxmlLoader.load(), 800, 600); // Kích thước cửa sổ 800x600
        stage.setTitle("Humanitarian Logistics Crawler");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}