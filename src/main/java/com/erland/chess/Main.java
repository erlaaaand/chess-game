package com.erland.chess;

import com.erland.chess.view.BoardPanel;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("Chess Game - Java VS Computer");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        
        // Tambahkan Panel Catur
        BoardPanel boardPanel = new BoardPanel();
        window.add(boardPanel);
        
        window.pack(); // Sesuaikan ukuran window dengan panel
        window.setLocationRelativeTo(null); // Tengah layar
        window.setVisible(true);
    }
}