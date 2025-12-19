package com.erland.chess.core;

import com.erland.chess.model.Move;

/**
 * Result of move execution
 */
public class MoveResult {
    private final boolean success;
    private final Move move;
    private final String errorMessage;
    private final boolean needsPromotion;
    
    private MoveResult(boolean success, Move move, String errorMessage, boolean needsPromotion) {
        this.success = success;
        this.move = move;
        this.errorMessage = errorMessage;
        this.needsPromotion = needsPromotion;
    }
    
    public static MoveResult success(Move move) {
        return new MoveResult(true, move, null, false);
    }
    
    public static MoveResult needsPromotion(Move move) {
        return new MoveResult(true, move, null, true);
    }
    
    public static MoveResult failed(String errorMessage) {
        return new MoveResult(false, null, errorMessage, false);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Move getMove() {
        return move;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean needsPromotion() {
        return needsPromotion;
    }
}