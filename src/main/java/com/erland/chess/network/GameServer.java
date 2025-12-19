package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.view.BoardPanel;
import java.io.*;
import java.net.*;

public class GameServer implements NetworkHandler {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BoardPanel boardPanel;
    private boolean running = false;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);
            
            // Wait for client
            clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            
            // Start listening thread
            new Thread(this::listenForMessages).start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        while (running && clientSocket != null && !clientSocket.isClosed()) {
            try {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                handleMessage(msg);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Connection lost");
                running = false;
                break;
            }
        }
    }

    private void handleMessage(NetworkMessage msg) {
        switch (msg.type) {
            case MOVE:
                if (boardPanel != null) {
                    boardPanel.receiveMove(msg.move);
                }
                break;
            case SURRENDER:
                if (boardPanel != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JOptionPane.showMessageDialog(boardPanel, 
                            "Opponent surrendered!");
                    });
                }
                break;
            case CANCEL:
                if (boardPanel != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JOptionPane.showMessageDialog(boardPanel, 
                            "Game cancelled by opponent!");
                    });
                }
                break;
        }
    }

    @Override
    public void sendMove(Board.Move move) {
        if (out != null) {
            try {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.MOVE);
                msg.move = move;
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendSurrender() {
        if (out != null) {
            try {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.SURRENDER);
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendCancel() {
        if (out != null) {
            try {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.CANCEL);
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setBoardPanel(BoardPanel panel) {
        this.boardPanel = panel;
    }

    @Override
    public void close() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}