package com.erland.chess.model.pieces;
import com.erland.chess.model.Board;

public class Queen extends Piece {
    public Queen(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "Queen";
        loadImage();
    }
    public boolean isValidMovement(int newCol, int newRow) {
        // Gabungan Rook dan Bishop logic, bisa pakai instance dummy atau copy logic.
        // Demi ringkas, copy logic dasar:
        if (newCol == col || newRow == row) { // Rook style
             if (newCol == col) {
                int step = (newRow > row) ? 1 : -1;
                for (int r = row + step; r != newRow; r += step) if (board.getPiece(col, r) != null) return false;
            } else {
                int step = (newCol > col) ? 1 : -1;
                for (int c = col + step; c != newCol; c += step) if (board.getPiece(c, row) != null) return false;
            }
            return true;
        } else if (Math.abs(newCol - col) == Math.abs(newRow - row)) { // Bishop style
            int colStep = (newCol > col) ? 1 : -1;
            int rowStep = (newRow > row) ? 1 : -1;
            for (int i = 1; i < Math.abs(newCol - col); i++) if (board.getPiece(col + i * colStep, row + i * rowStep) != null) return false;
            return true;
        }
        return false;
    }
}