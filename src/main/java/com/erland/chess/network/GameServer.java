package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move; // Added import
import com.erland.chess.view.BoardView;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.*;

public class GameServer implements NetworkHandler {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private BoardView boardPanel;
    private boolean running = false;
    private final Gson gson = new Gson();

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
            
            // Setup Text Streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            // Start listening thread
            new Thread(this::listenForMessages).start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        while (running && clientSocket != null && !clientSocket.isClosed()) {
            try {
                String json = in.readLine();
                if (json == null) break;
                
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

    // Fixed: Changed Board.Move to Move
    @Override
    public void sendMove(Move move) {
        if (out != null) {
            NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.MOVE);
            msg.move = move;
            out.println(gson.toJson(msg));
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
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}