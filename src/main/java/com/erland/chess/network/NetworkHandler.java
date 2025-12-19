package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.view.BoardPanel;

public interface NetworkHandler {
    void sendMove(Board.Move move);
    void sendSurrender();
    void sendCancel();
    void setBoardPanel(BoardPanel panel);
    void close();
}