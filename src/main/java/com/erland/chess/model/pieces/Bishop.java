package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;

public class Bishop extends Piece {
    
    public Bishop(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.isWhite = isWhite;
        this.name = "Bishop";
    }
    
    @Override
    public boolean isValidMovement(int newCol, int newRow) {
        // Must move diagonally
        if (Math.abs(newCol - col) != Math.abs(newRow - row)) {
            return false;
        }
        
        // Check path is clear
        int colStep = (newCol > col) ? 1 : -1;
        int rowStep = (newRow > row) ? 1 : -1;
        
        int currentCol = col + colStep;
        int currentRow = row + rowStep;
        
        while (currentCol != newCol && currentRow != newRow) {
            if (board.getPiece(currentCol, currentRow) != null) {
                return false; // Path blocked
            }
            currentCol += colStep;
            currentRow += rowStep;
        }
        
        return true;
    }
}