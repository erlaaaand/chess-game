package com.erland.chess.view;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.Move; // Gunakan Move yang benar
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.network.NetworkHandler;
import com.erland.chess.review.GameReviewer;
import com.erland.chess.view.MenuView.GameMode;
import com.erland.chess.view.handlers.*;
import com.erland.chess.view.renderers.BoardRenderer;
import com.erland.chess.view.ui.ControlPanelUI;
import com.erland.chess.view.ui.CoordinateLabels;

// AI Imports
import com.erland.chess.ai.AICharacter;
import com.erland.chess.service.AIService;
import com.erland.chess.core.GameEngine; // Jika nanti mau migrasi full ke engine

public class BoardView implements MoveExecutor.MoveListener {
    private static final int TILE_SIZE = 85;
    
    private final Stage primaryStage;
    private final BorderPane root;
    private final Board board;
    private final GameMode gameMode;
    private final boolean isHost;
    
    // Core components
    private final GameReviewer gameReviewer;
    private NetworkHandler networkHandler;
    private final AIService aiService; // Tambahkan Service AI
    private AICharacter currentAICharacter; // Simpan karakter AI
    
    // Rendering
    private BoardRenderer boardRenderer;
    private Canvas boardCanvas;
    private Canvas pieceCanvas;
    
    // Interaction
    private Pane highlightLayer;
    private Pane dragLayer;
    private BoardInteractionHandler interactionHandler;
    private MoveExecutor moveExecutor;
    private HighlightManager highlightManager;
    
    // UI Components
    private ControlPanelUI controlPanel;
    
    // Constructor Updated untuk menangani AI Character
    public BoardView(Stage stage, GameMode mode, Object networkOrAI, boolean isHost) {
        this.primaryStage = stage;
        this.gameMode = mode;
        this.isHost = isHost;
        this.board = new Board();
        this.gameReviewer = new GameReviewer();
        this.aiService = new AIService(); // Inisialisasi Service
        
        this.root = new BorderPane();
        root.getStyleClass().add("board-container");
        
        // Setup berdasarkan Mode
        if (mode == GameMode.NETWORK) {
            setupNetwork(networkOrAI);
        } else if (mode == GameMode.VS_COMPUTER) {
            if (networkOrAI instanceof AICharacter) {
                this.currentAICharacter = (AICharacter) networkOrAI;
            } else {
                // Default character jika null
                this.currentAICharacter = new AICharacter("Default AI", 1200);
            }
        }
        
        initializeComponents();
        createUI();
        
        // Start live analysis
        gameReviewer.startNewGame();
        System.out.println("Chess game started - Mode: " + mode);
    }
    
    private void setupNetwork(Object network) {
        if (network instanceof com.erland.chess.network.GameServer) {
            this.networkHandler = (com.erland.chess.network.GameServer) network;
        } else if (network instanceof com.erland.chess.network.GameClient) {
            this.networkHandler = (com.erland.chess.network.GameClient) network;
        }
        
        if (networkHandler != null) {
            networkHandler.setBoardPanel(this);
        }
    }
    
    private void initializeComponents() {
        // Create canvases
        boardCanvas = new Canvas(TILE_SIZE * 8, TILE_SIZE * 8);
        pieceCanvas = new Canvas(TILE_SIZE * 8, TILE_SIZE * 8);
        
        // Create layers
        highlightLayer = new Pane();
        highlightLayer.setPrefSize(TILE_SIZE * 8, TILE_SIZE * 8);
        highlightLayer.setMouseTransparent(true);
        
        dragLayer = new Pane();
        dragLayer.setPrefSize(TILE_SIZE * 8, TILE_SIZE * 8);
        dragLayer.setMouseTransparent(true);
        
        // Initialize managers
        boardRenderer = new BoardRenderer(boardCanvas, pieceCanvas, board);
        highlightManager = new HighlightManager(board, highlightLayer);
        moveExecutor = new MoveExecutor(board, gameMode, isHost, dragLayer);
        moveExecutor.setMoveListener(this);
        moveExecutor.setPromotionHandler(this::showPromotionDialog);
        
        interactionHandler = new BoardInteractionHandler(
            board, dragLayer, moveExecutor, highlightManager
        );
        
        // Setup mouse handlers
        pieceCanvas.setOnMousePressed(interactionHandler::handleMousePressed);
        pieceCanvas.setOnMouseDragged(interactionHandler::handleMouseDragged);
        pieceCanvas.setOnMouseReleased(interactionHandler::handleMouseReleased);
    }
    
