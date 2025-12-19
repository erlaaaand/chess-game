package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.view.BoardView;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.*;

public class GameClient implements NetworkHandler {
    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BoardView boardPanel;
    private boolean running = false;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            System.out.println("Connected to server: " + host + ":" + port);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            running = true;
            
            // Start listening thread
            new Thread(this::listenForMessages).start();
            
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    private void listenForMessages() {
        while (running && socket != null && !socket.isClosed()) {
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
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Game Over");
                        alert.setHeaderText("Opponent surrendered!");
                        alert.showAndWait();
                    });
                }
                break;
            case CANCEL:
                if (boardPanel != null) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Game Cancelled");
                        alert.setHeaderText("Game cancelled by opponent!");
                        alert.showAndWait();
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
    public void setBoardPanel(BoardView panel) {
        this.boardPanel = panel;
    }

    @Override
    public void close() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}