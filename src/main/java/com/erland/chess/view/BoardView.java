package com.erland.chess.view;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.pieces.*;
import com.erland.chess.network.NetworkHandler;
import com.erland.chess.review.GameReviewer;
import com.erland.chess.view.MenuView.GameMode;

import java.util.HashMap;
import java.util.Map;

public class BoardView {
    private static final int TILE_SIZE = 85;
    private static final int BOARD_SIZE = TILE_SIZE * 8;
    
    private Stage primaryStage;
    private BorderPane root;
    private Pane boardPane;
    private Canvas boardCanvas;
    private Canvas pieceCanvas;
    private VBox controlPanel;
    
    private Board board;
    private GameMode gameMode;
    private NetworkHandler networkHandler;
    private boolean isHost;
    private GameReviewer gameReviewer;
    
    // UI Components
    private Label turnLabel;
    private Label statusLabel;
    private Label checkLabel;
    private Button btnSurrender;
    private Button btnCancel;
    private Button btnMenu;
    private TextArea moveLog;
    
    // Piece images cache
    private Map<String, Image> pieceImages = new HashMap<>();
    
    // Animation
    private Piece draggedPiece = null;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private Pane dragLayer;
    private Rectangle draggedPieceRect;
    
    // Highlighting
    private Pane highlightLayer;
    
    public BoardView(Stage stage, GameMode mode, Object network, boolean isHost) {
        this.primaryStage = stage;
        this.gameMode = mode;
        this.isHost = isHost;
        this.board = new Board();
        this.gameReviewer = new GameReviewer();
        
        if (network instanceof com.erland.chess.network.GameServer) {
            this.networkHandler = (com.erland.chess.network.GameServer) network;
        } else if (network instanceof com.erland.chess.network.GameClient) {
            this.networkHandler = (com.erland.chess.network.GameClient) network;
        }
        
        loadPieceImages();
        createUI();
        
        if (networkHandler != null) {
            networkHandler.setBoardPanel(this);
        }
        
        // Start live analysis
        gameReviewer.startNewGame();
        System.out.println("Live game analysis started!");
    }
    
    private void loadPieceImages() {
        String[] colors = {"w", "b"};
        String[] pieces = {"king", "queen", "rook", "bishop", "knight", "pawn"};
        
        for (String color : colors) {
            for (String piece : pieces) {
                String key = color + "_" + piece;
                try {
                    Image img = new Image(getClass().getResourceAsStream(
                        "/images/" + key + ".png"), TILE_SIZE - 10, TILE_SIZE - 10, true, true);
                    pieceImages.put(key, img);
                } catch (Exception e) {
                    System.err.println("Failed to load image: " + key);
                }
            }
        }
    }
    
    private void createUI() {
        root = new BorderPane();
        root.getStyleClass().add("board-container");
        
        // Center: Chess board
        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        
        // Board canvas (tiles)
        boardCanvas = new Canvas(BOARD_SIZE, BOARD_SIZE);
        drawBoard();
        
        // Highlight layer (valid moves, selected piece)
        highlightLayer = new Pane();
        highlightLayer.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        highlightLayer.setMouseTransparent(true);
        
        // Piece canvas
        pieceCanvas = new Canvas(BOARD_SIZE, BOARD_SIZE);
        pieceCanvas.setOnMousePressed(this::handleMousePressed);
        pieceCanvas.setOnMouseDragged(this::handleMouseDragged);
        pieceCanvas.setOnMouseReleased(this::handleMouseReleased);
        drawPieces();
        
        // Drag layer (for animated piece)
        dragLayer = new Pane();
        dragLayer.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        dragLayer.setMouseTransparent(true);
        
        boardPane.getChildren().addAll(boardCanvas, highlightLayer, pieceCanvas, dragLayer);
        
        // Add coordinates
        addCoordinates();
        
        root.setCenter(boardPane);
        
        // Right: Control panel
        createControlPanel();
        root.setRight(controlPanel);
    }
    
