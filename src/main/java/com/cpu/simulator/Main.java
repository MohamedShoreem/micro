package com.cpu.simulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the MIPS CPU Simulator application
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            // Create the scene
            Scene scene = new Scene(root, 1200, 800);
            
            // Set up the stage
            primaryStage.setTitle("MIPS CPU Simulator - Tomasulo Algorithm");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error starting application: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
