package com.erland.chess.service;

import com.erland.chess.model.Move;
import com.erland.chess.network.NetworkMessage;
import com.erland.chess.network.NetworkHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for network operations
 * Handles async network communication
 */
public class NetworkService {
    private final ExecutorService executor;
    private NetworkHandler networkHandler;
    
    public NetworkService() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Network-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void setNetworkHandler(NetworkHandler handler) {
        this.networkHandler = handler;
    }
    
    /**
     * Send move asynchronously
     */
    public CompletableFuture<Void> sendMove(Move move) {
        return CompletableFuture.runAsync(() -> {
            if (networkHandler != null) {
                networkHandler.sendMove(move);
            }
        }, executor);
    }
    
    /**
     * Send game action
     */
    public CompletableFuture<Void> sendAction(NetworkMessage.MessageType action) {
        return CompletableFuture.runAsync(() -> {
            if (networkHandler != null) {
                switch (action) {
                    case SURRENDER:
                        networkHandler.sendSurrender();
                        break;
                    case CANCEL:
                        networkHandler.sendCancel();
                        break;
                }
            }
        }, executor);
    }
    
    public void shutdown() {
        if (networkHandler != null) {
            networkHandler.close();
        }
        executor.shutdown();
    }
}