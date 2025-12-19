package com.erland.chess.model;

import com.erland.chess.model.pieces.*;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

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
    
    public Board() {
        addPieces();
        findKings();
    }

    public void addPieces() {
        // Black (Top)
        pieceList[0][0] = new Rook(this, 0, 0, false);
        pieceList[1][0] = new Knight(this, 1, 0, false);
        pieceList[2][0] = new Bishop(this, 2, 0, false);
        pieceList[3][0] = new Queen(this, 3, 0, false);
        pieceList[4][0] = new King(this, 4, 0, false);
        pieceList[5][0] = new Bishop(this, 5, 0, false);
        pieceList[6][0] = new Knight(this, 6, 0, false);
        pieceList[7][0] = new Rook(this, 7, 0, false);
        for(int i=0; i<8; i++) pieceList[i][1] = new Pawn(this, i, 1, false);

        // White (Bottom)
        pieceList[0][7] = new Rook(this, 0, 7, true);
        pieceList[1][7] = new Knight(this, 1, 7, true);
        pieceList[2][7] = new Bishop(this, 2, 7, true);
        pieceList[3][7] = new Queen(this, 3, 7, true);
        pieceList[4][7] = new King(this, 4, 7, true);
        pieceList[5][7] = new Bishop(this, 5, 7, true);
        pieceList[6][7] = new Knight(this, 6, 7, true);
        pieceList[7][7] = new Rook(this, 7, 7, true);
        for(int i=0; i<8; i++) pieceList[i][6] = new Pawn(this, i, 6, true);
    }
    
    private void findKings() {
        for(int c=0; c<8; c++) {
            for(int r=0; r<8; r++) {
                Piece p = getPiece(c, r);
                if(p instanceof King) {
                    if(p.isWhite) whiteKing = (King)p;
                    else blackKing = (King)p;
                }
            }
        }
    }

    public Piece getPiece(int col, int row) {
        if (col < 0 || col > 7 || row < 0 || row > 7) return null;
        return pieceList[col][row];
    }

    public boolean movePiece(int newCol, int newRow) {
        if (selectedPiece != null && gameState == GameState.PLAYING) {
            if (selectedPiece.canMove(newCol, newRow)) {
                // Record move
                Piece captured = pieceList[newCol][newRow];
                Move move = new Move(selectedPiece, selectedPiece.col, selectedPiece.row, 
                                    newCol, newRow, captured);
                
                // Execute move
                pieceList[selectedPiece.col][selectedPiece.row] = null;
                pieceList[newCol][newRow] = selectedPiece;
                selectedPiece.col = newCol;
                selectedPiece.row = newRow;
                selectedPiece.hasMoved = true;
                
                moveHistory.add(move);
                totalMoves++;
                
                // Check game state
                isWhiteTurn = !isWhiteTurn;
                checkGameState();
                
                selectedPiece = null;
                return true;
            }
        }
        return false;
    }

    public void performComputerMove() {
        if(gameState != GameState.PLAYING) return;
        
        System.out.println("Computer thinking...");
        ArrayList<Piece> blackPieces = new ArrayList<>();
        
        for(int c=0; c<8; c++) {
            for(int r=0; r<8; r++) {
                Piece p = getPiece(c,r);
                if(p != null && !p.isWhite) blackPieces.add(p);
            }
        }

        // Try to find valid move
        Random rand = new Random();
        ArrayList<int[]> validMoves = new ArrayList<>();
        
        // Collect all valid moves
        for(Piece p : blackPieces) {
            for(int c=0; c<8; c++) {
                for(int r=0; r<8; r++) {
                    if(p.canMove(c, r)) {
                        validMoves.add(new int[]{p.col, p.row, c, r});
                    }
                }
            }
        }
        
        if(!validMoves.isEmpty()) {
            // Prioritize captures
            ArrayList<int[]> captureMoves = new ArrayList<>();
            for(int[] move : validMoves) {
                if(getPiece(move[2], move[3]) != null) {
                    captureMoves.add(move);
                }
            }
            
            int[] chosenMove = captureMoves.isEmpty() ? 
                validMoves.get(rand.nextInt(validMoves.size())) :
                captureMoves.get(rand.nextInt(captureMoves.size()));
            
            Piece p = getPiece(chosenMove[0], chosenMove[1]);
            selectedPiece = p;
            movePiece(chosenMove[2], chosenMove[3]);
        }
    }
    
    public boolean isKingInCheck(boolean isWhite) {
        King king = isWhite ? whiteKing : blackKing;
        if(king == null) return false;
        
        // Check if any enemy piece can capture the king
        for(int c=0; c<8; c++) {
            for(int r=0; r<8; r++) {
                Piece p = getPiece(c, r);
                if(p != null && p.isWhite != isWhite) {
                    if(p.canMove(king.col, king.row)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void checkGameState() {
        boolean inCheck = isKingInCheck(isWhiteTurn);
        boolean hasValidMove = hasValidMoves(isWhiteTurn);
        
        if(inCheck && !hasValidMove) {
            gameState = isWhiteTurn ? GameState.BLACK_WON : GameState.WHITE_WON;
            System.out.println("CHECKMATE! " + (isWhiteTurn ? "Black" : "White") + " wins!");
        } else if(!inCheck && !hasValidMove) {
            gameState = GameState.STALEMATE;
            System.out.println("STALEMATE!");
        }
    }
    
    private boolean hasValidMoves(boolean isWhite) {
        for(int c=0; c<8; c++) {
            for(int r=0; r<8; r++) {
                Piece p = getPiece(c, r);
                if(p != null && p.isWhite == isWhite) {
                    for(int tc=0; tc<8; tc++) {
                        for(int tr=0; tr<8; tr++) {
                            if(p.canMove(tc, tr)) {
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
        System.out.println((whiteResigns ? "White" : "Black") + " surrendered!");
    }
    
    public boolean canCancelGame() {
        return totalMoves <= 1;
    }

    public void draw(Graphics2D g2, int size) {
        int tileSize = size / 8;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                Piece p = pieceList[c][r];
                if (p != null) p.draw(g2, size);
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
            String capture = capturedPieceName != null ? "x" : "-";
            return pieceName.charAt(0) + from + capture + to;
        }
    }
}