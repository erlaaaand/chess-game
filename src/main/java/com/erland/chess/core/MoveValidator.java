package com.erland.chess.core;

import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.Piece;

/**
 * Validates chess moves according to rules
 * Single Responsibility: Move validation
 */
public class MoveValidator {
    private final Board board;
    
    public MoveValidator(Board board) {
        this.board = board;
    }
    
    /**
     * Validate if a move is legal
     */
    public ValidationResult validate(Piece piece, int toCol, int toRow) {
        // 1. Check bounds
        if (!isValidSquare(toCol, toRow)) {
            return ValidationResult.invalid("Move out of bounds");
        }
        
        // 2. Check if same position
        if (piece.col == toCol && piece.row == toRow) {
            return ValidationResult.invalid("Cannot move to same square");
        }
        
        // 3. Check turn (Opsional, biasanya dihandle di Engine, tapi bagus ada double check)
        if (piece.isWhite != board.isWhiteTurn) {
             return ValidationResult.invalid("Not your turn!");
        }

        // 4. Check piece specific movement rules
        if (!piece.canMove(toCol, toRow)) {
            return ValidationResult.invalid("Invalid move pattern for " + piece.name);
        }
        
        // 5. Check if would leave king in check (Crucial Rule)
        if (board.wouldBeInCheckAfterMove(piece, toCol, toRow)) {
            return ValidationResult.invalid("Move would leave king in check");
        }
        
        return ValidationResult.valid();
    }
    
    private boolean isValidSquare(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }
}