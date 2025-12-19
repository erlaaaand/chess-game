package com.erland.chess.network;

import com.erland.chess.model.Board;

public class NetworkMessage {
    // Hapus Serializable
    
    public enum MessageType {
        MOVE, SURRENDER, CANCEL
    }
    
    public MessageType type;
    public Board.Move move;
    
    public NetworkMessage(MessageType type) {
        this.type = type;
    }
}