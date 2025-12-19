package com.erland.chess.network;

import com.erland.chess.model.Board;
import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        MOVE, SURRENDER, CANCEL
    }
    
    public MessageType type;
    public Board.Move move;
    
    public NetworkMessage(MessageType type) {
        this.type = type;
    }
}