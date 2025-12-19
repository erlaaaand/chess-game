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
    
    // Giliran: true = White, false = Black
    public boolean isWhiteTurn = true; 
    
    // Mode AI
    public boolean isAgainstComputer = true; 

    public Board() {
        addPieces();
    }

    public void addPieces() {
        // Hitam (Atas)
        pieceList[0][0] = new Rook(this, 0, 0, false);
        pieceList[1][0] = new Knight(this, 1, 0, false);
        pieceList[2][0] = new Bishop(this, 2, 0, false);
        pieceList[3][0] = new Queen(this, 3, 0, false);
        pieceList[4][0] = new King(this, 4, 0, false);
        pieceList[5][0] = new Bishop(this, 5, 0, false);
        pieceList[6][0] = new Knight(this, 6, 0, false);
        pieceList[7][0] = new Rook(this, 7, 0, false);
        for(int i=0; i<8; i++) pieceList[i][1] = new Pawn(this, i, 1, false);

        // Putih (Bawah)
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

    public Piece getPiece(int col, int row) {
        if (col < 0 || col > 7 || row < 0 || row > 7) return null;
        return pieceList[col][row];
    }

    // Gerakkan bidak oleh User
    public void movePiece(int newCol, int newRow) {
        if (selectedPiece != null) {
            if (selectedPiece.canMove(newCol, newRow)) {
                // Lakukan gerakan
                pieceList[selectedPiece.col][selectedPiece.row] = null; // Hapus dari lama
                pieceList[newCol][newRow] = selectedPiece; // Taruh di baru
                selectedPiece.col = newCol;
                selectedPiece.row = newRow;
                
                // Ganti Giliran
                isWhiteTurn = !isWhiteTurn;
                selectedPiece = null;

                // Jika lawan komputer & sekarang giliran hitam
                if (isAgainstComputer && !isWhiteTurn) {
                    performComputerMove();
                }
            }
        }
    }

    // AI Sederhana (Random Valid Move + Prioritas Makan)
    private void performComputerMove() {
        System.out.println("Komputer berpikir...");
        ArrayList<Piece> blackPieces = new ArrayList<>();
        
        // 1. Kumpulkan semua bidak hitam
        for(int c=0; c<8; c++) {
            for(int r=0; r<8; r++) {
                Piece p = getPiece(c,r);
                if(p != null && !p.isWhite) blackPieces.add(p);
            }
        }

        // 2. Cari langkah valid (Coba 100x acak supaya variatif tapi cepat)
        Random rand = new Random();
        boolean moved = false;
        int attempts = 0;
        
        while (!moved && attempts < 1000) {
            if(blackPieces.isEmpty()) break;
            Piece p = blackPieces.get(rand.nextInt(blackPieces.size()));
            
            // Coba gerak ke posisi random di papan
            int targetC = rand.nextInt(8);
            int targetR = rand.nextInt(8);
            
            if (p.canMove(targetC, targetR)) {
                // Eksekusi
                pieceList[p.col][p.row] = null;
                pieceList[targetC][targetR] = p;
                p.col = targetC;
                p.row = targetR;
                
                moved = true;
                isWhiteTurn = true; // Balik ke user
            }
            attempts++;
        }
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
}