// Path: src/main/java/com/erland/chess/model/GameState.java
package com.erland.chess.model;

/**
 * Enumeration of possible game states
 */
public enum GameState {
    /**
     * Game is in progress
     */
    PLAYING("Playing"),
    
    /**
     * White player has won
     */
    WHITE_WON("White Won"),
    
    /**
     * Black player has won
     */
    BLACK_WON("Black Won"),
    
    /**
     * Game ended in stalemate (draw)
     */
    STALEMATE("Stalemate"),
    
    /**
     * Game was cancelled
     */
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    GameState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if game has ended
     */
    public boolean isGameOver() {
        return this != PLAYING;
    }
    
    /**
     * Get result string for PGN format
     */
    public String toPGNResult() {
        switch (this) {
            case WHITE_WON:
                return "1-0";
            case BLACK_WON:
                return "0-1";
            case STALEMATE:
                return "1/2-1/2";
            default:
                return "*";
        }
    }
    
    /**
     * Get winner color
     */
    public String getWinner() {
        switch (this) {
            case WHITE_WON:
                return "White";
            case BLACK_WON:
                return "Black";
            case STALEMATE:
                return "Draw";
            default:
                return "None";
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}