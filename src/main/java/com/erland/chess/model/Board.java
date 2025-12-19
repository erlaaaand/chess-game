package com.erland.chess.model;

import com.erland.chess.model.pieces.*;
import java.util.ArrayList;
import java.util.List;

public class Board {
    final int cols = 8;
    final int rows = 8;
    public Piece[][] pieceList = new Piece[cols][rows];
    public Piece selectedPiece;
    
    public boolean isWhiteTurn = true;
    public GameState gameState = GameState.PLAYING;
    
    public ArrayList<Move> moveHistory = new ArrayList<>();
    public int totalMoves = 0;
    
    private King whiteKing;
    private King blackKing;
    
    // En passant tracking
    public Pawn enPassantPawn = null;
    
    // Check status
    public boolean whiteInCheck = false;
    public boolean blackInCheck = false;

    public Board() {
        addPieces();
        findKings();
    }

    public void addPieces() {
        // Black pieces (Top)
        pieceList[0][0] = new Rook(this, 0, 0, false);
        pieceList[1][0] = new Knight(this, 1, 0, false);
        pieceList[2][0] = new Bishop(this, 2, 0, false);
        pieceList[3][0] = new Queen(this, 3, 0, false);
        pieceList[4][0] = new King(this, 4, 0, false);
        pieceList[5][0] = new Bishop(this, 5, 0, false);
        pieceList[6][0] = new Knight(this, 6, 0, false);
        pieceList[7][0] = new Rook(this, 7, 0, false);
        for(int i = 0; i < 8; i++) {
            pieceList[i][1] = new Pawn(this, i, 1, false);
        }

        // White pieces (Bottom)
        pieceList[0][7] = new Rook(this, 0, 7, true);
        pieceList[1][7] = new Knight(this, 1, 7, true);
        pieceList[2][7] = new Bishop(this, 2, 7, true);
        pieceList[3][7] = new Queen(this, 3, 7, true);
        pieceList[4][7] = new King(this, 4, 7, true);
        pieceList[5][7] = new Bishop(this, 5, 7, true);
        pieceList[6][7] = new Knight(this, 6, 7, true);
        pieceList[7][7] = new Rook(this, 7, 7, true);
        for(int i = 0; i < 8; i++) {
            pieceList[i][6] = new Pawn(this, i, 6, true);
        }
    }
    
    private void findKings() {
        for(int c = 0; c < 8; c++) {
            for(int r = 0; r < 8; r++) {
                Piece p = getPiece(c, r);
                if(p instanceof King) {
                    if(p.isWhite) {
                        whiteKing = (King)p;
                    } else {
                        blackKing = (King)p;
                    }
                }
            }
        }
    }

