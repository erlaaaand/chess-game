package com.erland.chess.network;

import com.erland.chess.model.Move;

/**
 * Network message for game communication
 * Uses JSON serialization instead of Java serialization
 */
public class NetworkMessage {
    
    public enum MessageType {
        MOVE,
        SURRENDER,
        CANCEL,
        CHAT,
        SYNC
    }
    
    public MessageType type;
    public Move move;
    public String message;
    public long timestamp;
    
    public NetworkMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public NetworkMessage(MessageType type) {
        this();
        this.type = type;
    }
    
    public NetworkMessage(MessageType type, Move move) {
        this(type);
        this.move = move;
    }
    
    public NetworkMessage(MessageType type, String message) {
        this(type);
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "NetworkMessage{type=" + type + 
               ", timestamp=" + timestamp + "}";
    }
}