// Path: src/main/java/com/erland/chess/ai/AIPlayer.java
package com.erland.chess.ai;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI Player implementation with different difficulty levels
 */
public class AIPlayer {
    private final AICharacter character;
    private final PythonBridge pythonBridge;
    private final int thinkingTime;
    
    public AIPlayer(AICharacter character) {
        this(character, 1000); // Default 1 second thinking time
    }
    
    public AIPlayer(AICharacter character, int thinkingTimeMs) {
        this.character = character;
        this.pythonBridge = PythonBridge.getInstance();
        this.thinkingTime = thinkingTimeMs;
    }
    
    /**
     * Get AI's move asynchronously
     */
    public CompletableFuture<Move> getMove(Board board) {
        // Try using Python engine first
        return pythonBridge.getBestMove(board, character, thinkingTime)
            .thenApply(moveUCI -> {
                if (moveUCI != null && !moveUCI.equals("none")) {
                    Move move = parseMoveFromUCI(board, moveUCI);
                    if (move != null) {
                        return move;
                    }
                }
                // Fallback to Java implementation
                return getFallbackMove(board);
            })
            .exceptionally(ex -> {
                System.err.println("AI error: " + ex.getMessage());
                return getFallbackMove(board);
            });
    }
    
    /**
     * Fallback move selection using Java
     */
    private Move getFallbackMove(Board board) {
        List<MoveOption> validMoves = generateValidMoves(board);
        
        if (validMoves.isEmpty()) {
            return null;
        }
        
        // Score all moves
        for (MoveOption option : validMoves) {
            option.score = evaluateMove(board, option.move);
        }
        
        // Sort by score
        validMoves.sort(Comparator.comparingDouble(m -> -m.score));
        
        // Apply personality-based selection
        return selectMoveWithPersonality(validMoves);
    }
    
    /**
     * Generate all valid moves
     */
    private List<MoveOption> generateValidMoves(Board board) {
        List<MoveOption> moves = new ArrayList<>();
        
        for (int fromCol = 0; fromCol < 8; fromCol++) {
            for (int fromRow = 0; fromRow < 8; fromRow++) {
                Piece piece = board.getPiece(fromCol, fromRow);
                
                if (piece != null && !piece.isWhite) {
                    for (int toCol = 0; toCol < 8; toCol++) {
                        for (int toRow = 0; toRow < 8; toRow++) {
                            if (piece.canMove(toCol, toRow) && 
                                !board.wouldBeInCheckAfterMove(piece, toCol, toRow)) {
                                
                                Piece captured = board.getPiece(toCol, toRow);
                                Move move = new Move(piece, fromCol, fromRow, 
                                                    toCol, toRow, captured);
                                moves.add(new MoveOption(move));
                            }
                        }
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Evaluate move quality
     */
    private double evaluateMove(Board board, Move move) {
        double score = 0.0;
        
        // Simulate move
        Piece piece = move.piece;
        int oldCol = piece.col;
        int oldRow = piece.row;
        Piece capturedPiece = board.getPiece(move.toCol, move.toRow);
        
        board.pieceList[oldCol][oldRow] = null;
        board.pieceList[move.toCol][move.toRow] = piece;
        piece.col = move.toCol;
        piece.row = move.toRow;
        
        // Material evaluation
        score += evaluateMaterial(board);
        
        // Position evaluation
        score += evaluatePosition(board, piece);
        
        // Tactical evaluation
        if (board.isKingInCheck(!piece.isWhite)) {
            score += 50;
        }
        
        // Capture bonus
        if (capturedPiece != null) {
            score += getPieceValue(capturedPiece) * 0.5;
        }
        
        // Center control
        if (isCenter(move.toCol, move.toRow)) {
            score += 15;
        }
        
        // Undo move
        board.pieceList[oldCol][oldRow] = piece;
        board.pieceList[move.toCol][move.toRow] = capturedPiece;
        piece.col = oldCol;
        piece.row = oldRow;
        
        return score;
    }
    
    /**
     * Evaluate material balance
     */
    private double evaluateMaterial(Board board) {
        double score = 0;
        
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                Piece piece = board.getPiece(col, row);
                if (piece != null) {
                    double value = getPieceValue(piece);
                    score += piece.isWhite ? -value : value;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Evaluate piece position
     */
    private double evaluatePosition(Board board, Piece piece) {
        double score = 0.0;
        
        // Prefer central positions
        int centerDistance = Math.abs(piece.col - 3) + Math.abs(piece.row - 3);
        score -= centerDistance * 2;
        
        // Mobility bonus
        int mobility = 0;
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (piece.canMove(col, row)) {
                    mobility++;
                }
            }
        }
        score += mobility * 2;
        
        return score;
    }
    
    /**
     * Select move based on AI personality
     */
    private Move selectMoveWithPersonality(List<MoveOption> moves) {
        if (moves.isEmpty()) {
            return null;
        }
        
        double riskTaking = character.getRiskTaking();
        
        if (riskTaking > 0.7) {
            // High risk: sometimes pick aggressive moves
            if (Math.random() < 0.3 && moves.size() > 3) {
                return moves.get(new Random().nextInt(Math.min(5, moves.size()))).move;
            }
        } else if (riskTaking < 0.3) {
            // Low risk: always pick safest move
            return moves.get(0).move;
        }
        
        // Normal: pick best with slight randomness
        if (Math.random() < 0.1 && moves.size() > 1) {
            return moves.get(1).move;
        }
        
        return moves.get(0).move;
    }
    
    /**
     * Parse UCI move string to Move object
     */
    private Move parseMoveFromUCI(Board board, String uci) {
        if (uci == null || uci.length() < 4) {
            return null;
        }
        
        try {
            int fromCol = uci.charAt(0) - 'a';
            int fromRow = 8 - (uci.charAt(1) - '0');
            int toCol = uci.charAt(2) - 'a';
            int toRow = 8 - (uci.charAt(3) - '0');
            
            Piece piece = board.getPiece(fromCol, fromRow);
            if (piece == null) {
                return null;
            }
            
            Piece captured = board.getPiece(toCol, toRow);
            return new Move(piece, fromCol, fromRow, toCol, toRow, captured);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if position is center
     */
    private boolean isCenter(int col, int row) {
        return (col >= 2 && col <= 5 && row >= 2 && row <= 5);
    }
    
    /**
     * Get piece value
     */
    private double getPieceValue(Piece piece) {
        String name = piece.name;
        switch (name) {
            case "Pawn": return 100;
            case "Knight": return 320;
            case "Bishop": return 330;
            case "Rook": return 500;
            case "Queen": return 900;
            case "King": return 20000;
            default: return 0;
        }
    }
    
    public AICharacter getCharacter() {
        return character;
    }
    
    /**
     * Internal class for move options
     */
    private static class MoveOption {
        Move move;
        double score;
        
        MoveOption(Move move) {
            this.move = move;
            this.score = 0.0;
        }
    }
}