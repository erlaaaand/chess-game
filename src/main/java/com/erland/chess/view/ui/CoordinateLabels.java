package com.erland.chess.view.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Utility class for adding coordinate labels to chess board
 */
public class CoordinateLabels {
    
    public static void addCoordinates(BorderPane container, Pane boardPane, int tileSize) {
        // Files (a-h) at bottom
        HBox filesBox = new HBox();
        filesBox.setAlignment(Pos.CENTER);
        filesBox.setSpacing(tileSize - 15);
        filesBox.setPadding(new Insets(5, 0, 0, 10));
        
        for (char c = 'a'; c <= 'h'; c++) {
            Label label = new Label(String.valueOf(c));
            label.getStyleClass().add("coordinate-label");
            label.setMinWidth(15);
            filesBox.getChildren().add(label);
        }
        
        // Ranks (8-1) at left
        VBox ranksBox = new VBox();
        ranksBox.setAlignment(Pos.CENTER);
        ranksBox.setSpacing(tileSize - 23);
        ranksBox.setPadding(new Insets(10, 5, 0, 0));
        
        for (int i = 8; i >= 1; i--) {
            Label label = new Label(String.valueOf(i));
            label.getStyleClass().add("coordinate-label");
            label.setMinHeight(15);
            ranksBox.getChildren().add(label);
        }
        
        container.setCenter(boardPane);
        container.setLeft(ranksBox);
        container.setBottom(filesBox);
    }
}