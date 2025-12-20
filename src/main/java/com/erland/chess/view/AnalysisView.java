package com.erland.chess.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import com.erland.chess.ai.PythonBridge;
import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.utils.NotationConverter;
import com.erland.chess.view.renderers.BoardRenderer;
import com.erland.chess.view.MenuView.GameMode; // Corrected import

import java.util.ArrayList;
import java.util.List;

public class AnalysisView {
    private final BorderPane root;
    private final Board originalBoard;
    private final GameMode gameMode; // Fixed: uses MenuView.GameMode
    private final BoardRenderer boardRenderer;
    private final javafx.scene.canvas.Canvas boardCanvas;
    private final javafx.scene.canvas.Canvas pieceCanvas;
    
    // Components
    private LineChart<String, Number> evaluationChart;
    private TextArea analysisLog;
    private ProgressBar progressBar;
    private Button btnBackToMenu;
    
    // Analysis State
    private Board shadowBoard; // Papan tiruan untuk replay
    private List<Move> gameMoves;
    private XYChart.Series<String, Number> series;

    public AnalysisView(javafx.stage.Stage stage, Board board, Object parent) {
        this.originalBoard = board;
        this.gameMoves = new ArrayList<>(board.moveHistory);
        this.gameMode = GameMode.LOCAL_MULTIPLAYER; // Fixed: Default fallback, or pass as param
        
        this.root = new BorderPane();
        this.root.setPadding(new Insets(20));
        this.root.getStyleClass().add("analysis-view");
        
        // Init Shadow Board (Board kosong baru)
        this.shadowBoard = new Board(); 
        
        // Init Rendering (Canvas baru khusus analisis)
        this.boardCanvas = new javafx.scene.canvas.Canvas(600, 600);
        this.pieceCanvas = new javafx.scene.canvas.Canvas(600, 600);
        this.boardRenderer = new BoardRenderer(boardCanvas, pieceCanvas, shadowBoard);
        
        createUI(stage);
        
        // Render posisi awal
        boardRenderer.drawPieces();
        
        // Jalankan analisis otomatis
        startFullAnalysis();
    }
    
    private void createUI(javafx.stage.Stage stage) {
        // --- Left: Board Visualization ---
        StackPane boardPane = new StackPane(boardCanvas, pieceCanvas);
        boardPane.setStyle("-fx-border-color: #8B4513; -fx-border-width: 5;");
        
        // --- Right: Charts & Logs ---
        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(400);
        rightPanel.setPadding(new Insets(0, 0, 0, 20));
        
        // 1. Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Move Number");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Evaluation (Centipawns)");
        
        evaluationChart = new LineChart<>(new CategoryAxis(), yAxis);
        evaluationChart.setTitle("Game Advantage");
        evaluationChart.setCreateSymbols(false);
        evaluationChart.setAnimated(false);
        
        series = new XYChart.Series<>();
        series.setName("White Advantage");
        evaluationChart.getData().add(series);
        
        // 2. Log
        analysisLog = new TextArea();
        analysisLog.setEditable(false);
        analysisLog.setWrapText(true);
        analysisLog.setPrefHeight(200);
        
        // 3. Controls
        HBox controls = new HBox(10);
        Button btnPrev = new Button("<< Prev");
        Button btnNext = new Button("Next >>");
        btnBackToMenu = new Button("Back to Menu");
        
        btnPrev.setOnAction(e -> navigateHistory(-1));
        btnNext.setOnAction(e -> navigateHistory(1));
        btnBackToMenu.setOnAction(e -> {
            MenuView menu = new MenuView(stage);
            stage.getScene().setRoot(menu.getRoot());
        });
        
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        
        controls.getChildren().addAll(btnPrev, btnNext, btnBackToMenu);
        controls.setAlignment(Pos.CENTER);
        
        rightPanel.getChildren().addAll(evaluationChart, new Label("Analysis Log:"), analysisLog, progressBar, controls);
        
        root.setCenter(boardPane);
        root.setRight(rightPanel);
    }
    
    private int currentMoveIndex = 0;
    
    private void navigateHistory(int direction) {
        int targetIndex = currentMoveIndex + direction;
        if (targetIndex < 0 || targetIndex > gameMoves.size()) return;
        
        currentMoveIndex = targetIndex;
        
        // Reconstruct board state at this move index
        reconstructBoardAt(currentMoveIndex);
    }
    
    private void reconstructBoardAt(int moveIndex) {
        // Reset shadow board
        shadowBoard = new Board(); // Reset ke posisi awal
        
        // Replay moves
        for (int i = 0; i < moveIndex; i++) {
            Move m = gameMoves.get(i);
            // Kita cari piece di shadow board yang sesuai dengan koordinat move
            com.erland.chess.model.pieces.Piece p = shadowBoard.getPiece(m.fromCol, m.fromRow);
            if (p != null) {
                shadowBoard.movePiece(m.toCol, m.toRow);
                // Handle promotion replay jika perlu (sederhana)
                if (m.promotionPiece != null) {
                    shadowBoard.promotePawn(m.toCol, m.toRow, m.promotionPiece);
                }
            }
        }
        
        // Update View
        boardRenderer.drawPieces();
        
        // Update Log text highlight (opsional)
        analysisLog.appendText("\nJumped to move " + moveIndex);
    }
    
    private void startFullAnalysis() {
        analysisLog.setText("Starting AI Analysis...\n");
        progressBar.setProgress(0);
        
        new Thread(() -> {
            PythonBridge bridge = PythonBridge.getInstance();
            Board tempBoard = new Board(); // Papan simulasi untuk AI
            
            for (int i = 0; i < gameMoves.size(); i++) {
                final int moveNum = i + 1;
                Move move = gameMoves.get(i);
                
                // Cari piece di tempBoard
                com.erland.chess.model.pieces.Piece p = tempBoard.getPiece(move.fromCol, move.fromRow);
                if (p != null) {
                    // Update board state
                    tempBoard.movePiece(move.toCol, move.toRow);
                    if (move.promotionPiece != null) tempBoard.promotePawn(move.toCol, move.toRow, move.promotionPiece);
                    
                    // Call AI Async
                    try {
                        // Kita gunakan evaluateMove dari bridge
                        var evaluationFuture = bridge.evaluateMove(tempBoard, move);
                        
                        // Wait for result
                        var eval = evaluationFuture.join();
                        
                        if (eval != null) {
                            Platform.runLater(() -> {
                                series.getData().add(new XYChart.Data<>(String.valueOf(moveNum), eval.score));
                                analysisLog.appendText(String.format("Move %d: %s (Score: %.2f) - %s\n", 
                                        moveNum, NotationConverter.toAlgebraic(move.toCol, move.toRow), eval.score, eval.comment));
                                progressBar.setProgress((double) moveNum / gameMoves.size());
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            Platform.runLater(() -> {
                analysisLog.appendText("Analysis Complete.");
                progressBar.setProgress(1.0);
            });
            
        }).start();
    }

    public Parent getRoot() {
        return root;
    }
}