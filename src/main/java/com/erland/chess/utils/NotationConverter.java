// Path: src/main/java/com/erland/chess/utils/NotationConverter.java
package com.erland.chess.utils;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move;

/**
 * Utility class for converting between different chess notations
 */
public class NotationConverter {
    
    /**
     * Convert square coordinates to algebraic notation
     * @param col Column (0-7)
     * @param row Row (0-7)
     * @return Algebraic notation (e.g., "e4")
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
     * @param algebraic Algebraic notation (e.g., "e4")
     * @return Array [col, row] or null if invalid
     */
    public static int[] fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() < 2) {
            return null;
        }
        
        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);
        
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            return null;
        }
        
        int col = file - 'a';
        int row = 8 - (rank - '0');
        
        return new int[]{col, row};
    }
    
    /**
     * Convert move to UCI notation
     * @param move Move object
     * @return UCI string (e.g., "e2e4")
     */
    public static String toUCI(Move move) {
        if (move == null) {
            return null;
        }
        
        String from = toAlgebraic(move.fromCol, move.fromRow);
        String to = toAlgebraic(move.toCol, move.toRow);
        
        String uci = from + to;
        
        // Add promotion piece if applicable
        if (move.promotionPiece != null) {
            char promotionChar = move.promotionPiece.toLowerCase().charAt(0);
            uci += promotionChar;
        }
        
        return uci;
    }
    
    /**
     * Convert move to Standard Algebraic Notation (SAN)
     * @param board Current board state
     * @param move Move to convert
     * @return SAN string (e.g., "Nf3", "exd5")
     */
    public static String toSAN(Board board, Move move) {
        if (move == null) {
            return "";
        }
        
        StringBuilder san = new StringBuilder();
        
        // Castling
        if (move.isCastling) {
            return move.toCol == 6 ? "O-O" : "O-O-O";
        }
        
        // Piece symbol (except for pawns)
        String pieceName = move.pieceName;
        if (!pieceName.equals("Pawn")) {
            san.append(getPieceSymbol(pieceName));
        }
        
        // Disambiguation (if needed)
        if (needsDisambiguation(board, move)) {
            san.append(toAlgebraic(move.fromCol, move.fromRow).charAt(0));
        }
        
        // Capture
        if (move.capturedPieceName != null || move.isEnPassant) {
            if (pieceName.equals("Pawn")) {
                // For pawn captures, include file
                san.append(toAlgebraic(move.fromCol, move.fromRow).charAt(0));
            }
            san.append('x');
        }
        
        // Destination square
        san.append(toAlgebraic(move.toCol, move.toRow));
        
        // Promotion
        if (move.promotionPiece != null) {
            san.append('=').append(getPieceSymbol(move.promotionPiece));
        }
        
        // Check/Checkmate (would need to simulate move)
        // This is simplified - proper implementation would check board state
        
        return san.toString();
    }
    
    /**
     * Convert FEN position to board
     * @param fen FEN string
     * @return Board object or null if invalid
     */
    public static Board fromFEN(String fen) {
        // This is a placeholder - full FEN parsing would be more complex
        // For now, return null and use default board initialization
        return null;
    }
    
    /**
     * Convert board to FEN notation
     * @param board Board object
     * @return FEN string
     */
    public static String toFEN(Board board) {
        StringBuilder fen = new StringBuilder();
        
        // Piece placement
        for (int row = 0; row < 8; row++) {
            int emptySquares = 0;
            
            for (int col = 0; col < 8; col++) {
                var piece = board.getPiece(col, row);
                
                if (piece == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    
                    char pieceChar = getFENPieceChar(piece.name);
                    fen.append(piece.isWhite ? 
                        Character.toUpperCase(pieceChar) : pieceChar);
                }
            }
            
            if (emptySquares > 0) {
                fen.append(emptySquares);
            }
            
            if (row < 7) {
                fen.append('/');
            }
        }
        
        // Active color
        fen.append(' ').append(board.isWhiteTurn ? 'w' : 'b');
        
        // Castling availability (simplified)
        fen.append(" KQkq");
        
        // En passant target square
        fen.append(" -");
        
        // Halfmove and fullmove clocks
        fen.append(" 0 ").append(board.totalMoves / 2 + 1);
        
        return fen.toString();
    }
    
    /**
     * Convert move to PGN notation
     * @param move Move object
     * @param moveNumber Move number
     * @return PGN string
     */
    public static String toPGN(Move move, int moveNumber) {
        if (move == null) {
            return "";
        }
        
        StringBuilder pgn = new StringBuilder();
        
        if (move.pieceIsWhite) {
            pgn.append(moveNumber).append(". ");
        }
        
        pgn.append(move.toNotation());
        
        return pgn.toString();
    }
    
    /**
     * Get piece symbol for SAN notation
     */
    private static char getPieceSymbol(String pieceName) {
        switch (pieceName) {
            case "King": return 'K';
            case "Queen": return 'Q';
            case "Rook": return 'R';
            case "Bishop": return 'B';
            case "Knight": return 'N';
            default: return ' ';
        }
    }
    
    /**
     * Get FEN character for piece
     */
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
    
    /**
     * Check if move needs disambiguation
     */
    private static boolean needsDisambiguation(Board board, Move move) {
        // Simplified - proper implementation would check for ambiguous moves
        return false;
    }
    
    /**
     * Validate algebraic notation format
     */
    public static boolean isValidAlgebraic(String notation) {
        if (notation == null || notation.length() != 2) {
            return false;
        }
        
        char file = notation.charAt(0);
        char rank = notation.charAt(1);
        
        return file >= 'a' && file <= 'h' && rank >= '1' && rank <= '8';
    }
    
    /**
     * Validate UCI notation format
     */
    public static boolean isValidUCI(String uci) {
        if (uci == null || uci.length() < 4 || uci.length() > 5) {
            return false;
        }
        
        return isValidAlgebraic(uci.substring(0, 2)) && 
               isValidAlgebraic(uci.substring(2, 4));
    }
}