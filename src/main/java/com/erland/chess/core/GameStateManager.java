package com.erland.chess.core;

import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;

import java.util.Optional;
import java.util.Stack;

/**
 * Manages game state transitions
 * Single Responsibility: State management
 */
public class GameStateManager {
    private final Board board;
    private final Stack<GameSnapshot> stateHistory;
    
    public GameStateManager(Board board) {
        this.board = board;
        this.stateHistory = new Stack<>();
    }
    
    /**
     * Called after a move is executed
     */
    public void afterMove(Move move) {
        // Save state for undo
        stateHistory.push(new GameSnapshot(board));
        
        // Update check status
        updateCheckStatus();
        
        // Check for game over conditions
        checkGameOver();
    }
    
    /**
     * Called after pawn promotion
     */
    public void afterPromotion() {
        updateCheckStatus();
        checkGameOver();
    }
    
    /**
     * Undo last move
     */
    public Optional<Move> undoLastMove() {
        if (stateHistory.isEmpty()) {
            return Optional.empty();
        }
        
        GameSnapshot snapshot = stateHistory.pop();
        snapshot.restore(board);
        
        return Optional.of(board.moveHistory.isEmpty() ? null : 
                          board.moveHistory.get(board.moveHistory.size() - 1));
    }
    
    /**
     * Check if game is over
     */
    public boolean isGameOver() {
        return board.gameState != GameState.PLAYING;
    }
    
    public GameState getGameState() {
        return board.gameState;
    }
    
    // Private methods
    
    private void updateCheckStatus() {
        board.whiteInCheck = board.isKingInCheck(true);
        board.blackInCheck = board.isKingInCheck(false);
    }
    
    private void checkGameOver() {
        boolean inCheck = board.isKingInCheck(board.isWhiteTurn);
        boolean hasValidMove = hasValidMoves(board.isWhiteTurn);
        
        if (inCheck && !hasValidMove) {
            board.gameState = board.isWhiteTurn ? GameState.BLACK_WON : GameState.WHITE_WON;
        } else if (!inCheck && !hasValidMove) {
            board.gameState = GameState.STALEMATE;
        }
    }
    
    private boolean hasValidMoves(boolean isWhite) {
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                var piece = board.getPiece(c, r);
                if (piece != null && piece.isWhite == isWhite) {
                    for (int tc = 0; tc < 8; tc++) {
                        for (int tr = 0; tr < 8; tr++) {
                            if (piece.canMove(tc, tr) && 
                                !board.wouldBeInCheckAfterMove(piece, tc, tr)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Snapshot of game state for undo
     */
    private static class GameSnapshot {
        // Store necessary state for restoration
        // Implementation omitted for brevity
        
        GameSnapshot(Board board) {
            // Store current state
        }
        
        void restore(Board board) {
            // Restore saved state
        }
    }
}