package com.erland.chess.view;

import com.erland.chess.network.GameServer;
import com.erland.chess.network.GameClient;
import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {
    private JFrame parentFrame;
    
    public MenuPanel(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(600, 500));
        setLayout(new GridBagLayout());
        setBackground(new Color(40, 40, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 50, 10, 50);
        
        // Title
        JLabel title = new JLabel("♔ CHESS GAME ♔", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        add(title, gbc);
        
        add(Box.createVerticalStrut(30), gbc);
        
        // Buttons
        JButton btnVsComputer = createMenuButton("VS Computer");
        JButton btnLocalMultiplayer = createMenuButton("Local Multiplayer");
        JButton btnHostGame = createMenuButton("Host Network Game");
        JButton btnJoinGame = createMenuButton("Join Network Game");
        JButton btnExit = createMenuButton("Exit");
        
        add(btnVsComputer, gbc);
        add(btnLocalMultiplayer, gbc);
        add(btnHostGame, gbc);
        add(btnJoinGame, gbc);
        add(btnExit, gbc);
        
        // Action Listeners
        btnVsComputer.addActionListener(e -> startGame(GameMode.VS_COMPUTER));
        btnLocalMultiplayer.addActionListener(e -> startGame(GameMode.LOCAL_MULTIPLAYER));
        btnHostGame.addActionListener(e -> hostNetworkGame());
        btnJoinGame.addActionListener(e -> joinNetworkGame());
        btnExit.addActionListener(e -> System.exit(0));
    }
    
    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(300, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(100, 150, 200));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(70, 130, 180));
            }
        });
        
        return btn;
    }
    
    private void startGame(GameMode mode) {
        parentFrame.getContentPane().removeAll();
        BoardPanel boardPanel = new BoardPanel(parentFrame, mode, null, true);
        parentFrame.add(boardPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    
    private void hostNetworkGame() {
        String portStr = JOptionPane.showInputDialog(this, "Enter port (default: 5555):", "5555");
        int port = 5555;
        try {
            if (portStr != null && !portStr.trim().isEmpty()) {
                port = Integer.parseInt(portStr);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port, using default 5555");
        }
        
        GameServer server = new GameServer(port);
        new Thread(() -> server.start()).start();
        
        JOptionPane.showMessageDialog(this, 
            "Server started on port " + port + "\nWaiting for opponent...",
            "Hosting Game", JOptionPane.INFORMATION_MESSAGE);
        
        parentFrame.getContentPane().removeAll();
        BoardPanel boardPanel = new BoardPanel(parentFrame, GameMode.NETWORK, server, true);
        parentFrame.add(boardPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    
    private void joinNetworkGame() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("5555");
        panel.add(new JLabel("Host IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Join Game", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String ip = ipField.getText();
            int port;
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid port!");
                return;
            }
            
            GameClient client = new GameClient(ip, port);
            if (client.connect()) {
                JOptionPane.showMessageDialog(this, "Connected to server!");
                
                parentFrame.getContentPane().removeAll();
                BoardPanel boardPanel = new BoardPanel(parentFrame, GameMode.NETWORK, client, false);
                parentFrame.add(boardPanel);
                parentFrame.revalidate();
                parentFrame.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to connect!");
            }
        }
    }
    
    public enum GameMode {
        VS_COMPUTER, LOCAL_MULTIPLAYER, NETWORK
    }
}