    public Piece getPiece(int col, int row) {
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            return null;
        }
        return pieceList[col][row];
    }

    public boolean movePiece(int newCol, int newRow) {
        if (selectedPiece == null || gameState != GameState.PLAYING) {
            return false;
        }
        
        if (selectedPiece.canMove(newCol, newRow)) {
            // Check if move would leave king in check
            if (wouldBeInCheckAfterMove(selectedPiece, newCol, newRow)) {
                return false;
            }
            
            // Record move details
            Piece captured = pieceList[newCol][newRow];
            int oldCol = selectedPiece.col;
            int oldRow = selectedPiece.row;
            boolean isEnPassant = false;
            boolean isCastling = false;
            Piece castlingRook = null;
            int rookOldCol = -1;
            int rookNewCol = -1;
            
            // Check for en passant capture
            if (selectedPiece instanceof Pawn && newCol != oldCol && captured == null) {
                if (enPassantPawn != null && enPassantPawn.col == newCol) {
                    captured = enPassantPawn;
                    pieceList[enPassantPawn.col][enPassantPawn.row] = null;
                    isEnPassant = true;
                }
            }
            
            // Check for castling
            if (selectedPiece instanceof King && Math.abs(newCol - oldCol) == 2) {
                isCastling = true;
                if (newCol == 6) { // Kingside castling
                    castlingRook = getPiece(7, oldRow);
                    rookOldCol = 7;
                    rookNewCol = 5;
                } else if (newCol == 2) { // Queenside castling
                    castlingRook = getPiece(0, oldRow);
                    rookOldCol = 0;
                    rookNewCol = 3;
                }
                
                if (castlingRook != null) {
                    pieceList[rookOldCol][oldRow] = null;
                    pieceList[rookNewCol][oldRow] = castlingRook;
                    castlingRook.col = rookNewCol;
                    castlingRook.hasMoved = true;
                }
            }
            
            // Execute move
            pieceList[oldCol][oldRow] = null;
            pieceList[newCol][newRow] = selectedPiece;
            selectedPiece.col = newCol;
            selectedPiece.row = newRow;
            selectedPiece.hasMoved = true;
            
            // Reset en passant if not a pawn double move
            Pawn newEnPassantPawn = null;
            if (selectedPiece instanceof Pawn && Math.abs(newRow - oldRow) == 2) {
                newEnPassantPawn = (Pawn) selectedPiece;
            }
            enPassantPawn = newEnPassantPawn;
            
            // Create move record
            Move move = new Move(selectedPiece, oldCol, oldRow, newCol, newRow, captured);
            move.isEnPassant = isEnPassant;
            move.isCastling = isCastling;
            if (isCastling) {
                move.castlingRookOldCol = rookOldCol;
                move.castlingRookNewCol = rookNewCol;
            }
            
            moveHistory.add(move);
            totalMoves++;
            
            // Switch turn and check game state
            isWhiteTurn = !isWhiteTurn;
            updateCheckStatus();
            checkGameState();
            
            selectedPiece = null;
            return true;
        }
        
        return false;
    }

    public void promotePawn(int col, int row, String type) {
        Piece pawn = getPiece(col, row);
        if (pawn == null) return;
        
        boolean isWhite = pawn.isWhite;
        Piece newPiece = null;
        
        switch (type) {
            case "Queen": newPiece = new Queen(this, col, row, isWhite); break;
            case "Rook": newPiece = new Rook(this, col, row, isWhite); break;
            case "Bishop": newPiece = new Bishop(this, col, row, isWhite); break;
            case "Knight": newPiece = new Knight(this, col, row, isWhite); break;
            default: newPiece = new Queen(this, col, row, isWhite); break;
        }
        
        if (newPiece != null) {
            pieceList[col][row] = newPiece;
            newPiece.hasMoved = true;
            if (!moveHistory.isEmpty()) {
                moveHistory.get(moveHistory.size() - 1).promotionPiece = type;
            }
            updateCheckStatus();
            checkGameState();
        }
    }
    
    // NOTE: performComputerMove DIHAPUS. Logika AI dipindahkan ke AIPlayer.java
    
    public boolean isKingInCheck(boolean isWhite) {
        King king = isWhite ? whiteKing : blackKing;
        if (king == null || pieceList[king.col][king.row] != king) {
            findKings();
            king = isWhite ? whiteKing : blackKing;
        }

        if (king == null) return false;
        
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = getPiece(c, r);
                if (p != null && p.isWhite != isWhite) {
                    if (p instanceof Pawn) {
                        int direction = p.isWhite ? -1 : 1;
                        if (king.row == p.row + direction && (Math.abs(king.col - p.col) == 1)) {
                            return true;
                        }
                    } else {
                        if (p.isValidMovement(king.col, king.row)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public boolean wouldBeInCheckAfterMove(Piece piece, int newCol, int newRow) {
        // Simulate move
        int oldCol = piece.col;
        int oldRow = piece.row;
        Piece capturedPiece = getPiece(newCol, newRow);
        Piece enPassantCaptured = null;
        
        if (piece instanceof Pawn && newCol != oldCol && capturedPiece == null) {
            if (enPassantPawn != null && enPassantPawn.col == newCol) {
                enPassantCaptured = enPassantPawn;
                pieceList[enPassantPawn.col][enPassantPawn.row] = null;
            }
        }
        
        pieceList[oldCol][oldRow] = null;
        pieceList[newCol][newRow] = piece;
        piece.col = newCol;
        piece.row = newRow;
        
        boolean inCheck = isKingInCheck(piece.isWhite);
        
        // Undo move
        pieceList[oldCol][oldRow] = piece;
        pieceList[newCol][newRow] = capturedPiece;
        piece.col = oldCol;
        piece.row = oldRow;
        
        if (enPassantCaptured != null) {
            pieceList[enPassantCaptured.col][enPassantCaptured.row] = enPassantCaptured;
        }
        
        return inCheck;
    }
    
    private void updateCheckStatus() {
        whiteInCheck = isKingInCheck(true);
        blackInCheck = isKingInCheck(false);
    }
    
    private void checkGameState() {
        boolean inCheck = isKingInCheck(isWhiteTurn);
        boolean hasValidMove = hasValidMoves(isWhiteTurn);
        
        if(inCheck && !hasValidMove) {
            gameState = isWhiteTurn ? GameState.BLACK_WON : GameState.WHITE_WON;
        } else if(!inCheck && !hasValidMove) {
            gameState = GameState.STALEMATE;
        }
    }
    
    private boolean hasValidMoves(boolean isWhite) {
        for(int c = 0; c < 8; c++) {
            for(int r = 0; r < 8; r++) {
                Piece p = getPiece(c, r);
                if(p != null && p.isWhite == isWhite) {
                    for(int tc = 0; tc < 8; tc++) {
                        for(int tr = 0; tr < 8; tr++) {
                            if(p.canMove(tc, tr) && !wouldBeInCheckAfterMove(p, tc, tr)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public void surrender(boolean whiteResigns) {
        gameState = whiteResigns ? GameState.BLACK_WON : GameState.WHITE_WON;
    }
    
    public boolean canCancelGame() {
        return totalMoves <= 1;
    }
}