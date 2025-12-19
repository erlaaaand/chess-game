package com.erland.chess;

import com.erland.chess.view.MenuPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Chess Game - Complete Edition");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            
            // Tampilkan Menu Utama
            MenuPanel menuPanel = new MenuPanel(window);
            window.add(menuPanel);
            
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}