    private void createUI() {
        // Create board pane with all layers
        Pane boardPane = new Pane();
        boardPane.setPrefSize(TILE_SIZE * 8, TILE_SIZE * 8);
        boardPane.getChildren().addAll(boardCanvas, highlightLayer, pieceCanvas, dragLayer);
        
        // Add coordinate labels
        BorderPane boardWithCoords = new BorderPane();
        boardWithCoords.setCenter(boardPane);
        CoordinateLabels.addCoordinates(boardWithCoords, boardPane, TILE_SIZE);
        
        root.setCenter(boardWithCoords);
        
        // Create control panel
        controlPanel = new ControlPanelUI(
            board,
            gameMode,
            this::handleSurrender,
            this::handleCancel,
            this::handleBackToMenu
        );
        root.setRight(controlPanel.getRoot());
        
        // Initial render
        boardRenderer.drawPieces();
    }
    
    // MoveListener implementation
    @Override
    public void onMoveExecuted(Move move) {
        // Update UI
        boardRenderer.setDraggedPiece(null);
        boardRenderer.drawPieces();
        controlPanel.updateMoveLog(board.moveHistory);
        controlPanel.updateTurnLabel(board.isWhiteTurn);
        controlPanel.updateCheckStatus(board.whiteInCheck, board.blackInCheck);
        controlPanel.updateStatus("Move executed", Color.web("#27ae60"));
        
        // Record for analysis
        gameReviewer.recordMove(board);
        
        // Send to network if needed
        if (networkHandler != null) {
            networkHandler.sendMove(move);
        }
        
        // Trigger computer move if needed
        if (gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn && 
            board.gameState == GameState.PLAYING) {
            scheduleComputerMove();
        }
    }
    
    @Override
    public void onGameStateChanged() {
        boardRenderer.drawPieces();
        controlPanel.updateTurnLabel(board.isWhiteTurn);
        controlPanel.updateCheckStatus(board.whiteInCheck, board.blackInCheck);
        checkGameEnd();
    }
    
    @Override
    public void onPieceSelected(Piece piece) {
        boardRenderer.setDraggedPiece(piece);
        controlPanel.updateStatus("Selected: " + piece.name, Color.web("#f39c12"));
    }
    
    @Override
    public void onPieceDeselected() {
        boardRenderer.setDraggedPiece(null);
        boardRenderer.drawPieces();
        controlPanel.updateStatus("Ready", Color.web("#95a5a6"));
    }
    
    @Override
    public void onInvalidMove(String reason) {
        controlPanel.updateStatus(reason, Color.web("#e74c3c"));
    }
    
    @Override
    public void onPromotionNeeded(int col, int row) {
        // Handled by showPromotionDialog
    }
    
