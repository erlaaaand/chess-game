package com.erland.chess.core;

import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;

/**
 * Listener interface for game events
 */
public interface GameEventListener {
    void onMoveExecuted(Move move);
    void onPromotion(int col, int row, String pieceType);
    void onGameOver(GameState gameState);
    void onCheckStatusChanged(boolean whiteInCheck, boolean blackInCheck);
}