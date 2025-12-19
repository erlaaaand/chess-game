package com.erland.chess.view.handlers;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.model.pieces.Pawn;
import com.erland.chess.view.MenuView.GameMode;

/**
 * Handles move execution, validation, and animation
 */
public class MoveExecutor {
    private static final int TILE_SIZE = 85;
    
    private final Board board;
    private final GameMode gameMode;
    private final boolean isHost;
    private final Pane dragLayer;
    
    private MoveListener moveListener;
    private PromotionHandler promotionHandler;
    
    public interface MoveListener {
        void onMoveExecuted(Board.Move move);
        void onPromotionNeeded(int col, int row);
        void onGameStateChanged();
        void onPieceSelected(Piece piece);
        void onPieceDeselected();
        void onInvalidMove(String reason);
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
        
        if (gameMode == GameMode.NETWORK) {
            return (isHost && board.isWhiteTurn) || (!isHost && !board.isWhiteTurn);
        }
        
        if (gameMode == GameMode.VS_COMPUTER) {
            return board.isWhiteTurn;
        }
        
        return true;
    }
    
    public boolean isPlayerPiece(Piece piece) {
        if (gameMode == GameMode.NETWORK) {
            boolean isWhitePiece = piece.isWhite;
            return (isHost && isWhitePiece) || (!isHost && !isWhitePiece);
        }
        
        if (gameMode == GameMode.VS_COMPUTER) {
            return piece.isWhite;
        }
        
        return piece.isWhite == board.isWhiteTurn;
    }
    
    public boolean tryMove(Piece piece, int targetCol, int targetRow) {
        if (piece == null) {
            notifyInvalidMove("No piece selected");
            return false;
        }
        
        if (!piece.canMove(targetCol, targetRow)) {
            notifyInvalidMove("Invalid move");
            return false;
        }
        
        if (board.wouldBeInCheckAfterMove(piece, targetCol, targetRow)) {
            notifyInvalidMove("Move would leave king in check");
            return false;
        }
        
        // Execute move with animation
        executeMove(piece, targetCol, targetRow);
        return true;
    }
    
    private void executeMove(Piece piece, int targetCol, int targetRow) {
        int fromCol = piece.col;
        int fromRow = piece.row;
        
        // Animate the move
        animatePieceMove(fromCol, fromRow, targetCol, targetRow, piece, () -> {
            // After animation completes, update board state
            board.selectedPiece = piece;
            
            if (board.movePiece(targetCol, targetRow)) {
                Board.Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                
                // Check for pawn promotion
                if (piece instanceof Pawn && (targetRow == 0 || targetRow == 7)) {
                    handlePromotion(targetCol, targetRow);
                } else {
                    // No promotion needed, notify listeners
                    notifyMoveExecuted(lastMove);
                    notifyGameStateChanged();
                }
            }
        });
    }
    
    private void animatePieceMove(int fromCol, int fromRow, int toCol, int toRow,
                                  Piece piece, Runnable onFinish) {
        double startX = fromCol * TILE_SIZE + 5;
        double startY = fromRow * TILE_SIZE + 5;
        double endX = toCol * TILE_SIZE + 5;
        double endY = toRow * TILE_SIZE + 5;
        
        // Create visual for animation
        Rectangle animRect = new Rectangle(startX, startY, TILE_SIZE - 10, TILE_SIZE - 10);
        
        String color = piece.isWhite ? "w" : "b";
        String pieceName = piece.name.toLowerCase();
        
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/images/" + color + "_" + pieceName + ".png"),
                TILE_SIZE - 10, TILE_SIZE - 10, true, true
            );
            animRect.setFill(new javafx.scene.paint.ImagePattern(img));
        } catch (Exception e) {
            animRect.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
        }
        
        animRect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        dragLayer.getChildren().clear();
        dragLayer.getChildren().add(animRect);
        
        // Create smooth animation
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), animRect);
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
    
    private void handlePromotion(int col, int row) {
        if (promotionHandler != null) {
            promotionHandler.showPromotionDialog(col, row, (pieceType) -> {
                board.promotePawn(col, row, pieceType);
                
                // Update last move with promotion info
                if (!board.moveHistory.isEmpty()) {
                    Board.Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                    notifyMoveExecuted(lastMove);
                }
                
                notifyGameStateChanged();
            });
        }
    }
    
    private void notifyMoveExecuted(Board.Move move) {
        if (moveListener != null) {
            moveListener.onMoveExecuted(move);
        }
    }
    
    private void notifyGameStateChanged() {
        if (moveListener != null) {
            moveListener.onGameStateChanged();
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
    
    private void notifyInvalidMove(String reason) {
        if (moveListener != null) {
            moveListener.onInvalidMove(reason);
        }
    }
}