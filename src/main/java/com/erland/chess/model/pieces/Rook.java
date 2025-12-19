package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;

class Rook extends Piece {
    
    public Rook(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.isWhite = isWhite;
        this.name = "Rook";
    }

    @Override
    public boolean isValidMovement(int newCol, int newRow) {
        // Must move horizontally or vertically
        if (newCol != col && newRow != row) {
            return false;
        }
        
        // Check path is clear
        if (newCol == col) {
            // Vertical movement
            int step = (newRow > row) ? 1 : -1;
            for (int r = row + step; r != newRow; r += step) {
                if (board.getPiece(col, r) != null) {
                    return false;
                }
            }
        } else {
            // Horizontal movement
            int step = (newCol > col) ? 1 : -1;
            for (int c = col + step; c != newCol; c += step) {
                if (board.getPiece(c, row) != null) {
                    return false;
                }
            }
        }
        
        return true;
    }
}