package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;

public class Pawn extends Piece {
    public Pawn(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; 
        this.row = row; 
        this.isWhite = isWhite;
        this.name = "Pawn";
    }

    @Override
    public boolean isValidMovement(int newCol, int newRow) {
        int direction = isWhite ? -1 : 1;

        // Move forward 1 square
        if (newCol == col && newRow == row + direction && board.getPiece(newCol, newRow) == null) {
            return true;
        }

        // Move forward 2 squares from starting position
        if (newCol == col && newRow == row + direction * 2 && board.getPiece(newCol, newRow) == null && 
            board.getPiece(newCol, newRow - direction) == null) {
            if ((isWhite && row == 6) || (!isWhite && row == 1)) {
                return true;
            }
        }

        // Capture diagonally
        if (Math.abs(newCol - col) == 1 && newRow == row + direction) {
            Piece target = board.getPiece(newCol, newRow);
            if (target != null && target.isWhite != isWhite) {
                return true;
            }
            
            // En passant
            if (target == null && board.enPassantPawn != null) {
                if (board.enPassantPawn.col == newCol && board.enPassantPawn.row == row) {
                    if (Math.abs(board.enPassantPawn.col - col) == 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}