    private void drawBoard() {
        GraphicsContext gc = boardCanvas.getGraphicsContext2D();
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 0) {
                    gc.setFill(Color.web("#f0d9b5"));
                } else {
                    gc.setFill(Color.web("#b58863"));
                }
                gc.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }
    
    private void drawPieces() {
        GraphicsContext gc = pieceCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, BOARD_SIZE, BOARD_SIZE);
        
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                Piece piece = board.getPiece(col, row);
                if (piece != null && piece != draggedPiece) {
                    drawPiece(gc, piece, col, row);
                }
            }
        }
    }
    
    private void drawPiece(GraphicsContext gc, Piece piece, int col, int row) {
        String color = piece.isWhite ? "w" : "b";
        String pieceName = piece.name.toLowerCase();
        String key = color + "_" + pieceName;
        
        Image img = pieceImages.get(key);
        if (img != null) {
            double x = col * TILE_SIZE + 5;
            double y = row * TILE_SIZE + 5;
            gc.drawImage(img, x, y);
        } else {
            // Fallback: draw text
            gc.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
            gc.setStroke(piece.isWhite ? Color.BLACK : Color.WHITE);
            gc.setLineWidth(2);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 48));
            String symbol = getPieceSymbol(piece);
            gc.strokeText(symbol, col * TILE_SIZE + 20, row * TILE_SIZE + 60);
            gc.fillText(symbol, col * TILE_SIZE + 20, row * TILE_SIZE + 60);
        }
    }
    
    private String getPieceSymbol(Piece piece) {
        String symbol = "";
        if (piece instanceof King) symbol = "♔";
        else if (piece instanceof Queen) symbol = "♕";
        else if (piece instanceof Rook) symbol = "♖";
        else if (piece instanceof Bishop) symbol = "♗";
        else if (piece instanceof Knight) symbol = "♘";
        else if (piece instanceof Pawn) symbol = "♙";
        return piece.isWhite ? symbol : symbol.toLowerCase();
    }
    
    private void addCoordinates() {
        // Files (a-h)
        HBox filesBox = new HBox();
        filesBox.setAlignment(Pos.CENTER);
        filesBox.setSpacing(TILE_SIZE - 15);
        filesBox.setPadding(new Insets(0, 0, 0, 10));
        for (char c = 'a'; c <= 'h'; c++) {
            Label label = new Label(String.valueOf(c));
            label.getStyleClass().add("coordinate-label");
            label.setMinWidth(15);
            filesBox.getChildren().add(label);
        }
        
        // Ranks (1-8)
        VBox ranksBox = new VBox();
        ranksBox.setAlignment(Pos.CENTER);
        ranksBox.setSpacing(TILE_SIZE - 23);
        ranksBox.setPadding(new Insets(10, 0, 0, 0));
        for (int i = 8; i >= 1; i--) {
            Label label = new Label(String.valueOf(i));
            label.getStyleClass().add("coordinate-label");
            label.setMinHeight(15);
            ranksBox.getChildren().add(label);
        }
        
        VBox leftContainer = new VBox();
        leftContainer.getChildren().add(ranksBox);
        VBox bottomContainer = new VBox();
        bottomContainer.getChildren().add(filesBox);
        
        BorderPane coordinates = new BorderPane();
        coordinates.setCenter(boardPane);
        coordinates.setLeft(leftContainer);
        coordinates.setBottom(bottomContainer);
        
        root.setCenter(coordinates);
    }
    
    private void handleMousePressed(MouseEvent e) {
        if (board.gameState != GameState.PLAYING) return;
        
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        if (col < 0 || col >= 8 || row < 0 || row >= 8) return;
        
        // Check turn restrictions
        if (gameMode == GameMode.NETWORK) {
            if ((isHost && !board.isWhiteTurn) || (!isHost && board.isWhiteTurn)) {
                updateStatus("Not your turn!", Color.web("#e74c3c"));
                return;
            }
        }
        
        if (gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn) {
            updateStatus("Computer is thinking...", Color.web("#f39c12"));
            return;
        }
        
        Piece clickedPiece = board.getPiece(col, row);
        
        // If no piece selected, try to select
        if (board.selectedPiece == null) {
            if (clickedPiece != null && clickedPiece.isWhite == board.isWhiteTurn) {
                board.selectedPiece = clickedPiece;
                draggedPiece = clickedPiece;
                dragOffsetX = e.getX() - col * TILE_SIZE;
                dragOffsetY = e.getY() - row * TILE_SIZE;
                updateStatus("Selected: " + clickedPiece.name, Color.web("#f39c12"));
                updateHighlights();
                drawPieces();
                createDraggedPieceVisual(clickedPiece, e.getX() - dragOffsetX, e.getY() - dragOffsetY);
            }
        } else {
            // If clicking another own piece, switch selection
            if (clickedPiece != null && clickedPiece.isWhite == board.isWhiteTurn && 
                clickedPiece != board.selectedPiece) {
                board.selectedPiece = clickedPiece;
                draggedPiece = clickedPiece;
                dragOffsetX = e.getX() - col * TILE_SIZE;
                dragOffsetY = e.getY() - row * TILE_SIZE;
                updateStatus("Selected: " + clickedPiece.name, Color.web("#f39c12"));
                updateHighlights();
                drawPieces();
                createDraggedPieceVisual(clickedPiece, e.getX() - dragOffsetX, e.getY() - dragOffsetY);
            } else {
                // Try to move
                attemptMove(col, row);
            }
        }
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (draggedPiece != null && draggedPieceRect != null) {
            draggedPieceRect.setLayoutX(e.getX() - dragOffsetX);
            draggedPieceRect.setLayoutY(e.getY() - dragOffsetY);
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        if (draggedPiece == null) return;
        
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        // Remove drag visual
        dragLayer.getChildren().clear();
        draggedPieceRect = null;
        
        if (col >= 0 && col < 8 && row >= 0 && row < 8) {
            attemptMove(col, row);
        } else {
            // Invalid drop, cancel
            board.selectedPiece = null;
            draggedPiece = null;
            updateHighlights();
            drawPieces();
        }
    }
    
    // Continued in Part 2...
    
    public Parent getRoot() {
        return root;
    }
    
    public void setBoardPanel(BoardView panel) {
        // For network handler compatibility
    }

    // Continuation of BoardView.java

    private void createDraggedPieceVisual(Piece piece, double x, double y) {
        String color = piece.isWhite ? "w" : "b";
        String pieceName = piece.name.toLowerCase();
        String key = color + "_" + pieceName;
        
        Image img = pieceImages.get(key);
        if (img != null) {
            draggedPieceRect = new Rectangle(x, y, TILE_SIZE - 10, TILE_SIZE - 10);
            draggedPieceRect.setFill(new javafx.scene.paint.ImagePattern(img));
            draggedPieceRect.setEffect(new javafx.scene.effect.DropShadow(15, Color.BLACK));
            dragLayer.getChildren().add(draggedPieceRect);
        }
    }
    
    private void attemptMove(int targetCol, int targetRow) {
        if (board.selectedPiece == null) return;
        
        Piece movingPiece = board.selectedPiece;
        int fromCol = movingPiece.col;
        int fromRow = movingPiece.row;
        
        if (board.movePiece(targetCol, targetRow)) {
            // Animate move
            animatePieceMove(fromCol, fromRow, targetCol, targetRow, movingPiece, () -> {
                // Check for pawn promotion
                if (movingPiece instanceof Pawn && (targetRow == 0 || targetRow == 7)) {
                    showPromotionDialog(targetCol, targetRow);
                }
                
                // Update game state
                gameReviewer.recordMove(board);
                updateMoveLog();
                updateTurnLabel();
                updateCheckStatus();
                updateStatus("Move executed", Color.web("#27ae60"));
                
                // Send move to network
                if (networkHandler != null && !board.moveHistory.isEmpty()) {
                    networkHandler.sendMove(board.moveHistory.get(board.moveHistory.size() - 1));
                }
                
                // Computer move
                if (gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn && 
                    board.gameState == GameState.PLAYING) {
                    updateStatus("Computer thinking...", Color.web("#f39c12"));
                    
                    PauseTransition pause = new PauseTransition(Duration.millis(800));
                    pause.setOnFinished(evt -> {
                        board.performComputerMove();
                        gameReviewer.recordMove(board);
                        updateMoveLog();
                        updateTurnLabel();
                        updateCheckStatus();
                        checkGameEnd();
                        drawPieces();
                    });
                    pause.play();
                }
                
                checkGameEnd();
            });
        } else {
            // Invalid move
            board.selectedPiece = null;
            draggedPiece = null;
            updateStatus("Invalid move!", Color.web("#e74c3c"));
            updateHighlights();
            drawPieces();
        }
    }
    
    private void animatePieceMove(int fromCol, int fromRow, int toCol, int toRow, 
                                  Piece piece, Runnable onFinish) {
        double startX = fromCol * TILE_SIZE + 5;
        double startY = fromRow * TILE_SIZE + 5;
        double endX = toCol * TILE_SIZE + 5;
        double endY = toRow * TILE_SIZE + 5;
        
        // Create temporary piece visual
        String color = piece.isWhite ? "w" : "b";
        String pieceName = piece.name.toLowerCase();
        String key = color + "_" + pieceName;
        Image img = pieceImages.get(key);
        
        Rectangle animRect = new Rectangle(startX, startY, TILE_SIZE - 10, TILE_SIZE - 10);
        if (img != null) {
            animRect.setFill(new javafx.scene.paint.ImagePattern(img));
        }
        animRect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        
        dragLayer.getChildren().clear();
        dragLayer.getChildren().add(animRect);
        
        // Animate
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), animRect);
        transition.setFromX(0);
        transition.setFromY(0);
        transition.setToX(endX - startX);
        transition.setToY(endY - startY);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        
        transition.setOnFinished(e -> {
            dragLayer.getChildren().clear();
            draggedPiece = null;
            board.selectedPiece = null;
            updateHighlights();
            drawPieces();
            if (onFinish != null) onFinish.run();
        });
        
        transition.play();
    }
    
    private void updateHighlights() {
        highlightLayer.getChildren().clear();
        
        if (board.selectedPiece == null) return;
        
        // Highlight selected tile
        Rectangle selectedRect = new Rectangle(
            board.selectedPiece.col * TILE_SIZE, 
            board.selectedPiece.row * TILE_SIZE, 
            TILE_SIZE, TILE_SIZE
        );
        selectedRect.setFill(Color.web("#f6f669", 0.5));
        highlightLayer.getChildren().add(selectedRect);
        
        // Highlight valid moves
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (board.selectedPiece.canMove(col, row) && 
                    !board.wouldBeInCheckAfterMove(board.selectedPiece, col, row)) {
                    
                    Piece target = board.getPiece(col, row);
                    if (target != null) {
                        // Capture indicator (red circle)
                        Circle circle = new Circle(
                            col * TILE_SIZE + TILE_SIZE / 2.0, 
                            row * TILE_SIZE + TILE_SIZE / 2.0, 
                            TILE_SIZE / 3.0
                        );
                        circle.setFill(Color.web("#e74c3c", 0.4));
                        circle.setStroke(Color.web("#c0392b"));
                        circle.setStrokeWidth(2);
                        highlightLayer.getChildren().add(circle);
                    } else {
                        // Regular move indicator (green dot)
                        Circle dot = new Circle(
                            col * TILE_SIZE + TILE_SIZE / 2.0, 
                            row * TILE_SIZE + TILE_SIZE / 2.0, 
                            TILE_SIZE / 6.0
                        );
                        dot.setFill(Color.web("#27ae60", 0.6));
                        highlightLayer.getChildren().add(dot);
                    }
                }
            }
        }
        
        // Highlight king in check
        if (board.whiteInCheck || board.blackInCheck) {
            King king = board.whiteInCheck ? 
                findKing(true) : findKing(false);
            if (king != null) {
                Rectangle checkRect = new Rectangle(
                    king.col * TILE_SIZE, king.row * TILE_SIZE, 
                    TILE_SIZE, TILE_SIZE
                );
                checkRect.setFill(Color.web("#e74c3c", 0.5));
                checkRect.setEffect(new javafx.scene.effect.Glow(0.8));
                highlightLayer.getChildren().add(checkRect);
            }
        }
    }
    
    private King findKing(boolean isWhite) {
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = board.getPiece(c, r);
                if (p instanceof King && p.isWhite == isWhite) {
                    return (King) p;
                }
            }
        }
        return null;
    }
    
    private void showPromotionDialog(int col, int row) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Choose promotion piece:");
        
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
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(choice -> {
            board.promotePawn(col, row, choice);
            drawPieces();
            updateCheckStatus();
            checkGameEnd();
        });
    }
    
    private void createControlPanel() {
        controlPanel = new VBox(15);
        controlPanel.getStyleClass().add("control-panel");
        controlPanel.setPadding(new Insets(20));
        controlPanel.setPrefWidth(300);
        controlPanel.setAlignment(Pos.TOP_CENTER);
        
        // Title
        Label title = new Label("♔ CHESS GAME ♔");
        title.getStyleClass().add("panel-title");
        
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
        btnSurrender.setOnAction(e -> surrender());
        
        btnCancel = new Button("Cancel Game");
        btnCancel.getStyleClass().addAll("game-button", "cancel-button");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> cancelGame());
        
        btnMenu = new Button("Back to Menu");
        btnMenu.getStyleClass().add("game-button");
        btnMenu.setMaxWidth(Double.MAX_VALUE);
        btnMenu.setOnAction(e -> backToMenu());
        
        // Move log
        Label logLabel = new Label("Move History:");
        logLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        moveLog = new TextArea();
        moveLog.getStyleClass().add("move-log");
        moveLog.setEditable(false);
        moveLog.setPrefHeight(300);
        moveLog.setWrapText(true);
        
        controlPanel.getChildren().addAll(
            title, turnLabel, checkLabel, statusLabel,
            new Separator(), btnSurrender, btnCancel, btnMenu,
            new Separator(), logLabel, moveLog
        );
    }
    
    private void updateTurnLabel() {
        turnLabel.setText("Turn: " + (board.isWhiteTurn ? "White" : "Black"));
        btnCancel.setDisable(!board.canCancelGame());
    }
    
    private void updateCheckStatus() {
        if (board.whiteInCheck) {
            checkLabel.setText("⚠ WHITE IN CHECK! ⚠");
            checkLabel.setTextFill(Color.web("#e74c3c"));
        } else if (board.blackInCheck) {
            checkLabel.setText("⚠ BLACK IN CHECK! ⚠");
            checkLabel.setTextFill(Color.web("#e74c3c"));
        } else {
            checkLabel.setText("");
        }
        updateHighlights();
    }
    
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
    
    private void updateMoveLog() {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < board.moveHistory.size(); i++) {
            if (i % 2 == 0) {
                log.append(String.format("%2d. ", (i / 2 + 1)));
            }
            log.append(board.moveHistory.get(i).toNotation()).append(" ");
            if (i % 2 == 1) {
                log.append("\n");
            }
        }
        moveLog.setText(log.toString());
        moveLog.setScrollTop(Double.MAX_VALUE);
    }
    
    private void surrender() {
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
                drawPieces();
            }
        });
    }
    
    private void cancelGame() {
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
                backToMenu();
            }
        });
    }
    
    private void checkGameEnd() {
        if (board.gameState != GameState.PLAYING) {
            btnSurrender.setDisable(true);
            btnCancel.setDisable(true);
            
            String result = "";
            Color color = Color.WHITE;
            
            switch (board.gameState) {
                case WHITE_WON:
                    result = board.blackInCheck ? "♔ CHECKMATE - White Wins! ♔" : "♔ White Wins! ♔";
                    color = Color.web("#87ceeb");
                    break;
                case BLACK_WON:
                    result = board.whiteInCheck ? "♚ CHECKMATE - Black Wins! ♚" : "♚ Black Wins! ♚";
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
            
            updateStatus(result, color);
            checkLabel.setText("");
            
            if (board.gameState != GameState.CANCELLED) {
                PauseTransition pause = new PauseTransition(Duration.millis(1500));
                pause.setOnFinished(e -> showReviewDialog());
                pause.play();
            }
        }
    }
    
    private void showReviewDialog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Game Review");
        confirm.setHeaderText("Game finished!");
        confirm.setContentText("Would you like to add a review comment and see analysis?");
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
    
    private void backToMenu() {
        if (networkHandler != null) {
            try {
                networkHandler.close();
            } catch (Exception e) {
                System.err.println("Error closing network: " + e.getMessage());
            }
        }
        
        MenuView menuView = new MenuView(primaryStage);
        primaryStage.getScene().setRoot(menuView.getRoot());
    }
    
    public void receiveMove(Board.Move move) {
        javafx.application.Platform.runLater(() -> {
            Piece p = board.getPiece(move.fromCol, move.fromRow);
            if (p != null) {
                board.selectedPiece = p;
                if (board.movePiece(move.toCol, move.toRow)) {
                    if (move.promotionPiece != null) {
                        board.promotePawn(move.toCol, move.toRow, move.promotionPiece);
                    }
                    gameReviewer.recordMove(board);
                    updateMoveLog();
                    updateTurnLabel();
                    updateCheckStatus();
                    checkGameEnd();
                    drawPieces();
                }
            }
        });
    }
}
