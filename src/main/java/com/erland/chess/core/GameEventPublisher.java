package com.erland.chess.core;

import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Event publisher using Observer pattern
 * Decouples game engine from UI
 */
public class GameEventPublisher {
    private final List<GameEventListener> listeners = new ArrayList<>();
    
    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }
    
    public void publishMoveExecuted(Move move) {
        listeners.forEach(l -> l.onMoveExecuted(move));
    }
    
    public void publishPromotion(int col, int row, String pieceType) {
        listeners.forEach(l -> l.onPromotion(col, row, pieceType));
    }
    
    public void publishGameOver(GameState gameState) {
        listeners.forEach(l -> l.onGameOver(gameState));
    }
    
    public void publishCheckStatus(boolean whiteInCheck, boolean blackInCheck) {
        listeners.forEach(l -> l.onCheckStatusChanged(whiteInCheck, blackInCheck));
    }
}