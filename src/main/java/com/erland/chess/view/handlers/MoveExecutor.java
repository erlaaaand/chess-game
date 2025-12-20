package com.erland.chess.view.handlers;

import com.erland.chess.Constants;
import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.utils.PieceImageLoader;
import com.erland.chess.view.MenuView.GameMode;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * FIXED: Better null safety, improved animation handling, proper state management
 */
public class MoveExecutor {
    
    private final Board board;
    private final GameMode gameMode;
    private final boolean isHost;
    private final Pane dragLayer;
    
    private MoveListener moveListener;
    private PromotionHandler promotionHandler;
    
    private volatile boolean isAnimating = false;
    
    public interface MoveListener {
        void onMoveExecuted(Move move);
        void onGameStateChanged();
        void onPieceSelected(Piece piece);
        void onPieceDeselected();
        void onInvalidMove(String reason);
        void onPromotionNeeded(int col, int row);
    }
    
    public interface PromotionHandler {
        void showPromotionDialog(int col, int row, PromotionCallback callback);
    }
    
    public interface PromotionCallback {
        void onPromotionSelected(String pieceType);
    }
    
    public MoveExecutor(Board board, GameMode gameMode, boolean isHost, Pane dragLayer) {
        this.board = board;
        this.gameMode = gameMode;
        this.isHost = isHost;
        this.dragLayer = dragLayer;
    }
    
    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
    }
    
    public void setPromotionHandler(PromotionHandler handler) {
        this.promotionHandler = handler;
    }
    
    public boolean canPlayerMove() {
        if (board.gameState != GameState.PLAYING) {
            return false;
        }
        
        if (isAnimating) {
            return false;
        }
        
        switch (gameMode) {
            case NETWORK:
                return (isHost && board.isWhiteTurn) || (!isHost && !board.isWhiteTurn);
            case VS_COMPUTER:
                return board.isWhiteTurn;
            default:
                return true;
        }
    }
    
    public boolean isPlayerPiece(Piece piece) {
        if (piece == null) return false;
        
        switch (gameMode) {
            case NETWORK:
                return (isHost && piece.isWhite) || (!isHost && !piece.isWhite);
            case VS_COMPUTER:
                return piece.isWhite;
            default:
                return piece.isWhite == board.isWhiteTurn;
        }
    }
    
    /**
     * FIXED: Better validation and null safety
     */
    public boolean tryMove(Piece piece, int targetCol, int targetRow) {
        if (piece == null) {
            notifyInvalidMove("No piece selected");
            return false;
        }
        
        if (isAnimating) {
            notifyInvalidMove("Please wait for animation to complete");
            return false;
        }
        
        // Validation
        if (!isValidSquare(targetCol, targetRow)) {
            notifyInvalidMove("Target square out of bounds");
            return false;
        }
        
        if (!piece.canMove(targetCol, targetRow)) {
            notifyInvalidMove("Invalid move for " + piece.name);
            return false;
        }
        
        if (board.wouldBeInCheckAfterMove(piece, targetCol, targetRow)) {
            notifyInvalidMove("Move would leave king in check");
            return false;
        }
        
        // Check for pawn promotion
        boolean isPromotion = piece.name.equals("Pawn") && 
                             (targetRow == 0 || targetRow == 7);
        
        if (isPromotion) {
            handlePromotionMove(piece, targetCol, targetRow);
            return true;
        } else {
            handleNormalMove(piece, targetCol, targetRow);
            return true;
        }
    }
    
    /**
     * FIXED: Proper promotion handling with animation
     */
    private void handlePromotionMove(Piece piece, int targetCol, int targetRow) {
        int fromCol = piece.col;
        int fromRow = piece.row;
        
        isAnimating = true;
        
        animatePieceMove(fromCol, fromRow, targetCol, targetRow, piece, () -> {
            // Store piece reference before move
            final Piece movedPiece = piece;
            
            // Set board state
            board.selectedPiece = movedPiece;
            
            // Execute move
            boolean success = board.movePiece(targetCol, targetRow);
            
            if (success) {
                // Show promotion dialog
                handlePromotion(targetCol, targetRow);
            }
            
            isAnimating = false;
        });
    }
    
    /**
     * FIXED: Safer normal move handling
     */
    private void handleNormalMove(Piece piece, int targetCol, int targetRow) {
        int fromCol = piece.col;
        int fromRow = piece.row;
        
        isAnimating = true;
        
        animatePieceMove(fromCol, fromRow, targetCol, targetRow, piece, () -> {
            // Store piece reference
            final Piece movedPiece = piece;
            
            // Set board state
            board.selectedPiece = movedPiece;
            
            // Execute move
            boolean success = board.movePiece(targetCol, targetRow);
            
            if (success && !board.moveHistory.isEmpty()) {
                Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                notifyMoveExecuted(lastMove);
                notifyGameStateChanged();
            } else {
                notifyInvalidMove("Move execution failed");
            }
            
            isAnimating = false;
        });
    }
    
    /**
     * FIXED: Smoother animation with better error handling
     */
    private void animatePieceMove(int fromCol, int fromRow, int toCol, int toRow,
                                  Piece piece, Runnable onFinish) {
        try {
            double startX = fromCol * Constants.TILE_SIZE + 5;
            double startY = fromRow * Constants.TILE_SIZE + 5;
            double endX = toCol * Constants.TILE_SIZE + 5;
            double endY = toRow * Constants.TILE_SIZE + 5;
            
            // Create visual di Thread UI
            Platform.runLater(() -> {
                Rectangle animRect = createPieceVisual(piece, startX, startY);
                dragLayer.getChildren().clear(); // Bersihkan sisa drag sebelumnya
                dragLayer.getChildren().add(animRect);
                
                TranslateTransition transition = new TranslateTransition(
                    Duration.millis(Constants.MOVE_ANIMATION_DURATION), animRect
                );
                transition.setFromX(0);
                transition.setFromY(0);
                transition.setToX(endX - startX);
                transition.setToY(endY - startY);
                transition.setInterpolator(Interpolator.EASE_BOTH);
                
                transition.setOnFinished(e -> {
                    dragLayer.getChildren().clear(); // Hapus visual animasi
                    if (onFinish != null) {
                        onFinish.run(); // Jalankan callback (update model & redraw board)
                    }
                });
                
                transition.play();
            });
            
        } catch (Exception e) {
            // Fallback jika error animasi
            Platform.runLater(() -> {
                dragLayer.getChildren().clear();
                if (onFinish != null) onFinish.run();
            });
        }
    }
    
    /**
     * FIXED: Better visual creation with error handling
     */
    private Rectangle createPieceVisual(Piece piece, double x, double y) {
        Rectangle rect = new Rectangle(x, y, 
            Constants.TILE_SIZE - 10, Constants.TILE_SIZE - 10);
        
        try {
            javafx.scene.image.Image img = PieceImageLoader.getInstance()
                .getImage(piece.isWhite, piece.name);
            
            if (img != null) {
                rect.setFill(new javafx.scene.paint.ImagePattern(img));
            } else {
                // Fallback color
                rect.setFill(piece.isWhite ? Color.LIGHTGRAY : Color.DARKGRAY);
                rect.setStroke(piece.isWhite ? Color.BLACK : Color.WHITE);
                rect.setStrokeWidth(2);
            }
        } catch (Exception e) {
            System.err.println("Failed to load piece image: " + e.getMessage());
            rect.setFill(piece.isWhite ? Color.LIGHTGRAY : Color.DARKGRAY);
        }
        
        rect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        return rect;
    }
    
    private void handlePromotion(int col, int row) {
        if (promotionHandler != null) {
            Platform.runLater(() -> {
                promotionHandler.showPromotionDialog(col, row, pieceType -> {
                    board.promotePawn(col, row, pieceType);
                    
                    if (!board.moveHistory.isEmpty()) {
                        Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                        notifyMoveExecuted(lastMove);
                    }
                    notifyGameStateChanged();
                });
            });
        } else {
            // Auto-promote to Queen
            board.promotePawn(col, row, "Queen");
            
            if (!board.moveHistory.isEmpty()) {
                Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                notifyMoveExecuted(lastMove);
            }
            notifyGameStateChanged();
        }
    }
    
    private void notifyMoveExecuted(Move move) {
        if (moveListener != null) {
            Platform.runLater(() -> moveListener.onMoveExecuted(move));
        }
    }
    
    private void notifyGameStateChanged() {
        if (moveListener != null) {
            Platform.runLater(() -> moveListener.onGameStateChanged());
        }
    }
    
    private void notifyInvalidMove(String reason) {
        if (moveListener != null) {
            Platform.runLater(() -> moveListener.onInvalidMove(reason));
        }
    }
    
    public void onPieceSelected(Piece piece) {
        if (moveListener != null) {
            moveListener.onPieceSelected(piece);
        }
    }
    
    public void onPieceDeselected() {
        if (moveListener != null) {
            moveListener.onPieceDeselected();
        }
    }
    
    private boolean isValidSquare(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }
    
    public boolean isAnimating() {
        return isAnimating;
    }
}