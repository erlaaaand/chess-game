package com.erland.chess.view;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.erland.chess.Constants;
import com.erland.chess.ai.AICharacter;
import com.erland.chess.ai.AIPlayer;
import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.network.NetworkHandler;
import com.erland.chess.review.GameReviewer;
import com.erland.chess.view.MenuView.GameMode;
import com.erland.chess.view.handlers.BoardInteractionHandler;
import com.erland.chess.view.handlers.HighlightManager;
import com.erland.chess.view.handlers.MoveExecutor;
import com.erland.chess.view.renderers.BoardRenderer;
import com.erland.chess.view.ui.ControlPanelUI;
import com.erland.chess.view.ui.CoordinateLabels;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main chess board view with complete game logic integration
 * FIXED: All compilation errors, thread safety, proper imports
 */
public class BoardView implements MoveExecutor.MoveListener {
    
    private final Stage primaryStage;
    private final BorderPane root;
    private final Board board;
    private final GameMode gameMode;
    private final boolean isHost;
    
    // Core components
    private final GameReviewer gameReviewer;
    private NetworkHandler networkHandler;
    private AICharacter aiCharacter;
    
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
    
    // Thread safety
    private final AtomicBoolean aiIsThinking = new AtomicBoolean(false);
    private volatile boolean isDisposed = false;
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AI-Move-Thread");
        t.setDaemon(true);
        return t;
    });
    
    // ==================== CONSTRUCTOR ====================
    
    public BoardView(Stage stage, GameMode mode, Object networkOrAI, boolean isHost) {
        this.primaryStage = stage;
        this.gameMode = mode;
        this.isHost = isHost;
        this.board = new Board();
        this.gameReviewer = new GameReviewer();
        
        this.root = new BorderPane();
        root.getStyleClass().add("board-container");
        
        setupGameMode(networkOrAI);
        initializeComponents();
        createUI();
        
        gameReviewer.startNewGame();
        System.out.println("=".repeat(60));
        System.out.println("Chess game started - Mode: " + mode);
        System.out.println("=".repeat(60));
    }
    
    // ==================== SETUP ====================
    
    private void setupGameMode(Object networkOrAI) {
        if (gameMode == GameMode.NETWORK) {
            if (networkOrAI instanceof NetworkHandler) {
                this.networkHandler = (NetworkHandler) networkOrAI;
                networkHandler.setBoardPanel(this);
            }
        } else if (gameMode == GameMode.VS_COMPUTER) {
            if (networkOrAI instanceof AICharacter) {
                this.aiCharacter = (AICharacter) networkOrAI;
            } else {
                this.aiCharacter = new AICharacter("Default AI", 1200);
            }
        }
    }
    
    private void initializeComponents() {
        boardCanvas = new Canvas(Constants.BOARD_PIXEL_SIZE, Constants.BOARD_PIXEL_SIZE);
        pieceCanvas = new Canvas(Constants.BOARD_PIXEL_SIZE, Constants.BOARD_PIXEL_SIZE);
        
        highlightLayer = new Pane();
        highlightLayer.setPrefSize(Constants.BOARD_PIXEL_SIZE, Constants.BOARD_PIXEL_SIZE);
        highlightLayer.setMouseTransparent(true);
        
        dragLayer = new Pane();
        dragLayer.setPrefSize(Constants.BOARD_PIXEL_SIZE, Constants.BOARD_PIXEL_SIZE);
        dragLayer.setMouseTransparent(true);
        
        boardRenderer = new BoardRenderer(boardCanvas, pieceCanvas, board);
        highlightManager = new HighlightManager(board, highlightLayer);
        moveExecutor = new MoveExecutor(board, gameMode, isHost, dragLayer);
        moveExecutor.setMoveListener(this);
        moveExecutor.setPromotionHandler(this::showPromotionDialog);
        
        interactionHandler = new BoardInteractionHandler(
            board, dragLayer, moveExecutor, highlightManager
        );
        
        pieceCanvas.setOnMousePressed(interactionHandler::handleMousePressed);
        pieceCanvas.setOnMouseDragged(interactionHandler::handleMouseDragged);
        pieceCanvas.setOnMouseReleased(interactionHandler::handleMouseReleased);
    }
    
    private void createUI() {
        Pane boardPane = new Pane();
        boardPane.setPrefSize(Constants.BOARD_PIXEL_SIZE, Constants.BOARD_PIXEL_SIZE);
        boardPane.getChildren().addAll(boardCanvas, highlightLayer, pieceCanvas, dragLayer);
        
        BorderPane boardWithCoords = new BorderPane();
        boardWithCoords.setCenter(boardPane);
        CoordinateLabels.addCoordinates(boardWithCoords, boardPane, Constants.TILE_SIZE);
        
        root.setCenter(boardWithCoords);
        
        controlPanel = new ControlPanelUI(
            board,
            gameMode,
            this::handleSurrender,
            this::handleCancel,
            this::handleBackToMenu
        );
        root.setRight(controlPanel.getRoot());
        
        boardRenderer.drawPieces();
    }
    
    // ==================== MOVE LISTENER IMPLEMENTATION ====================
    
    @Override
    public void onMoveExecuted(Move move) {
        if (isDisposed) return;
        
        Platform.runLater(() -> {
            boardRenderer.setDraggedPiece(null);
            boardRenderer.drawPieces();
            controlPanel.updateMoveLog(board.moveHistory);
            controlPanel.updateTurnLabel(board.isWhiteTurn);
            controlPanel.updateCheckStatus(board.whiteInCheck, board.blackInCheck);
            controlPanel.updateStatus("Move executed", Color.web("#27ae60"));
            
            gameReviewer.recordMove(board);
            
            if (networkHandler != null) {
                networkHandler.sendMove(move);
            }
            
            // Trigger AI move if needed
            if (shouldAIMoveNext() && aiIsThinking.compareAndSet(false, true)) {
                scheduleAIMove();
            }
        });
    }
    
    @Override
    public void onGameStateChanged() {
        if (isDisposed) return;
        
        Platform.runLater(() -> {
            boardRenderer.drawPieces();
            controlPanel.updateTurnLabel(board.isWhiteTurn);
            controlPanel.updateCheckStatus(board.whiteInCheck, board.blackInCheck);
            checkGameEnd();
        });
    }
    
    @Override
    public void onPieceSelected(Piece piece) {
        if (isDisposed) return;
        
        Platform.runLater(() -> {
            boardRenderer.setDraggedPiece(piece);
            controlPanel.updateStatus("Selected: " + piece.name, Color.web("#f39c12"));
        });
    }
    
    @Override
    public void onPieceDeselected() {
        if (isDisposed) return;
        
        Platform.runLater(() -> {
            boardRenderer.setDraggedPiece(null);
            boardRenderer.drawPieces();
            controlPanel.updateStatus("Ready", Color.web("#95a5a6"));
        });
    }
    
    @Override
    public void onInvalidMove(String reason) {
        if (isDisposed) return;
        
        Platform.runLater(() -> {
            controlPanel.updateStatus(reason, Color.web("#e74c3c"));
        });
    }
    
    @Override
    public void onPromotionNeeded(int col, int row) {
        // Handled by showPromotionDialog callback
    }
    
    // ==================== AI LOGIC ====================
    
    /**
     * Check if AI should move next
     */
    private boolean shouldAIMoveNext() {
        return gameMode == GameMode.VS_COMPUTER && 
               !board.isWhiteTurn && 
               board.gameState == GameState.PLAYING &&
               !aiIsThinking.get();
    }
    
    /**
     * Schedule AI move asynchronously
     */
    private void scheduleAIMove() {
        Platform.runLater(() -> {
            controlPanel.updateStatus("AI thinking...", Color.web("#f39c12"));
        });
        
        aiExecutor.submit(() -> {
            try {
                // Brief pause for realism
                Thread.sleep(Constants.AI_MIN_THINK_TIME);
                
                if (isDisposed) {
                    aiIsThinking.set(false);
                    return;
                }
                
                // Get AI move
                AIPlayer aiPlayer = new AIPlayer(aiCharacter);
                CompletableFuture<Move> moveFuture = aiPlayer.getMove(board);
                
                // Wait for move with timeout
                Move aiMove = null;
                try {
                    aiMove = moveFuture.get(
                        Constants.AI_MAX_THINK_TIME, 
                        TimeUnit.MILLISECONDS
                    );
                } catch (TimeoutException e) {
                    System.err.println("AI move timeout");
                } catch (ExecutionException e) {
                    System.err.println("AI execution error: " + e.getCause());
                }
                
                // Execute move on UI thread
                final Move finalMove = aiMove;
                if (finalMove != null && !isDisposed) {
                    Platform.runLater(() -> executeAIMove(finalMove));
                } else {
                    Platform.runLater(() -> {
                        controlPanel.updateStatus("AI cannot move", Color.RED);
                        aiIsThinking.set(false);
                    });
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    controlPanel.updateStatus("AI interrupted", Color.RED);
                    aiIsThinking.set(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    controlPanel.updateStatus("AI error: " + e.getMessage(), Color.RED);
                    aiIsThinking.set(false);
                });
            }
        });
    }
    
    /**
     * Execute AI move on the board
     */
    private void executeAIMove(Move move) {
        try {
            // Validate move object
            if (move == null) {
                controlPanel.updateStatus("Invalid AI move: null", Color.RED);
                aiIsThinking.set(false);
                return;
            }
            
            // Ensure piece reference exists
            if (move.piece == null) {
                Piece p = board.getPiece(move.fromCol, move.fromRow);
                if (p == null) {
                    controlPanel.updateStatus("Invalid AI move: no piece at source", Color.RED);
                    aiIsThinking.set(false);
                    return;
                }
                move.piece = p;
            }
            
            // Set selected piece
            board.selectedPiece = move.piece;
            
            // Execute move
            boolean success = board.movePiece(move.toCol, move.toRow);
            
            if (success) {
                // Handle pawn promotion
                if (move.piece.name.equals("Pawn") && 
                    (move.toRow == 0 || move.toRow == 7)) {
                    board.promotePawn(move.toCol, move.toRow, "Queen");
                    
                    if (!board.moveHistory.isEmpty()) {
                        board.moveHistory.get(board.moveHistory.size() - 1)
                            .promotionPiece = "Queen";
                    }
                }
                
                // Notify listeners
                onMoveExecuted(move);
                onGameStateChanged();
            } else {
                controlPanel.updateStatus("AI move failed", Color.RED);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            controlPanel.updateStatus("AI move error: " + e.getMessage(), Color.RED);
        } finally {
            aiIsThinking.set(false);
        }
    }
    
    // ==================== PROMOTION DIALOG ====================
    
    private void showPromotionDialog(int col, int row, 
                                    MoveExecutor.PromotionCallback callback) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Choose your promotion piece:");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < Constants.PROMOTION_PIECES.length; i++) {
            String piece = Constants.PROMOTION_PIECES[i];
            String symbol = Constants.PROMOTION_SYMBOLS[i];
            
            Button btn = new Button(symbol + "\n" + piece);
            btn.setMinSize(80, 80);
            btn.getStyleClass().add("promotion-piece");
            btn.setOnAction(e -> {
                dialog.setResult(piece);
                dialog.close();
            });
            grid.add(btn, i, 0);
        }
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.showAndWait().ifPresent(choice -> {
            callback.onPromotionSelected(choice);
            boardRenderer.drawPieces();
        });
    }
    
    // ==================== GAME END ====================
    
    private void checkGameEnd() {
        if (board.gameState == GameState.PLAYING) {
            return;
        }
        
        controlPanel.disableGameButtons();
        
        String result;
        Color color;
        
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
            default:
                return;
        }
        
        controlPanel.updateStatus(result, color);
        
        if (board.gameState != GameState.CANCELLED) {
            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(e -> showReviewDialog());
            pause.play();
        }
    }
    
    // ==================== BUTTON HANDLERS ====================
    
    private void handleSurrender() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Surrender");
        confirm.setHeaderText("Are you sure you want to surrender?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean whiteResigns = (gameMode == GameMode.NETWORK) ? 
                    isHost : board.isWhiteTurn;
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
            showAlert(Alert.AlertType.ERROR, "Cannot Cancel", 
                "Game cannot be cancelled after the first move!");
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
        // Set disposed flag
        isDisposed = true;
        aiIsThinking.set(false);
        
        // Shutdown AI executor
        aiExecutor.shutdown();
        try {
            if (!aiExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                aiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close network
        if (networkHandler != null) {
            try {
                networkHandler.close();
            } catch (Exception e) {
                System.err.println("Error closing network: " + e.getMessage());
            }
        }
        
        // Return to menu
        MenuView menuView = new MenuView(primaryStage);
        primaryStage.getScene().setRoot(menuView.getRoot());
    }
    
    // ==================== REVIEW & ANALYSIS ====================
    
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
    
    // ==================== NETWORK SUPPORT ====================
    
    /**
     * Receive move from network opponent
     */
    public void receiveMove(Move move) {
        Platform.runLater(() -> {
            if (isDisposed) return;
            
            Piece p = board.getPiece(move.fromCol, move.fromRow);
            if (p != null) {
                moveExecutor.tryMove(p, move.toCol, move.toRow);
                
                if (move.promotionPiece != null) {
                    board.promotePawn(move.toCol, move.toRow, move.promotionPiece);
                }
            }
        });
    }
    
    // ==================== UTILITIES ====================
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public Parent getRoot() {
        return root;
    }
}