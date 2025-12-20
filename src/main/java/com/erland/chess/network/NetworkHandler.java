package com.erland.chess.network;

import com.erland.chess.model.Move;
import com.erland.chess.view.BoardView;

public interface NetworkHandler {
    void sendMove(Move move);
    void sendSurrender();
    void sendCancel();
    void setBoardPanel(BoardView panel);
    void close();
    default boolean connect() { return true; } // For GameClient
}