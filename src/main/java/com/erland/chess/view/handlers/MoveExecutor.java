package com.erland.chess.view.handlers;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.erland.chess.core.GameEngine; // Gunakan Engine
import com.erland.chess.core.MoveResult;
import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.view.MenuView.GameMode;
import com.erland.chess.utils.PieceImageLoader; // Gunakan loader

/**
 * Handles move execution, validation, and animation via GameEngine
 */
public class MoveExecutor {
    private static final int TILE_SIZE = 85;
    
    private final Board board; // Masih butuh board untuk cek turn/state sederhana
    private final GameEngine gameEngine; // Tambahkan Engine
    private final GameMode gameMode;
    private final boolean isHost;
    private final Pane dragLayer;
    
    private MoveListener moveListener;
    private PromotionHandler promotionHandler;
    
    public interface MoveListener {
        void onMoveExecuted(Board.Move move);
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
    
    // Constructor Updated: Menerima GameEngine
    public MoveExecutor(Board board, GameEngine engine, GameMode gameMode, boolean isHost, Pane dragLayer) {
        this.board = board;
        this.gameEngine = engine;
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
        if (board.gameState != GameState.PLAYING) return false;
        
        if (gameMode == GameMode.NETWORK) {
            return (isHost && board.isWhiteTurn) || (!isHost && !board.isWhiteTurn);
        }
        if (gameMode == GameMode.VS_COMPUTER) {
            return board.isWhiteTurn; // Player selalu putih vs Komputer (default)
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

        // Gunakan GameEngine untuk eksekusi
        MoveResult result = gameEngine.executeMove(piece, targetCol, targetRow);
        
        if (result.isSuccess()) {
            if (result.needsPromotion()) {
                // Handle visual move first (tanpa logic board)
                animatePieceMove(piece.col, piece.row, targetCol, targetRow, piece, () -> {
                    // Tampilkan dialog promosi
                    handlePromotion(targetCol, targetRow); 
                });
                return true; 
            } else {
                // Gerakan sukses normal
                animatePieceMove(piece.col, piece.row, targetCol, targetRow, piece, () -> {
                   notifyMoveExecuted(result.getMove());
                   notifyGameStateChanged();
                });
                return true;
            }
        } else {
            notifyInvalidMove(result.getErrorMessage());
            return false;
        }
    }
    
    private void animatePieceMove(int fromCol, int fromRow, int toCol, int toRow,
                                  Piece piece, Runnable onFinish) {
        double startX = fromCol * TILE_SIZE + 5;
        double startY = fromRow * TILE_SIZE + 5;
        double endX = toCol * TILE_SIZE + 5;
        double endY = toRow * TILE_SIZE + 5;
        
        Rectangle animRect = new Rectangle(startX, startY, TILE_SIZE - 10, TILE_SIZE - 10);
        
        try {
            javafx.scene.image.Image img = PieceImageLoader.getInstance().getImage(piece.isWhite, piece.name);
            animRect.setFill(new javafx.scene.paint.ImagePattern(img));
        } catch (Exception e) {
            animRect.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
        }
        
        animRect.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        dragLayer.getChildren().clear();
        dragLayer.getChildren().add(animRect);
        
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
                gameEngine.promotePawn(col, row, pieceType);
                
                // Ambil move terakhir (promosi)
                if (!board.moveHistory.isEmpty()) {
                    Board.Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
                    notifyMoveExecuted(lastMove);
                }
                notifyGameStateChanged();
            });
        }
    }
    
    private void notifyMoveExecuted(Board.Move move) {
        if (moveListener != null) moveListener.onMoveExecuted(move);
    }
    
    private void notifyGameStateChanged() {
        if (moveListener != null) moveListener.onGameStateChanged();
    }
    
    private void notifyInvalidMove(String reason) {
        if (moveListener != null) moveListener.onInvalidMove(reason);
    }
    
    public void onPieceSelected(Piece piece) {
        if (moveListener != null) moveListener.onPieceSelected(piece);
    }
    
    public void onPieceDeselected() {
        if (moveListener != null) moveListener.onPieceDeselected();
    }
}