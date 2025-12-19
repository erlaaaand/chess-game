package com.erland.chess.model.pieces;
import com.erland.chess.model.Board;

public class Bishop extends Piece {
    public Bishop(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "Bishop";
        loadImage();
    }
    public boolean isValidMovement(int newCol, int newRow) {
        if (Math.abs(newCol - col) == Math.abs(newRow - row)) {
            int colStep = (newCol > col) ? 1 : -1;
            int rowStep = (newRow > row) ? 1 : -1;
            for (int i = 1; i < Math.abs(newCol - col); i++) {
                if (board.getPiece(col + i * colStep, row + i * rowStep) != null) return false;
            }
            return true;
        }
        return false;
    }
}