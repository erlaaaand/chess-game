package com.erland.chess.view;

import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.network.GameServer;
import com.erland.chess.network.GameClient;
import com.erland.chess.network.NetworkHandler;
import com.erland.chess.review.GameReviewer;
import com.erland.chess.view.MenuPanel.GameMode;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel {
    final int tileSize = 85;
    final int boardSize = tileSize * 8;
    final int panelWidth = boardSize + 250; // Extra space for controls
    
    Board board = new Board();
    GameMode gameMode;
    NetworkHandler networkHandler;
    boolean isHost;
    JFrame parentFrame;
    
    // UI Components
    JPanel controlPanel;
    JLabel turnLabel;
    JLabel statusLabel;
    JButton btnSurrender;
    JButton btnCancel;
    JButton btnMenu;
    JTextArea moveLog;

    public BoardPanel(JFrame frame, GameMode mode, Object network, boolean isHost) {
        this.parentFrame = frame;
        this.gameMode = mode;
        this.isHost = isHost;
        
        if(network instanceof GameServer) {
            this.networkHandler = (GameServer)network;
        } else if(network instanceof GameClient) {
            this.networkHandler = (GameClient)network;
        }
        
        setPreferredSize(new Dimension(panelWidth, boardSize));
        setLayout(null);
        setBackground(new Color(40, 40, 40));
        
        setupControlPanel();
        setupMouseListener();
        
        if(networkHandler != null) {
            networkHandler.setBoardPanel(this);
        }
    }
    
    private void setupControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setBounds(boardSize + 10, 0, 230, boardSize);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(50, 50, 50));
        
        // Turn indicator
        turnLabel = new JLabel("Turn: White", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(turnLabel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Status
        statusLabel = new JLabel("Playing", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setForeground(Color.GREEN);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(statusLabel);
        controlPanel.add(Box.createVerticalStrut(20));
        
        // Surrender button
        btnSurrender = createButton("Surrender");
        btnSurrender.addActionListener(e -> surrender());
        controlPanel.add(btnSurrender);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Cancel button
        btnCancel = createButton("Cancel Game");
        btnCancel.addActionListener(e -> cancelGame());
        controlPanel.add(btnCancel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Menu button
        btnMenu = createButton("Back to Menu");
        btnMenu.addActionListener(e -> backToMenu());
        controlPanel.add(btnMenu);
        controlPanel.add(Box.createVerticalStrut(20));
        
        // Move log
        JLabel logLabel = new JLabel("Move History:", SwingConstants.CENTER);
        logLabel.setForeground(Color.WHITE);
        logLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(logLabel);
        
        moveLog = new JTextArea(15, 20);
        moveLog.setEditable(false);
        moveLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        moveLog.setBackground(new Color(30, 30, 30));
        moveLog.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(moveLog);
        scrollPane.setMaximumSize(new Dimension(220, 300));
        controlPanel.add(scrollPane);
        
        add(controlPanel);
    }
    
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 35));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }
    
    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(board.gameState != GameState.PLAYING) return;
                
                int col = e.getX() / tileSize;
                int row = e.getY() / tileSize;
                
                if(col >= 8 || row >= 8) return;
                
                // Network game restrictions
                if(gameMode == GameMode.NETWORK) {
                    if((isHost && !board.isWhiteTurn) || (!isHost && board.isWhiteTurn)) {
                        statusLabel.setText("Not your turn!");
                        statusLabel.setForeground(Color.RED);
                        return;
                    }
                }
                
                // Computer game restriction
                if(gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn) {
                    return;
                }
                
                if (board.selectedPiece == null) {
                    Piece p = board.getPiece(col, row);
                    if (p != null && p.isWhite == board.isWhiteTurn) {
                        board.selectedPiece = p;
                        statusLabel.setText("Selected: " + p.name);
                        statusLabel.setForeground(Color.YELLOW);
                    }
                } else {
                    if(board.movePiece(col, row)) {
                        updateMoveLog();
                        updateTurnLabel();
                        statusLabel.setText("Move executed");
                        statusLabel.setForeground(Color.GREEN);
                        
                        // Send move via network
                        if(networkHandler != null) {
                            networkHandler.sendMove(board.moveHistory.get(board.moveHistory.size()-1));
                        }
                        
                        // Computer move
                        if(gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn && 
                           board.gameState == GameState.PLAYING) {
                            Timer timer = new Timer(500, evt -> {
                                board.performComputerMove();
                                updateMoveLog();
                                updateTurnLabel();
                                checkGameEnd();
                                repaint();
                            });
                            timer.setRepeats(false);
                            timer.start();
                        }
                        
                        checkGameEnd();
                    } else {
                        board.selectedPiece = null;
                        statusLabel.setText("Invalid move");
                        statusLabel.setForeground(Color.RED);
                    }
                }
                repaint();
            }
        });
    }
    
    public void receiveMove(Board.Move move) {
        Piece p = board.getPiece(move.fromCol, move.fromRow);
        if(p != null) {
            board.selectedPiece = p;
            board.movePiece(move.toCol, move.toRow);
            updateMoveLog();
            updateTurnLabel();
            checkGameEnd();
            repaint();
        }
    }
    
    private void updateTurnLabel() {
        turnLabel.setText("Turn: " + (board.isWhiteTurn ? "White" : "Black"));
        btnCancel.setEnabled(board.canCancelGame());
    }
    
    private void updateMoveLog() {
        StringBuilder log = new StringBuilder();
        for(int i = 0; i < board.moveHistory.size(); i++) {
            if(i % 2 == 0) {
                log.append((i/2 + 1)).append(". ");
            }
            log.append(board.moveHistory.get(i).toNotation()).append(" ");
            if(i % 2 == 1) log.append("\n");
        }
        moveLog.setText(log.toString());
        moveLog.setCaretPosition(moveLog.getDocument().getLength());
    }
    
    private void surrender() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to surrender?", "Surrender", 
            JOptionPane.YES_NO_OPTION);
        
        if(confirm == JOptionPane.YES_OPTION) {
            boolean whiteResigns = (gameMode == GameMode.NETWORK) ? isHost : board.isWhiteTurn;
            board.surrender(whiteResigns);
            
            if(networkHandler != null) {
                networkHandler.sendSurrender();
            }
            
            checkGameEnd();
            repaint();
        }
    }
    
    private void cancelGame() {
        if(!board.canCancelGame()) {
            JOptionPane.showMessageDialog(this, 
                "Cannot cancel after first move!", "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Cancel this game?", "Cancel Game", 
            JOptionPane.YES_NO_OPTION);
        
        if(confirm == JOptionPane.YES_OPTION) {
            board.gameState = GameState.CANCELLED;
            if(networkHandler != null) {
                networkHandler.sendCancel();
            }
            JOptionPane.showMessageDialog(this, "Game cancelled!");
            backToMenu();
        }
    }
    
    private void checkGameEnd() {
        if(board.gameState != GameState.PLAYING) {
            btnSurrender.setEnabled(false);
            btnCancel.setEnabled(false);
            
            String result = "";
            switch(board.gameState) {
                case WHITE_WON:
                    result = "White Wins!";
                    statusLabel.setForeground(Color.CYAN);
                    break;
                case BLACK_WON:
                    result = "Black Wins!";
                    statusLabel.setForeground(Color.MAGENTA);
                    break;
                case STALEMATE:
                    result = "Stalemate - Draw!";
                    statusLabel.setForeground(Color.YELLOW);
                    break;
                case CANCELLED:
                    result = "Game Cancelled";
                    statusLabel.setForeground(Color.GRAY);
                    break;
            }
            
            statusLabel.setText(result);
            
            if(board.gameState != GameState.CANCELLED) {
                Timer timer = new Timer(1000, e -> showReviewDialog());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }
    
    private void showReviewDialog() {
        int option = JOptionPane.showConfirmDialog(this,
            "Game finished! Would you like to save a review?",
            "Game Review", JOptionPane.YES_NO_OPTION);
        
        if(option == JOptionPane.YES_OPTION) {
            String comment = JOptionPane.showInputDialog(this,
                "Enter your review/comments:", "Game Review",
                JOptionPane.PLAIN_MESSAGE);
            
            if(comment != null && !comment.trim().isEmpty()) {
                GameReviewer reviewer = new GameReviewer();
                boolean saved = reviewer.saveReview(board, comment);
                
                if(saved) {
                    JOptionPane.showMessageDialog(this,
                        "Review saved successfully!\nYou can analyze it with Python scripts.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to save review!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void backToMenu() {
        if(networkHandler != null) {
            networkHandler.close();
        }
        
        parentFrame.getContentPane().removeAll();
        MenuPanel menu = new MenuPanel(parentFrame);
        parentFrame.add(menu);
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw board
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                if ((c + r) % 2 == 0) {
                    g2.setColor(new Color(235, 235, 208));
                } else {
                    g2.setColor(new Color(119, 149, 86));
                }
                g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                
                // Highlight selected piece
                if (board.selectedPiece != null && 
                    board.selectedPiece.col == c && board.selectedPiece.row == r) {
                    g2.setColor(new Color(255, 255, 0, 100));
                    g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                }
                
                // Show valid moves
                if (board.selectedPiece != null && board.selectedPiece.canMove(c, r)) {
                    g2.setColor(new Color(0, 255, 0, 80));
                    g2.fillOval(c * tileSize + tileSize/3, r * tileSize + tileSize/3, 
                               tileSize/3, tileSize/3);
                }
            }
        }
        
        // Draw coordinates
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        for(int i = 0; i < 8; i++) {
            g2.drawString(String.valueOf((char)('a' + i)), i * tileSize + 5, boardSize - 5);
            g2.drawString(String.valueOf(8 - i), 5, i * tileSize + 15);
        }

        // Draw pieces
        board.draw(g2, boardSize);
        
        // Game over overlay
        if(board.gameState != GameState.PLAYING && board.gameState != GameState.CANCELLED) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, boardSize, boardSize);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            String text = "";
            switch(board.gameState) {
                case WHITE_WON: text = "WHITE WINS!"; break;
                case BLACK_WON: text = "BLACK WINS!"; break;
                case STALEMATE: text = "STALEMATE!"; break;
            }
            
            FontMetrics fm = g2.getFontMetrics();
            int x = (boardSize - fm.stringWidth(text)) / 2;
            int y = boardSize / 2;
            g2.drawString(text, x, y);
        }
    }
}