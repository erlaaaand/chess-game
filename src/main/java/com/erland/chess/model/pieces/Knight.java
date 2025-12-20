package com.erland.chess.model.pieces;
import com.erland.chess.model.Board;

public class Knight extends Piece {
    public Knight(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "Knight";
        // Removed loadImage();
    }
    public boolean isValidMovement(int newCol, int newRow) {
        return Math.abs(newCol - col) * Math.abs(newRow - row) == 2;
    }
}