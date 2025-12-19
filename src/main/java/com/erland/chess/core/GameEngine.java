package com.erland.chess.core;

import com.erland.chess.model.Board;
import com.erland.chess.model.GameState;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core game engine - handles all game logic
 * Single Responsibility: Game rules and state management
 */
public class GameEngine {
    private final Board board;
    private final GameStateManager stateManager;
    private final MoveValidator moveValidator;
    private final GameEventPublisher eventPublisher;
    
    public GameEngine(GameEventPublisher eventPublisher) {
        this.board = new Board();
        this.stateManager = new GameStateManager(board);
        this.moveValidator = new MoveValidator(board);
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Execute a move with full validation
     */
    public MoveResult executeMove(Piece piece, int toCol, int toRow) {
        // Validate
        ValidationResult validation = moveValidator.validate(piece, toCol, toRow);
        if (!validation.isValid()) {
            return MoveResult.failed(validation.getReason());
        }
        
        // Execute
        Move move = createMove(piece, toCol, toRow);
        boolean executed = board.movePiece(toCol, toRow);
        
        if (!executed) {
            return MoveResult.failed("Move execution failed");
        }
        
        // Update state
        stateManager.afterMove(move);
        
        // Publish event
        eventPublisher.publishMoveExecuted(move);
        
        // Check for special conditions
        if (isPromotionNeeded(move)) {
            return MoveResult.needsPromotion(move);
        }
        
        if (stateManager.isGameOver()) {
            eventPublisher.publishGameOver(stateManager.getGameState());
        }
        
        return MoveResult.success(move);
    }
    
    /**
     * Get all legal moves for a piece
     */
    public List<Move> getLegalMoves(Piece piece) {
        List<Move> legalMoves = new ArrayList<>();
        
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (moveValidator.validate(piece, col, row).isValid()) {
                    legalMoves.add(new Move(piece, piece.col, piece.row, col, row, 
                                           board.getPiece(col, row)));
                }
            }
        }
        
        return legalMoves;
    }
    
    /**
     * Promote pawn
     */
    public void promotePawn(int col, int row, String pieceType) {
        board.promotePawn(col, row, pieceType);
        stateManager.afterPromotion();
        eventPublisher.publishPromotion(col, row, pieceType);
    }
    
    /**
     * Undo last move
     */
    public Optional<Move> undoLastMove() {
        return stateManager.undoLastMove();
    }
    
    /**
     * Surrender current player
     */
    public void surrender(boolean whiteResigns) {
        board.surrender(whiteResigns);
        eventPublisher.publishGameOver(board.gameState);
    }
    
    // Helper methods
    
    private Move createMove(Piece piece, int toCol, int toRow) {
        return new Move(piece, piece.col, piece.row, toCol, toRow, 
                       board.getPiece(toCol, toRow));
    }
    
    private boolean isPromotionNeeded(Move move) {
        Piece piece = move.piece;
        return piece.name.equals("Pawn") && (move.toRow == 0 || move.toRow == 7);
    }
    
    // Getters
    
    public Board getBoard() {
        return board;
    }
    
    public GameState getGameState() {
        return board.gameState;
    }
    
    public boolean isWhiteTurn() {
        return board.isWhiteTurn;
    }
    
    public List<Move> getMoveHistory() {
        return new ArrayList<>(board.moveHistory);
    }
}