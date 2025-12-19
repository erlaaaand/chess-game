package com.erland.chess;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.erland.chess.view.MenuView;

public class ChessApplication extends Application {
    
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("♔ Chess Game - Modern Edition");
        
        // Load icon if available
        try {
            primaryStage.getIcons().add(new Image(
                getClass().getResourceAsStream("/images/chess_icon.png")));
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
        }
        
        // Create and show menu
        MenuView menuView = new MenuView(primaryStage);
        Scene scene = new Scene(menuView.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Load CSS stylesheet
        try {
            scene.getStylesheets().add(
                getClass().getResource("/styles/chess.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styling");
        }
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        System.out.println("=".repeat(60));
        System.out.println("Chess Game - JavaFX Edition Started!");
        System.out.println("=".repeat(60));
    }
    
    @Override
    public void stop() {
        System.out.println("Application closing...");
        // Cleanup if needed
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}