package com.erland.chess.view;

import com.erland.chess.network.GameServer;
import com.erland.chess.network.GameClient;
import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {
    private JFrame parentFrame;
    
    public MenuPanel(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(600, 550));
        setLayout(new GridBagLayout());
        setBackground(new Color(40, 40, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 50, 10, 50);
        
        // Title
        JLabel title = new JLabel("♔ CHESS GAME ♔", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 52));
        title.setForeground(new Color(255, 215, 0));
        add(title, gbc);
        
        JLabel subtitle = new JLabel("Complete Edition with Python Analysis", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.ITALIC, 14));
        subtitle.setForeground(new Color(200, 200, 200));
        add(subtitle, gbc);
        
        add(Box.createVerticalStrut(30), gbc);
        
        // Buttons
        JButton btnVsComputer = createMenuButton("🤖 VS Computer");
        JButton btnLocalMultiplayer = createMenuButton("👥 Local Multiplayer");
        JButton btnHostGame = createMenuButton("🌐 Host Network Game");
        JButton btnJoinGame = createMenuButton("🔗 Join Network Game");
        JButton btnExit = createMenuButton("❌ Exit");
        
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
        btnExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
        
        // Footer info
        JLabel footer = new JLabel("© 2024 Chess Game | Java + Python Integration", SwingConstants.CENTER);
        footer.setFont(new Font("Arial", Font.PLAIN, 10));
        footer.setForeground(new Color(150, 150, 150));
        add(footer, gbc);
    }
    
    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(350, 55));
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
        parentFrame.pack();
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    
    private void hostNetworkGame() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField portField = new JTextField("5555");
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        
        int result = JOptionPane.showConfirmDialog(this, panel,
            "Host Network Game", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            int port = 5555;
            try {
                String portStr = portField.getText().trim();
                if (!portStr.isEmpty()) {
                    port = Integer.parseInt(portStr);
                    if(port < 1024 || port > 65535) {
                        JOptionPane.showMessageDialog(this, 
                            "Port must be between 1024 and 65535!\nUsing default port 5555");
                        port = 5555;
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid port number!\nUsing default port 5555");
                port = 5555;
            }
            
            final int finalPort = port;
            GameServer server = new GameServer(finalPort);
            
            // Show waiting dialog
            JDialog waitDialog = new JDialog(parentFrame, "Waiting for opponent...", true);
            waitDialog.setLayout(new BorderLayout(10, 10));
            JLabel waitLabel = new JLabel(
                "<html><center>Server started on port " + finalPort + 
                "<br><br>Waiting for opponent to connect...<br><br>" +
                "Share this IP and port with your opponent</center></html>",
                SwingConstants.CENTER);
            waitLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            waitDialog.add(waitLabel, BorderLayout.CENTER);
            
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> {
                server.close();
                waitDialog.dispose();
            });
            waitDialog.add(cancelBtn, BorderLayout.SOUTH);
            
            waitDialog.setSize(350, 180);
            waitDialog.setLocationRelativeTo(this);
            
            // Start server in background
            new Thread(() -> {
                server.start();
                SwingUtilities.invokeLater(() -> {
                    waitDialog.dispose();
                    startNetworkGame(server, true);
                });
            }).start();
            
            waitDialog.setVisible(true);
        }
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
            "Join Network Game", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String ip = ipField.getText().trim();
            if(ip.isEmpty()) {
                ip = "localhost";
            }
            
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
                if(port < 1024 || port > 65535) {
                    JOptionPane.showMessageDialog(this, "Port must be between 1024 and 65535!");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid port number!");
                return;
            }
            
            GameClient client = new GameClient(ip, port);
            
            // Show connecting dialog
            JDialog connectDialog = new JDialog(parentFrame, "Connecting...", false);
            JLabel connectLabel = new JLabel("Connecting to " + ip + ":" + port + "...", 
                                            SwingConstants.CENTER);
            connectLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            connectDialog.add(connectLabel);
            connectDialog.setSize(300, 100);
            connectDialog.setLocationRelativeTo(this);
            connectDialog.setVisible(true);
            
            // Try to connect
            new Thread(() -> {
                boolean connected = client.connect();
                SwingUtilities.invokeLater(() -> {
                    connectDialog.dispose();
                    if (connected) {
                        JOptionPane.showMessageDialog(this, 
                            "Successfully connected to server!",
                            "Connection Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        startNetworkGame(client, false);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Failed to connect to server!\n\n" +
                            "Please check:\n" +
                            "- Host IP address is correct\n" +
                            "- Port number is correct\n" +
                            "- Host has started the server\n" +
                            "- Firewall is not blocking the connection",
                            "Connection Failed",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }
    
    private void startNetworkGame(Object network, boolean isHost) {
        parentFrame.getContentPane().removeAll();
        BoardPanel boardPanel = new BoardPanel(parentFrame, GameMode.NETWORK, network, isHost);
        parentFrame.add(boardPanel);
        parentFrame.pack();
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    
    public enum GameMode {
        VS_COMPUTER, LOCAL_MULTIPLAYER, NETWORK
    }
}