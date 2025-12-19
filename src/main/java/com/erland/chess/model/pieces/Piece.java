package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;
import java.io.Serializable;

public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    public int col, row;
    public boolean isWhite;
    public String name;
    public boolean hasMoved = false;
    protected Board board;

    public Piece(Board board) {
        this.board = board;
    }

    // Hapus method loadImage() dan draw() serta variabel BufferedImage
    // Model tidak perlu tahu cara menggambar dirinya sendiri.

    public boolean isWhite() { 
        return isWhite; 
    }

    public boolean canMove(int targetCol, int targetRow) {
        // Boundary check
        if(targetCol < 0 || targetCol > 7 || targetRow < 0 || targetRow > 7) {
            return false;
        }
        
        // Can't move to same position
        if(targetCol == col && targetRow == row) {
            return false;
        }
        
        // Can't capture own piece
        Piece target = board.getPiece(targetCol, targetRow);
        if(target != null && target.isWhite == this.isWhite) {
            return false;
        }
        
        // Check piece-specific movement rules
        return isValidMovement(targetCol, targetRow);
    }

    public abstract boolean isValidMovement(int newCol, int newRow);
}