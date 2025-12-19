package com.erland.chess.utils;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.King;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.model.pieces.Rook;

/**
 * Utility class for converting between different chess notations
 */
public class NotationConverter {
    
    /**
     * Convert square coordinates to algebraic notation
     */
    public static String toAlgebraic(int col, int row) {
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            return "??";
        }
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }
    
    /**
     * Convert algebraic notation to coordinates
     */
    public static int[] fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() < 2) return null;
        
        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);
        
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') return null;
        
        int col = file - 'a';
        int row = 8 - (rank - '0');
        
        return new int[]{col, row};
    }
    
    /**
     * Convert move to UCI notation
     */
    public static String toUCI(Move move) {
        if (move == null) return null;
        
        String from = toAlgebraic(move.fromCol, move.fromRow);
        String to = toAlgebraic(move.toCol, move.toRow);
        
        String uci = from + to;
        
        if (move.promotionPiece != null) {
            char promotionChar = move.promotionPiece.toLowerCase().charAt(0);
            uci += promotionChar;
        }
        return uci;
    }
    
    /**
     * Convert board to FEN notation (Improved & Accurate)
     */
    public static String toFEN(Board board) {
        StringBuilder fen = new StringBuilder();
        
        // 1. Piece placement
        for (int row = 0; row < 8; row++) {
            int emptySquares = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(col, row);
                if (piece == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    char pieceChar = getFENPieceChar(piece.name);
                    fen.append(piece.isWhite ? Character.toUpperCase(pieceChar) : pieceChar);
                }
            }
            if (emptySquares > 0) {
                fen.append(emptySquares);
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        
        // 2. Active color
        fen.append(' ').append(board.isWhiteTurn ? 'w' : 'b');
        
        // 3. Castling availability
        fen.append(' ');
        String castling = getCastlingRights(board);
        fen.append(castling);
        
        // 4. En passant target square
        fen.append(' ');
        if (board.enPassantPawn != null) {
            // Target square is "behind" the pawn
            int direction = board.enPassantPawn.isWhite ? 1 : -1; 
            // Note: Direction logic might need adjustment based on how Board tracks it
            // Normally target square is the square passed over.
            // If white pawn is at rank 4 (index 3), it moved from rank 2 (index 1). Target is rank 3 (index 2).
            int epRow = board.enPassantPawn.row + direction; 
            fen.append(toAlgebraic(board.enPassantPawn.col, epRow));
        } else {
            fen.append('-');
        }
        
        // 5. Halfmove clock (Simplified)
        fen.append(" 0"); 
        
        // 6. Fullmove number
        fen.append(" ").append(board.totalMoves / 2 + 1);
        
        return fen.toString();
    }
    
    private static String getCastlingRights(Board board) {
        StringBuilder rights = new StringBuilder();
        
        // White
        Piece wk = board.getPiece(4, 7); // e1
        if (wk instanceof King && wk.isWhite && !wk.hasMoved) {
            Piece wrk = board.getPiece(7, 7); // h1
            Piece wrq = board.getPiece(0, 7); // a1
            
            if (wrk instanceof Rook && wrk.isWhite && !wrk.hasMoved) rights.append('K');
            if (wrq instanceof Rook && wrq.isWhite && !wrq.hasMoved) rights.append('Q');
        }
        
        // Black
        Piece bk = board.getPiece(4, 0); // e8
        if (bk instanceof King && !bk.isWhite && !bk.hasMoved) {
            Piece brk = board.getPiece(7, 0); // h8
            Piece brq = board.getPiece(0, 0); // a8
            
            if (brk instanceof Rook && !brk.isWhite && !brk.hasMoved) rights.append('k');
            if (brq instanceof Rook && !brq.isWhite && !brq.hasMoved) rights.append('q');
        }
        
        return rights.length() > 0 ? rights.toString() : "-";
    }
    
    private static char getFENPieceChar(String pieceName) {
        switch (pieceName) {
            case "King": return 'k';
            case "Queen": return 'q';
            case "Rook": return 'r';
            case "Bishop": return 'b';
            case "Knight": return 'n';
            case "Pawn": return 'p';
            default: return '?';
        }
    }
}