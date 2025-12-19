package com.erland.chess.model.pieces;
import com.erland.chess.model.Board;

public class King extends Piece {
    public King(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "King";
        loadImage();
    }
    public boolean isValidMovement(int newCol, int newRow) {
        return Math.abs(newCol - col) <= 1 && Math.abs(newRow - row) <= 1;
    }
}