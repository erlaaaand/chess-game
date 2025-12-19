package com.erland.chess.view;

import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.Piece;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel {
    final int tileSize = 85; // Ukuran per kotak
    final int boardSize = tileSize * 8;
    Board board = new Board();

    public BoardPanel() {
        this.setPreferredSize(new Dimension(boardSize, boardSize));
        this.setBackground(Color.GREEN);
        
        // Input Mouse
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Konversi koordinat pixel ke koordinat papan (0-7)
                int col = e.getX() / tileSize;
                int row = e.getY() / tileSize;

                // Logika Klik
                if (board.selectedPiece == null) {
                    // 1. Klik pertama: Pilih bidak
                    Piece p = board.getPiece(col, row);
                    // Pastikan bidak ada dan warnanya sesuai giliran
                    if (p != null && p.isWhite == board.isWhiteTurn) {
                        board.selectedPiece = p;
                        System.out.println("Dipilih: " + p.name);
                    }
                } else {
                    // 2. Klik kedua: Gerak ke kotak tujuan
                    board.movePiece(col, row);
                    board.selectedPiece = null; // Reset pilihan jika klik salah/selesai
                }
                repaint(); // Gambar ulang
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1. Gambar Papan Catur
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                if ((c + r) % 2 == 0) {
                    g2.setColor(new Color(235, 235, 208)); // Warna Putih Gading
                } else {
                    g2.setColor(new Color(119, 149, 86));  // Warna Hijau Catur Klasik
                }
                g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                
                // Highlight jika ada bidak terpilih
                if (board.selectedPiece != null && board.selectedPiece.col == c && board.selectedPiece.row == r) {
                    g2.setColor(new Color(255, 255, 0, 100)); // Kuning transparan
                    g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                }
            }
        }

        // 2. Gambar Bidak
        board.draw(g2, boardSize);
    }
}