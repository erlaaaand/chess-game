package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;
import java.io.Serializable;

/**
 * Abstract base class for all chess pieces
 * Model layer - no rendering logic
 */
public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    // Position
    public int col, row;
    
    // Properties
    public boolean isWhite;
    public String name;
    public boolean hasMoved = false;
    
    // Reference to board
    protected transient Board board;

    public Piece(Board board) {
        this.board = board;
    }

    /**
     * Check if piece can move to target square
     * Includes boundary checking and same-color blocking
     */
    public boolean canMove(int targetCol, int targetRow) {
        // Boundary check
        if (targetCol < 0 || targetCol > 7 || targetRow < 0 || targetRow > 7) {
            return false;
        }
        
        // Can't move to same position
        if (targetCol == col && targetRow == row) {
            return false;
        }
        
        // Can't capture own piece
        Piece target = board.getPiece(targetCol, targetRow);
        if (target != null && target.isWhite == this.isWhite) {
            return false;
        }
        
        // Check piece-specific movement rules
        return isValidMovement(targetCol, targetRow);
    }

    /**
     * Abstract method for piece-specific movement validation
     * Must be implemented by each piece type
     */
    public abstract boolean isValidMovement(int newCol, int newRow);
    
    /**
     * Get piece color
     */
    public boolean isWhite() { 
        return isWhite; 
    }
    
    /**
     * Restore board reference after deserialization
     */
    public void setBoard(Board board) {
        this.board = board;
    }
    
    @Override
    public String toString() {
        return (isWhite ? "White " : "Black ") + name + " at " + 
               ((char)('a' + col)) + (8 - row);
    }
}