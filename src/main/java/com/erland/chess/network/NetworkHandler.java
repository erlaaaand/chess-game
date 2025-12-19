package com.erland.chess.network;

import com.erland.chess.model.Board;
import com.erland.chess.view.BoardView;

public interface NetworkHandler {
    void sendMove(Board.Move move);
    void sendSurrender();
    void sendCancel();
    void setBoardPanel(BoardView panel);
    void close();
}