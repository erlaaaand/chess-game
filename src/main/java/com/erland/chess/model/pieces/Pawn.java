package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;

public class Pawn extends Piece {
    public Pawn(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "Pawn";
        loadImage();
    }

    public boolean isValidMovement(int newCol, int newRow) {
        int colorIndex = isWhite ? 1 : -1;

        // Gerak 1 langkah ke depan (y naik untuk hitam, turun untuk putih jika 0 di atas)
        // Di Java Swing, 0,0 biasanya kiri atas. Jadi Putih (bawah) row-nya berkurang.
        // Asumsi: Putih di Row 6/7 (Bawah), Hitam di Row 0/1 (Atas).
        // Putih gerak KE ATAS (Row makin KECIL), Hitam KE BAWAH (Row makin BESAR)
        int direction = isWhite ? -1 : 1; 

        // Gerak lurus 1 kotak
        if (newCol == col && newRow == row + direction && board.getPiece(newCol, newRow) == null) {
            return true;
        }

        // Gerak lurus 2 kotak (Awal saja)
        if (newCol == col && newRow == row + direction * 2 && board.getPiece(newCol, newRow) == null && board.getPiece(newCol, newRow - direction) == null) {
            if ((isWhite && row == 6) || (!isWhite && row == 1)) return true;
        }

        // Makan Miring (Capture)
        if (Math.abs(newCol - col) == 1 && newRow == row + direction && board.getPiece(newCol, newRow) != null) {
            return true;
        }

        return false;
    }
}