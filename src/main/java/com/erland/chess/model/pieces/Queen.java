package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;

public class Queen extends Piece {
    
    public Queen(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.isWhite = isWhite;
        this.name = "Queen";
    }
    
    @Override
    public boolean isValidMovement(int newCol, int newRow) {
        // Queen moves like rook OR bishop
        boolean isRookMove = (newCol == col || newRow == row);
        boolean isBishopMove = (Math.abs(newCol - col) == Math.abs(newRow - row));
        
        if (!isRookMove && !isBishopMove) {
            return false;
        }
        
        // Check path is clear
        int colStep = Integer.compare(newCol, col);
        int rowStep = Integer.compare(newRow, row);
        
        int currentCol = col + colStep;
        int currentRow = row + rowStep;
        
        while (currentCol != newCol || currentRow != newRow) {
            if (board.getPiece(currentCol, currentRow) != null) {
                return false;
            }
            currentCol += colStep;
            currentRow += rowStep;
        }
        
        return true;
    }
}