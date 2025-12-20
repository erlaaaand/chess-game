package com.erland.chess.view.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.erland.chess.model.Board;
import com.erland.chess.model.Move; // Added import
import com.erland.chess.view.MenuView.GameMode;

import java.util.ArrayList;

/**
 * Control panel UI component
 */
public class ControlPanelUI {
    private final VBox root;
    private final Board board;
    private final GameMode gameMode;
    
    // UI Components
    private Label turnLabel;
    private Label statusLabel;
    private Label checkLabel;
    private Button btnSurrender;
    private Button btnCancel;
    private Button btnMenu;
    private TextArea moveLog;
    
    public ControlPanelUI(Board board, GameMode gameMode,
                         Runnable onSurrender, Runnable onCancel, Runnable onMenu) {
        this.board = board;
        this.gameMode = gameMode;
        this.root = new VBox(15);
        
        createUI(onSurrender, onCancel, onMenu);
    }
    
    private void createUI(Runnable onSurrender, Runnable onCancel, Runnable onMenu) {
        root.getStyleClass().add("control-panel");
        root.setPadding(new Insets(20));
        root.setPrefWidth(300);
        root.setAlignment(Pos.TOP_CENTER);
        
        // Title
        Label title = new Label("♔ CHESS GAME ♔");
        title.getStyleClass().add("panel-title");
        
        // Game mode label
        Label modeLabel = new Label(getGameModeText());
        modeLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
        
        // Turn indicator
        turnLabel = new Label("Turn: White");
        turnLabel.getStyleClass().add("turn-label");
        
        // Check indicator
        checkLabel = new Label("");
        checkLabel.getStyleClass().add("check-label");
        
        // Status
        statusLabel = new Label("Ready to play");
        statusLabel.getStyleClass().add("status-label");
        
        // Buttons
        btnSurrender = new Button("Surrender");
        btnSurrender.getStyleClass().addAll("game-button", "surrender-button");
        btnSurrender.setMaxWidth(Double.MAX_VALUE);
        btnSurrender.setOnAction(e -> onSurrender.run());
        
        btnCancel = new Button("Cancel Game");
        btnCancel.getStyleClass().addAll("game-button", "cancel-button");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> onCancel.run());
        
        btnMenu = new Button("Back to Menu");
        btnMenu.getStyleClass().add("game-button");
        btnMenu.setMaxWidth(Double.MAX_VALUE);
        btnMenu.setOnAction(e -> onMenu.run());
        
        // Move log
        Label logLabel = new Label("Move History:");
        logLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        moveLog = new TextArea();
        moveLog.getStyleClass().add("move-log");
        moveLog.setEditable(false);
        moveLog.setPrefHeight(300);
        moveLog.setWrapText(true);
        
        // Add all components
        root.getChildren().addAll(
            title,
            modeLabel,
            new Separator(),
            turnLabel,
            checkLabel,
            statusLabel,
            new Separator(),
            btnSurrender,
            btnCancel,
            btnMenu,
            new Separator(),
            logLabel,
            moveLog
        );
    }
    
    private String getGameModeText() {
        switch (gameMode) {
            case VS_COMPUTER:
                return "vs Computer";
            case LOCAL_MULTIPLAYER:
                return "Local Multiplayer";
            case NETWORK:
                return "Online Game";
            default:
                return "Game Mode";
        }
    }
    
    public void updateTurnLabel(boolean isWhiteTurn) {
        turnLabel.setText("Turn: " + (isWhiteTurn ? "White" : "Black"));
        btnCancel.setDisable(!board.canCancelGame());
    }
    
    public void updateCheckStatus(boolean whiteInCheck, boolean blackInCheck) {
        if (whiteInCheck) {
            checkLabel.setText("⚠️ WHITE IN CHECK! ⚠️");
            checkLabel.setTextFill(Color.web("#e74c3c"));
        } else if (blackInCheck) {
            checkLabel.setText("⚠️ BLACK IN CHECK! ⚠️");
            checkLabel.setTextFill(Color.web("#e74c3c"));
        } else {
            checkLabel.setText("");
        }
    }
    
    public void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
    
    // Fixed: Changed Board.Move to Move
    public void updateMoveLog(ArrayList<Move> moveHistory) {
        StringBuilder log = new StringBuilder();
        
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                log.append(String.format("%2d. ", (i / 2 + 1)));
            }
            log.append(moveHistory.get(i).toNotation()).append(" ");
            if (i % 2 == 1) {
                log.append("\n");
            }
        }
        
        moveLog.setText(log.toString());
        moveLog.setScrollTop(Double.MAX_VALUE);
    }
    
    public void disableGameButtons() {
        btnSurrender.setDisable(true);
        btnCancel.setDisable(true);
    }
    
    public VBox getRoot() {
        return root;
    }
}