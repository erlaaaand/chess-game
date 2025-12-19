package com.erland.chess.model;

import com.erland.chess.model.pieces.Piece;
import java.io.Serializable;

/**
 * Representasi satu langkah dalam catur
 */
public class Move implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Move data
    public transient Piece piece;
    public String pieceName;
    public boolean pieceIsWhite;
    public int fromCol, fromRow, toCol, toRow;
    
    // Captured piece
    public transient Piece capturedPiece;
    public String capturedPieceName;
    
    // Special moves
    public boolean isEnPassant = false;
    public boolean isCastling = false;
    public int castlingRookOldCol = -1;
    public int castlingRookNewCol = -1;
    public String promotionPiece = null;
    
    // Metadata
    public long timestamp;
    public int moveNumber;
    
    // Evaluation (untuk AI learning)
    public double evaluation = 0.0;
    public String moveQuality = "normal"; // brilliant, good, normal, inaccuracy, mistake, blunder
    
    public Move(Piece piece, int fromCol, int fromRow, int toCol, int toRow, Piece captured) {
        this.piece = piece;
        this.pieceName = piece.name;
        this.pieceIsWhite = piece.isWhite;
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.capturedPiece = captured;
        this.capturedPieceName = captured != null ? captured.name : null;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Convert move to standard algebraic notation
     */
    public String toNotation() {
        if (isCastling) {
            return toCol == 6 ? "O-O" : "O-O-O";
        }
        
        String from = getSquareName(fromCol, fromRow);
        String to = getSquareName(toCol, toRow);
        String capture = capturedPieceName != null || isEnPassant ? "x" : "-";
        
        String notation = pieceName.charAt(0) + from + capture + to;
        
        if (isEnPassant) {
            notation += " e.p.";
        }
        
        if (promotionPiece != null) {
            notation += "=" + promotionPiece.charAt(0);
        }
        
        return notation;
    }
    
    /**
     * Convert to UCI format (for chess engines)
     */
    public String toUCI() {
        String from = getSquareName(fromCol, fromRow);
        String to = getSquareName(toCol, toRow);
        String promotion = promotionPiece != null ? promotionPiece.toLowerCase().charAt(0) + "" : "";
        return from + to + promotion;
    }
    
    private String getSquareName(int col, int row) {
        return "" + (char)('a' + col) + (8 - row);
    }
    
    /**
     * Parse UCI format to move coordinates
     */
    public static int[] parseUCI(String uci) {
        if (uci.length() < 4) return null;
        
        int fromCol = uci.charAt(0) - 'a';
        int fromRow = 8 - (uci.charAt(1) - '0');
        int toCol = uci.charAt(2) - 'a';
        int toRow = 8 - (uci.charAt(3) - '0');
        
        return new int[]{fromCol, fromRow, toCol, toRow};
    }
    
    @Override
    public String toString() {
        return toNotation();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return fromCol == other.fromCol && fromRow == other.fromRow &&
               toCol == other.toCol && toRow == other.toRow;
    }
    
    @Override
    public int hashCode() {
        return fromCol * 1000 + fromRow * 100 + toCol * 10 + toRow;
    }
}