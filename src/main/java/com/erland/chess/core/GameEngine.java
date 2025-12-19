package com.erland.chess.core;

import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core game engine - handles all game logic flow
 */
public class GameEngine {
    private final Board board;
    private final GameStateManager stateManager;
    private final MoveValidator moveValidator;
    private final GameEventPublisher eventPublisher;
    
    // Kita butuh akses ke board yang sama dengan yang dipakai View
    public GameEngine(Board board) {
        this.board = board;
        this.stateManager = new GameStateManager(board);
        this.moveValidator = new MoveValidator(board);
        this.eventPublisher = new GameEventPublisher(); // Internal publisher
    }
    
    /**
     * Execute a move with full validation and state updates
     */
    public MoveResult executeMove(Piece piece, int toCol, int toRow) {
        // 1. Validate
        ValidationResult validation = moveValidator.validate(piece, toCol, toRow);
        if (!validation.isValid()) {
            return MoveResult.failed(validation.getReason());
        }
        
        // 2. Cek Promosi sebelum eksekusi (untuk UI handling)
        // Kita butuh tahu apakah ini gerakan pawn ke ujung
        boolean isPawnPromotion = piece.name.equals("Pawn") && (toRow == 0 || toRow == 7);
        if (isPawnPromotion) {
            // Kita kembalikan status butuh promosi, move BELUM dieksekusi di board
            // UI harus minta input user, lalu panggil promotePawn()
            // Namun, untuk menyederhanakan flow, kita bisa buat objek move sementara
            Move pendingMove = new Move(piece, piece.col, piece.row, toCol, toRow, board.getPiece(toCol, toRow));
            return MoveResult.needsPromotion(pendingMove);
        }

        // 3. Execute Move di Board
        // Catatan: Pastikan Board.movePiece() Anda hanya mengupdate posisi array & koordinat,
        // logika turn switching dll sebaiknya di sini, tapi untuk kompatibilitas dengan Board yang lama
        // kita biarkan Board melakukan hal dasarnya.
        boolean executed = board.movePiece(toCol, toRow);
        
        if (!executed) {
            return MoveResult.failed("Engine execution failed");
        }
        
        // Ambil move object yang baru saja dibuat di history Board
        Move lastMove = board.moveHistory.get(board.moveHistory.size() - 1);
        
        // 4. Update Game State (Check, Checkmate, Stalemate) handled by Board/GameStateManager internally
        // Tapi kita bisa paksa refresh state di sini jika perlu
        stateManager.afterMove(lastMove);
        
        // 5. Publish Event (Opsional, jika UI listen ke engine)
        eventPublisher.publishMoveExecuted(lastMove);
        
        return MoveResult.success(lastMove);
    }
    
    /**
     * Handle Pawn Promotion execution
     */
    public void promotePawn(int col, int row, String pieceType) {
        board.promotePawn(col, row, pieceType);
        stateManager.afterPromotion();
    }
    
    public Board getBoard() {
        return board;
    }
    
    public GameEventPublisher getEventPublisher() {
        return eventPublisher;
    }
}