    // --- REFACTORED AI LOGIC ---
    private void scheduleComputerMove() {
        controlPanel.updateStatus("Computer thinking...", Color.web("#f39c12"));
        
        // Bungkus board dalam GameEngine sementara agar sesuai dengan signature AIService
        // Atau buat method helper di sini. Untuk simplifikasi, kita panggil AIPlayer manual
        // lewat Thread terpisah agar UI tidak freeze.
        
        new Thread(() -> {
            try {
                // Simulasi delay berpikir (opsional, sudah ada di AIPlayer sebenarnya)
                Thread.sleep(500);
                
                // Gunakan AIPlayer untuk mendapatkan Move
                com.erland.chess.ai.AIPlayer aiPlayer = new com.erland.chess.ai.AIPlayer(currentAICharacter);
                
                // getMove() return CompletableFuture, kita join() karena sudah di dalam Thread background
                Move aiMove = aiPlayer.getMove(board).join();
                
                // Eksekusi move di JavaFX Thread
                Platform.runLater(() -> {
                    if (aiMove != null) {
                        executeAIMove(aiMove);
                    } else {
                        controlPanel.updateStatus("AI cannot move (Resign/Stalemate)", Color.RED);
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void executeAIMove(Move move) {
        // Set piece yang akan digerakkan
        board.selectedPiece = move.piece;
        
        // Eksekusi gerakan
        boolean success = board.movePiece(move.toCol, move.toRow);
        
        if (success) {
            // Handle Promosi Otomatis untuk AI (biasanya jadi Queen)
            if (move.piece.name.equals("Pawn") && (move.toRow == 0 || move.toRow == 7)) {
                board.promotePawn(move.toCol, move.toRow, "Queen");
                // Update catatan move terakhir agar tercatat promosinya
                if (!board.moveHistory.isEmpty()) {
                    board.moveHistory.get(board.moveHistory.size() - 1).promotionPiece = "Queen";
                }
            }
            
            // Refresh UI
            onMoveExecuted(move);
            onGameStateChanged();
        }
    }
    
    private void showPromotionDialog(int col, int row, MoveExecutor.PromotionCallback callback) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Choose your promotion piece:");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);
        
        String[] pieces = {"Queen", "Rook", "Bishop", "Knight"};
        String[] icons = {"♕", "♖", "♗", "♘"};
        
        for (int i = 0; i < pieces.length; i++) {
            Button btn = new Button(icons[i] + "\n" + pieces[i]);
            btn.setMinSize(80, 80);
            btn.getStyleClass().add("promotion-piece");
            String piece = pieces[i];
            btn.setOnAction(e -> {
                dialog.setResult(piece);
                dialog.close();
            });
            grid.add(btn, i, 0);
        }
        
        dialog.getDialogPane().setContent(grid);
        // Add default result to prevent close without choice
        dialog.setResultConverter(dialogButton -> null); 
        
        dialog.showAndWait().ifPresent(choice -> {
            callback.onPromotionSelected(choice);
            boardRenderer.drawPieces();
        });
    }
    
    private void checkGameEnd() {
        if (board.gameState == GameState.PLAYING) {
            return;
        }
        
        controlPanel.disableGameButtons();
        
        String result = "";
        Color color = Color.WHITE;
        
        switch (board.gameState) {
            case WHITE_WON:
                result = "♔ White Wins! ♔";
                color = Color.web("#87ceeb");
                break;
            case BLACK_WON:
                result = "♚ Black Wins! ♚";
                color = Color.web("#ff69b4");
                break;
            case STALEMATE:
                result = "Draw - Stalemate!";
                color = Color.web("#ffd700");
                break;
            case CANCELLED:
                result = "Game Cancelled";
                color = Color.GRAY;
                break;
        }
        
        controlPanel.updateStatus(result, color);
        
        if (board.gameState != GameState.CANCELLED) {
            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(e -> showReviewDialog());
            pause.play();
        }
    }
    
    private void handleSurrender() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Surrender");
        confirm.setHeaderText("Are you sure you want to surrender?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean whiteResigns = (gameMode == GameMode.NETWORK) ? isHost : board.isWhiteTurn;
                board.surrender(whiteResigns);
                
                if (networkHandler != null) {
                    networkHandler.sendSurrender();
                }
                
                checkGameEnd();
                boardRenderer.drawPieces();
            }
        });
    }
    
    private void handleCancel() {
        if (!board.canCancelGame()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Cannot Cancel");
            alert.setHeaderText("Game cannot be cancelled after the first move!");
            alert.showAndWait();
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Game");
        confirm.setHeaderText("Cancel this game?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                board.gameState = GameState.CANCELLED;
                
                if (networkHandler != null) {
                    networkHandler.sendCancel();
                }
                
                handleBackToMenu();
            }
        });
    }
    
    private void handleBackToMenu() {
        if (networkHandler != null) {
            try {
                networkHandler.close();
            } catch (Exception e) {
                System.err.println("Error closing network: " + e.getMessage());
            }
        }
        
        if (aiService != null) {
             aiService.shutdown();
        }
        
        MenuView menuView = new MenuView(primaryStage);
        primaryStage.getScene().setRoot(menuView.getRoot());
    }
    
    private void showReviewDialog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Game Review");
        confirm.setHeaderText("Game finished!");
        confirm.setContentText("Would you like to add a review and see analysis?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                TextInputDialog input = new TextInputDialog();
                input.setTitle("Game Review");
                input.setHeaderText("Enter your review/comments:");
                input.setContentText("Comment:");
                
                input.showAndWait().ifPresent(comment -> {
                    boolean saved = gameReviewer.finalizeGame(board, comment);
                    if (saved) {
                        showAnalysisView();
                    }
                });
            } else {
                gameReviewer.finalizeGame(board, "No comment");
            }
        });
    }
    
    private void showAnalysisView() {
        AnalysisView analysisView = new AnalysisView(primaryStage, board, this);
        primaryStage.getScene().setRoot(analysisView.getRoot());
    }
    
    public void receiveMove(Move move) {
        javafx.application.Platform.runLater(() -> {
            Piece p = board.getPiece(move.fromCol, move.fromRow);
            if (p != null) {
                moveExecutor.tryMove(p, move.toCol, move.toRow);
                
                if (move.promotionPiece != null) {
                    board.promotePawn(move.toCol, move.toRow, move.promotionPiece);
                }
            }
        });
    }
    
    public Parent getRoot() {
        return root;
    }
}