package com.erland.chess.view;

import com.erland.chess.model.Board;
import com.erland.chess.model.Board.GameState;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.model.pieces.King;
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
    final int panelWidth = boardSize + 250;
    
    Board board = new Board();
    GameMode gameMode;
    NetworkHandler networkHandler;
    boolean isHost;
    JFrame parentFrame;
    GameReviewer gameReviewer;
    
    // UI Components
    JPanel controlPanel;
    JLabel turnLabel;
    JLabel statusLabel;
    JLabel checkLabel;
    JButton btnSurrender;
    JButton btnCancel;
    JButton btnMenu;
    JTextArea moveLog;

    public BoardPanel(JFrame frame, GameMode mode, Object network, boolean isHost) {
        this.parentFrame = frame;
        this.gameMode = mode;
        this.isHost = isHost;
        this.gameReviewer = new GameReviewer();
        
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
        
        // Start live game review
        gameReviewer.startNewGame();
        System.out.println("Live game analysis started!");
    }
    
    private void setupControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setBounds(boardSize + 10, 0, 230, boardSize);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(50, 50, 50));
        
        // Title
        JLabel titleLabel = new JLabel("CHESS GAME", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(titleLabel);
        controlPanel.add(Box.createVerticalStrut(15));
        
        // Turn indicator
        turnLabel = new JLabel("Turn: White", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(turnLabel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Check indicator
        checkLabel = new JLabel("", SwingConstants.CENTER);
        checkLabel.setFont(new Font("Arial", Font.BOLD, 16));
        checkLabel.setForeground(Color.RED);
        checkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(checkLabel);
        controlPanel.add(Box.createVerticalStrut(5));
        
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
        logLabel.setFont(new Font("Arial", Font.BOLD, 14));
        logLabel.setForeground(Color.WHITE);
        logLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(logLabel);
        controlPanel.add(Box.createVerticalStrut(5));
        
        moveLog = new JTextArea(15, 20);
        moveLog.setEditable(false);
        moveLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        moveLog.setBackground(new Color(30, 30, 30));
        moveLog.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(moveLog);
        scrollPane.setMaximumSize(new Dimension(220, 300));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
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
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(100, 150, 200));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(70, 130, 180));
            }
        });
        
        return btn;
    }
    
    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(board.gameState != GameState.PLAYING) {
                    return;
                }
                
                int col = e.getX() / tileSize;
                int row = e.getY() / tileSize;
                
                if(col >= 8 || row >= 8 || col < 0 || row < 0) {
                    return;
                }
                
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
                    statusLabel.setText("Computer is thinking...");
                    statusLabel.setForeground(Color.ORANGE);
                    return;
                }
                
                handlePieceSelection(col, row);
            }
        });
    }
    
    private void handlePieceSelection(int col, int row) {
        if (board.selectedPiece == null) {
            Piece p = board.getPiece(col, row);
            if (p != null && p.isWhite == board.isWhiteTurn) {
                board.selectedPiece = p;
                statusLabel.setText("Selected: " + p.name);
                statusLabel.setForeground(Color.YELLOW);
                repaint();
            }
        } else {
            if(board.movePiece(col, row)) {
                // Update live analysis
                gameReviewer.recordMove(board);
                
                updateMoveLog();
                updateTurnLabel();
                updateCheckStatus();
                statusLabel.setText("Move executed");
                statusLabel.setForeground(Color.GREEN);
                
                // Send move via network
                if(networkHandler != null && !board.moveHistory.isEmpty()) {
                    networkHandler.sendMove(board.moveHistory.get(board.moveHistory.size() - 1));
                }
                
                // Computer move
                if(gameMode == GameMode.VS_COMPUTER && !board.isWhiteTurn && 
                   board.gameState == GameState.PLAYING) {
                    statusLabel.setText("Computer thinking...");
                    statusLabel.setForeground(Color.ORANGE);
                    
                    Timer timer = new Timer(800, evt -> {
                        board.performComputerMove();
                        gameReviewer.recordMove(board);
                        updateMoveLog();
                        updateTurnLabel();
                        updateCheckStatus();
                        checkGameEnd();
                        repaint();
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
                
                checkGameEnd();
            } else {
                board.selectedPiece = null;
                statusLabel.setText("Invalid move!");
                statusLabel.setForeground(Color.RED);
            }
            repaint();
        }
    }
    
    public void receiveMove(Board.Move move) {
        SwingUtilities.invokeLater(() -> {
            Piece p = board.getPiece(move.fromCol, move.fromRow);
            if(p != null) {
                board.selectedPiece = p;
                if(board.movePiece(move.toCol, move.toRow)) {
                    gameReviewer.recordMove(board);
                    updateMoveLog();
                    updateTurnLabel();
                    updateCheckStatus();
                    checkGameEnd();
                    repaint();
                }
            }
        });
    }
    
    private void updateTurnLabel() {
        turnLabel.setText("Turn: " + (board.isWhiteTurn ? "White" : "Black"));
        btnCancel.setEnabled(board.canCancelGame());
    }
    
    private void updateCheckStatus() {
        if (board.whiteInCheck) {
            checkLabel.setText("⚠ WHITE IN CHECK! ⚠");
            checkLabel.setForeground(new Color(255, 100, 100));
        } else if (board.blackInCheck) {
            checkLabel.setText("⚠ BLACK IN CHECK! ⚠");
            checkLabel.setForeground(new Color(255, 100, 100));
        } else {
            checkLabel.setText("");
        }
    }
    
    private void updateMoveLog() {
        StringBuilder log = new StringBuilder();
        for(int i = 0; i < board.moveHistory.size(); i++) {
            if(i % 2 == 0) {
                log.append(String.format("%2d. ", (i/2 + 1)));
            }
            log.append(board.moveHistory.get(i).toNotation()).append(" ");
            if(i % 2 == 1) {
                log.append("\n");
            }
        }
        moveLog.setText(log.toString());
        moveLog.setCaretPosition(moveLog.getDocument().getLength());
    }
    
    private void surrender() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to surrender?", 
            "Surrender Confirmation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
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
                "Cannot cancel game after the first move!", 
                "Cancel Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Cancel this game?", 
            "Cancel Game", 
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
            Color resultColor = Color.WHITE;
            
            switch(board.gameState) {
                case WHITE_WON:
                    result = board.blackInCheck ? "♔ CHECKMATE - White Wins! ♔" : "♔ White Wins! ♔";
                    resultColor = new Color(135, 206, 250);
                    break;
                case BLACK_WON:
                    result = board.whiteInCheck ? "♚ CHECKMATE - Black Wins! ♚" : "♚ Black Wins! ♚";
                    resultColor = new Color(255, 105, 180);
                    break;
                case STALEMATE:
                    result = "Draw - Stalemate!";
                    resultColor = Color.YELLOW;
                    break;
                case CANCELLED:
                    result = "Game Cancelled";
                    resultColor = Color.GRAY;
                    break;
            }
            
            statusLabel.setText(result);
            statusLabel.setForeground(resultColor);
            checkLabel.setText("");
            
            if(board.gameState != GameState.CANCELLED) {
                Timer timer = new Timer(1500, e -> showReviewDialog());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }
    
    private void showReviewDialog() {
        int option = JOptionPane.showConfirmDialog(this,
            "Game finished! Would you like to add a review comment?",
            "Game Review", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if(option == JOptionPane.YES_OPTION) {
            String comment = JOptionPane.showInputDialog(this,
                "Enter your review/comments about this game:",
                "Game Review",
                JOptionPane.PLAIN_MESSAGE);
            
            if(comment != null && !comment.trim().isEmpty()) {
                boolean saved = gameReviewer.finalizeGame(board, comment);
                
                if(saved) {
                    JOptionPane.showMessageDialog(this,
                        "✓ Review saved successfully!\n\n" +
                        "Game data has been saved for analysis.\n" +
                        "You can analyze it using Python scripts:\n" +
                        "- chess_analyzer.py (batch analysis)\n" +
                        "- chess_live_analyzer.py (real-time)\n" +
                        "- chess_ml_analyzer.py (advanced)",
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to save review!",
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // Save without comment
            gameReviewer.finalizeGame(board, "No comment provided");
        }
    }
    
    private void backToMenu() {
        if(networkHandler != null) {
            try {
                networkHandler.close();
            } catch (Exception e) {
                System.err.println("Error closing network connection: " + e.getMessage());
            }
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
        
        // Enable antialiasing for better graphics
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw board tiles
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                // Alternate colors
                if ((c + r) % 2 == 0) {
                    g2.setColor(new Color(235, 235, 208));
                } else {
                    g2.setColor(new Color(119, 149, 86));
                }
                g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                
                // Highlight king in check
                Piece p = board.getPiece(c, r);
                if (p instanceof King) {
                    boolean inCheck = p.isWhite ? board.whiteInCheck : board.blackInCheck;
                    if (inCheck) {
                        g2.setColor(new Color(255, 0, 0, 120));
                        g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                    }
                }
                
                // Highlight selected piece
                if (board.selectedPiece != null && 
                    board.selectedPiece.col == c && board.selectedPiece.row == r) {
                    g2.setColor(new Color(255, 255, 0, 120));
                    g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                }
                
                // Show valid moves for selected piece
                if (board.selectedPiece != null && board.selectedPiece.canMove(c, r)) {
                    Piece target = board.getPiece(c, r);
                    if(target != null) {
                        // Red circle for capture
                        g2.setColor(new Color(255, 0, 0, 100));
                        g2.fillOval(c * tileSize + tileSize/4, r * tileSize + tileSize/4, 
                                   tileSize/2, tileSize/2);
                    } else {
                        // Green dot for regular move
                        g2.setColor(new Color(0, 255, 0, 100));
                        g2.fillOval(c * tileSize + tileSize/3, r * tileSize + tileSize/3, 
                                   tileSize/3, tileSize/3);
                    }
                }
            }
        }
        
        // Draw coordinates
        g2.setColor(new Color(80, 80, 80));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        for(int i = 0; i < 8; i++) {
            // Files (a-h)
            g2.drawString(String.valueOf((char)('a' + i)), 
                         i * tileSize + tileSize - 15, boardSize - 5);
            // Ranks (1-8)
            g2.drawString(String.valueOf(8 - i), 
                         5, i * tileSize + 15);
        }

        // Draw pieces
        board.draw(g2, boardSize);
        
        // Game over overlay
        if(board.gameState != GameState.PLAYING && board.gameState != GameState.CANCELLED) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, boardSize, boardSize);
            
            String text = "";
            Color textColor = Color.WHITE;
            
            switch(board.gameState) {
                case WHITE_WON: 
                    text = board.blackInCheck ? "CHECKMATE!" : "WHITE WINS!";
                    textColor = new Color(135, 206, 250);
                    break;
                case BLACK_WON: 
                    text = board.whiteInCheck ? "CHECKMATE!" : "BLACK WINS!";
                    textColor = new Color(255, 105, 180);
                    break;
                case STALEMATE: 
                    text = "STALEMATE!";
                    textColor = Color.YELLOW;
                    break;
            }
            
            g2.setColor(textColor);
            g2.setFont(new Font("Arial", Font.BOLD, 52));
            FontMetrics fm = g2.getFontMetrics();
            int x = (boardSize - fm.stringWidth(text)) / 2;
            int y = boardSize / 2;
            
            // Draw text shadow
            g2.setColor(Color.BLACK);
            g2.drawString(text, x + 3, y + 3);
            
            // Draw main text
            g2.setColor(textColor);
            g2.drawString(text, x, y);
        }
    }
}