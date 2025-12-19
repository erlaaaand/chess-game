package com.erland.chess.model;

import com.erland.chess.model.pieces.*;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;
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

    public void performComputerMove() {
        if(gameState != GameState.PLAYING) {
            return;
        }
        
        System.out.println("Computer is thinking...");
        
        // Collect all black pieces
        List<Piece> blackPieces = new ArrayList<>();
        for(int c = 0; c < 8; c++) {
            for(int r = 0; r < 8; r++) {
                Piece p = getPiece(c, r);
                if(p != null && !p.isWhite) {
                    blackPieces.add(p);
                }
            }
        }

        // Collect all valid moves that don't leave king in check
        List<int[]> validMoves = new ArrayList<>();
        for(Piece p : blackPieces) {
            for(int c = 0; c < 8; c++) {
                for(int r = 0; r < 8; r++) {
                    if(p.canMove(c, r) && !wouldBeInCheckAfterMove(p, c, r)) {
                        validMoves.add(new int[]{p.col, p.row, c, r});
                    }
                }
            }
        }
        
        if(validMoves.isEmpty()) {
            System.out.println("Computer has no valid moves!");
            return;
        }
        
        // Prioritize captures
        List<int[]> captureMoves = new ArrayList<>();
        for(int[] move : validMoves) {
            Piece target = getPiece(move[2], move[3]);
            if(target != null || (getPiece(move[0], move[1]) instanceof Pawn && 
                move[2] != move[0] && target == null && enPassantPawn != null)) {
                captureMoves.add(move);
            }
        }
        
        Random rand = new Random();
        int[] chosenMove = captureMoves.isEmpty() ? 
            validMoves.get(rand.nextInt(validMoves.size())) :
            captureMoves.get(rand.nextInt(captureMoves.size()));
        
        Piece p = getPiece(chosenMove[0], chosenMove[1]);
        if(p != null) {
            selectedPiece = p;
            movePiece(chosenMove[2], chosenMove[3]);
            System.out.println("Computer moved: " + p.name + " from " + 
                             (char)('a' + chosenMove[0]) + (8 - chosenMove[1]) + 
                             " to " + (char)('a' + chosenMove[2]) + (8 - chosenMove[3]));
        }
    }
    
    public boolean isKingInCheck(boolean isWhite) {
        King king = isWhite ? whiteKing : blackKing;
        if(king == null) {
            return false;
        }
        
        // Check if any enemy piece can capture the king
        for(int c = 0; c < 8; c++) {
            for(int r = 0; r < 8; r++) {
                Piece p = getPiece(c, r);
                if(p != null && p.isWhite != isWhite) {
                    // For pawns, check diagonal attacks only
                    if(p instanceof Pawn) {
                        int direction = p.isWhite ? -1 : 1;
                        if(Math.abs(king.col - p.col) == 1 && king.row == p.row + direction) {
                            return true;
                        }
                    } else if(p.isValidMovement(king.col, king.row)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean wouldBeInCheckAfterMove(Piece piece, int newCol, int newRow) {
        // Simulate move
        int oldCol = piece.col;
        int oldRow = piece.row;
        Piece capturedPiece = getPiece(newCol, newRow);
        Piece enPassantCaptured = null;
        
        // Handle en passant
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
        
        if (whiteInCheck) {
            System.out.println("White King is in CHECK!");
        }
        if (blackInCheck) {
            System.out.println("Black King is in CHECK!");
        }
    }
    
    private void checkGameState() {
        boolean inCheck = isKingInCheck(isWhiteTurn);
        boolean hasValidMove = hasValidMoves(isWhiteTurn);
        
        if(inCheck && !hasValidMove) {
            gameState = isWhiteTurn ? GameState.BLACK_WON : GameState.WHITE_WON;
            System.out.println("CHECKMATE! " + (isWhiteTurn ? "Black" : "White") + " wins!");
        } else if(!inCheck && !hasValidMove) {
            gameState = GameState.STALEMATE;
            System.out.println("STALEMATE - Game is a draw!");
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
        System.out.println((whiteResigns ? "White" : "Black") + " resigned from the game!");
    }
    
    public boolean canCancelGame() {
        return totalMoves <= 1;
    }

    public void draw(Graphics2D g2, int size) {
        int tileSize = size / 8;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                Piece p = pieceList[c][r];
                if (p != null) {
                    p.draw(g2, size);
                }
            }
        }
    }
    
    public enum GameState {
        PLAYING, WHITE_WON, BLACK_WON, STALEMATE, CANCELLED
    }
    
    public static class Move implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        public transient Piece piece;
        public String pieceName;
        public boolean pieceIsWhite;
        public int fromCol, fromRow, toCol, toRow;
        public transient Piece capturedPiece;
        public String capturedPieceName;
        public long timestamp;
        public boolean isEnPassant = false;
        public boolean isCastling = false;
        public int castlingRookOldCol = -1;
        public int castlingRookNewCol = -1;
        
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
        
        public String toNotation() {
            String from = "" + (char)('a' + fromCol) + (8 - fromRow);
            String to = "" + (char)('a' + toCol) + (8 - toRow);
            
            if (isCastling) {
                return toCol == 6 ? "O-O" : "O-O-O"; // Kingside or Queenside
            }
            
            String capture = capturedPieceName != null || isEnPassant ? "x" : "-";
            String notation = pieceName.charAt(0) + from + capture + to;
            
            if (isEnPassant) {
                notation += " e.p.";
            }
            
            return notation;
        }
    }
}