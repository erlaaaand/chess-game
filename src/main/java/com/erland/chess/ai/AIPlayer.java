package com.erland.chess.ai;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.utils.NotationConverter;
import java.util.concurrent.CompletableFuture;

public class AIPlayer {
    private final AICharacter character;
    private final int timeLimitMs = 1000; // Waktu berpikir default

    public AIPlayer(AICharacter character) {
        this.character = character;
    }

    public CompletableFuture<Move> getMove(Board board) {
        // Panggil Python Bridge secara async
        return PythonBridge.getInstance()
            .getBestMove(board, character, timeLimitMs)
            .thenApply(moveString -> parseMoveString(board, moveString));
    }

    private Move parseMoveString(Board board, String uciMove) {
        if (uciMove == null || uciMove.length() < 4) return null;

        // Parse format UCI (contoh: "e2e4")
        String fromSquare = uciMove.substring(0, 2);
        String toSquare = uciMove.substring(2, 4);

        int[] from = NotationConverter.fromAlgebraic(fromSquare);
        int[] to = NotationConverter.fromAlgebraic(toSquare);

        if (from == null || to == null) return null;

        Piece piece = board.getPiece(from[0], from[1]);
        if (piece == null) return null;

        // Cek jika ada promosi (contoh: "a7a8q")
        String promotion = null;
        if (uciMove.length() == 5) {
            char promoChar = uciMove.charAt(4);
            switch (promoChar) {
                case 'q': promotion = "Queen"; break;
                case 'r': promotion = "Rook"; break;
                case 'b': promotion = "Bishop"; break;
                case 'n': promotion = "Knight"; break;
            }
        }

        Move move = new Move(piece, from[0], from[1], to[0], to[1], board.getPiece(to[0], to[1]));
        move.promotionPiece = promotion;
        return move;
    }
}