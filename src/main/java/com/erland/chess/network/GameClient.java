package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.view.BoardView;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.*;

public class GameClient implements NetworkHandler {
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out; // Ganti ObjectOutputStream
    private BufferedReader in; // Ganti ObjectInputStream
    private BoardView boardPanel;
    private boolean running = false;
    private final Gson gson = new Gson(); // Tambahkan Gson

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean connect() { // Pastikan signature sesuai interface
        try {
            socket = new Socket(host, port);
            System.out.println("Connected to server: " + host + ":" + port);
            
            // Setup Text Streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
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
                String json = in.readLine();
                if (json == null) break; // End of stream
                
                NetworkMessage msg = gson.fromJson(json, NetworkMessage.class);
                handleMessage(msg);
            } catch (IOException e) {
                System.out.println("Connection lost");
                running = false;
                break;
            }
        }
    }

    private void handleMessage(NetworkMessage msg) {
        if (msg == null) return;
        
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
            NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.MOVE);
            msg.move = move;
            String json = gson.toJson(msg);
            out.println(json); // Kirim JSON string
        }
    }

    @Override
    public void sendSurrender() {
        if (out != null) {
            NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.SURRENDER);
            out.println(gson.toJson(msg));
        }
    }

    @Override
    public void sendCancel() {
        if (out != null) {
            NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.CANCEL);
            out.println(gson.toJson(msg));
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