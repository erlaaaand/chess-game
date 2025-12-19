package com.erland.chess.view.handlers;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.erland.chess.Constants;
import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.view.MenuView.GameMode;
import com.erland.chess.utils.PieceImageLoader;

/**
 * Handles move execution with proper validation and animation
 */
public class MoveExecutor {
    
    private final Board board;
    private final GameMode gameMode;
    private final boolean isHost;
    private final Pane dragLayer;
    
    private MoveListener moveListener;
    private PromotionHandler promotionHandler;
    
    // ==================== INTERFACES ====================
    
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
    
    // ==================== CONSTRUCTOR ====================
    
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
    
    // ==================== PERMISSION CHECKS ====================
    
    public boolean canPlayerMove() {
        if (board.gameState != GameState.PLAYING) {
            return false;
        }
        
        switch (gameMode) {
            case NETWORK:
                return (isHost && board.isWhiteTurn) || (!isHost && !board.isWhiteTurn);
            case VS_COMPUTER:
                return board.isWhiteTurn; // Player is always white
            default:
                return true; // Local multiplayer
        }
    }
    
    public boolean isPlayerPiece(Piece piece) {
        switch (gameMode) {
            case NETWORK:
                return (isHost && piece.isWhite) || (!isHost && !piece.isWhite);
            case VS_COMPUTER:
                return piece.isWhite;
            default:
                return piece.isWhite == board.isWhiteTurn;
        }
    }
    
    // ==================== MOVE EXECUTION ====================
    
    public boolean tryMove(Piece piece, int targetCol, int targetRow) {
        if (piece == null) {
            notifyInvalidMove("No piece selected");
            return false;
        }
        
        // Validation
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
            // Animate move first, then show promotion dialog
            animatePieceMove(piece.col, piece.row, targetCol, targetRow, piece, () -> {
                // Execute move on board
                board.movePiece(targetCol, targetRow);
                
                // Show promotion dialog
                handlePromotion(targetCol, targetRow);
            });
            return true;
        } else {
            // Normal move
            int fromCol = piece.col;
            int fromRow = piece.row;
            
            animatePieceMove(fromCol, fromRow, targetCol, targetRow, piece, () -> {
                // Execute move on board
                boolean success = board.movePiece(targetCol, targetRow);
                
                if (success && !board.moveHistory.isEmpty()) {
                    Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                    notifyMoveExecuted(lastMove);
                    notifyGameStateChanged();
                }
            });
            return true;
        }
    }
    
    // ==================== ANIMATION ====================
    
    private void animatePieceMove(int fromCol, int fromRow, int toCol, int toRow,
                                  Piece piece, Runnable onFinish) {
        double startX = fromCol * Constants.TILE_SIZE + 5;
        double startY = fromRow * Constants.TILE_SIZE + 5;
        double endX = toCol * Constants.TILE_SIZE + 5;
        double endY = toRow * Constants.TILE_SIZE + 5;
        
        Rectangle animRect = createPieceVisual(piece, startX, startY);
        dragLayer.getChildren().clear();
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
            dragLayer.getChildren().clear();
            if (onFinish != null) {
                onFinish.run();
            }
        });
        
        transition.play();
    }
    
    private Rectangle createPieceVisual(Piece piece, double x, double y) {
        Rectangle rect = new Rectangle(x, y, 
            Constants.TILE_SIZE - 10, Constants.TILE_SIZE - 10);
        
        try {
            javafx.scene.image.Image img = PieceImageLoader.getInstance()
                .getImage(piece.isWhite, piece.name);
            
            if (img != null) {
                rect.setFill(new javafx.scene.paint.ImagePattern(img));
            } else {
                rect.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
            }
        } catch (Exception e) {
            rect.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
        }
        
        rect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        return rect;
    }
    
    // ==================== PROMOTION ====================
    
    private void handlePromotion(int col, int row) {
        if (promotionHandler != null) {
            promotionHandler.showPromotionDialog(col, row, pieceType -> {
                board.promotePawn(col, row, pieceType);
                
                if (!board.moveHistory.isEmpty()) {
                    Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                    notifyMoveExecuted(lastMove);
                }
                notifyGameStateChanged();
            });
        } else {
            // Auto-promote to Queen if no handler
            board.promotePawn(col, row, "Queen");
            notifyGameStateChanged();
        }
    }
    
    // ==================== NOTIFICATIONS ====================
    
    private void notifyMoveExecuted(Move move) {
        if (moveListener != null) {
            moveListener.onMoveExecuted(move);
        }
    }
    
    private void notifyGameStateChanged() {
        if (moveListener != null) {
            moveListener.onGameStateChanged();
        }
    }
    
    private void notifyInvalidMove(String reason) {
        if (moveListener != null) {
            moveListener.onInvalidMove(reason);
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
}