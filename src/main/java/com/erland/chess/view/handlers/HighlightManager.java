package com.erland.chess.view.handlers;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.King;
import com.erland.chess.model.pieces.Piece;

/**
 * Manages all visual highlights on the chess board
 */
public class HighlightManager {
    private static final int TILE_SIZE = 85;
    
    private final Board board;
    private final Pane highlightLayer;
    
    public HighlightManager(Board board, Pane highlightLayer) {
        this.board = board;
        this.highlightLayer = highlightLayer;
    }
    
    public void updateHighlights(Piece selectedPiece) {
        clearHighlights();
        
        if (selectedPiece == null) {
            return;
        }
        
        // Highlight selected square
        highlightSelectedSquare(selectedPiece.col, selectedPiece.row);
        
        // Highlight valid moves
        highlightValidMoves(selectedPiece);
        
        // Highlight king in check
        highlightCheckIfNeeded();
    }
    
    public void clearHighlights() {
        highlightLayer.getChildren().clear();
    }
    
    private void highlightSelectedSquare(int col, int row) {
        Rectangle selectedRect = new Rectangle(
            col * TILE_SIZE,
            row * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        selectedRect.setFill(Color.web("#f6f669", 0.5));
        selectedRect.setStroke(Color.web("#f6f669"));
        selectedRect.setStrokeWidth(3);
        highlightLayer.getChildren().add(selectedRect);
    }
    
    private void highlightValidMoves(Piece piece) {
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (!piece.canMove(col, row)) {
                    continue;
                }
                
                if (board.wouldBeInCheckAfterMove(piece, col, row)) {
                    continue;
                }
                
                Piece target = board.getPiece(col, row);
                
                if (target != null) {
                    // Capture indicator
                    highlightCaptureSquare(col, row);
                } else {
                    // Regular move indicator
                    highlightMoveSquare(col, row);
                }
            }
        }
    }
    
    private void highlightMoveSquare(int col, int row) {
        Circle dot = new Circle(
            col * TILE_SIZE + TILE_SIZE / 2.0,
            row * TILE_SIZE + TILE_SIZE / 2.0,
            TILE_SIZE / 6.0
        );
        dot.setFill(Color.web("#27ae60", 0.7));
        dot.setStroke(Color.web("#229954"));
        dot.setStrokeWidth(2);
        highlightLayer.getChildren().add(dot);
    }
    
    private void highlightCaptureSquare(int col, int row) {
        // Outer ring for capture
        Circle outerCircle = new Circle(
            col * TILE_SIZE + TILE_SIZE / 2.0,
            row * TILE_SIZE + TILE_SIZE / 2.0,
            TILE_SIZE / 2.5
        );
        outerCircle.setFill(Color.TRANSPARENT);
        outerCircle.setStroke(Color.web("#e74c3c"));
        outerCircle.setStrokeWidth(4);
        
        // Corner indicators
        double cornerSize = TILE_SIZE / 5.0;
        double offset = TILE_SIZE / 10.0;
        
        // Top-left corner
        Rectangle tlCorner = createCornerIndicator(
            col * TILE_SIZE + offset,
            row * TILE_SIZE + offset,
            cornerSize, cornerSize, true, true
        );
        
        // Top-right corner
        Rectangle trCorner = createCornerIndicator(
            col * TILE_SIZE + TILE_SIZE - offset - cornerSize,
            row * TILE_SIZE + offset,
            cornerSize, cornerSize, false, true
        );
        
        // Bottom-left corner
        Rectangle blCorner = createCornerIndicator(
            col * TILE_SIZE + offset,
            row * TILE_SIZE + TILE_SIZE - offset - cornerSize,
            cornerSize, cornerSize, true, false
        );
        
        // Bottom-right corner
        Rectangle brCorner = createCornerIndicator(
            col * TILE_SIZE + TILE_SIZE - offset - cornerSize,
            row * TILE_SIZE + TILE_SIZE - offset - cornerSize,
            cornerSize, cornerSize, false, false
        );
        
        highlightLayer.getChildren().addAll(outerCircle, tlCorner, trCorner, blCorner, brCorner);
    }
    
    private Rectangle createCornerIndicator(double x, double y, double w, double h,
                                           boolean left, boolean top) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.web("#e74c3c"));
        rect.setStrokeWidth(3);
        
        // Only show two sides of the corner
        if (left && top) {
            // Top-left: show left and top
            rect.setStyle("-fx-stroke-line-cap: square;");
        }
        
        return rect;
    }
    
    private void highlightCheckIfNeeded() {
        if (board.whiteInCheck) {
            highlightKingInCheck(true);
        }
        
        if (board.blackInCheck) {
            highlightKingInCheck(false);
        }
    }
    
    private void highlightKingInCheck(boolean isWhite) {
        King king = findKing(isWhite);
        
        if (king == null) {
            return;
        }
        
        Rectangle checkRect = new Rectangle(
            king.col * TILE_SIZE,
            king.row * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        
        checkRect.setFill(Color.web("#e74c3c", 0.5));
        checkRect.setStroke(Color.web("#c0392b"));
        checkRect.setStrokeWidth(4);
        checkRect.setEffect(new javafx.scene.effect.Glow(0.8));
        
        // Pulsing animation
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(800), checkRect
        );
        fade.setFromValue(0.5);
        fade.setToValue(0.8);
        fade.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
        
        highlightLayer.getChildren().add(checkRect);
    }
    
    private King findKing(boolean isWhite) {
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                Piece p = board.getPiece(c, r);
                if (p instanceof King && p.isWhite == isWhite) {
                    return (King) p;
                }
            }
        }
        return null;
    }
    
    public void highlightLastMove(Board.Move move) {
        if (move == null) {
            return;
        }
        
        // Highlight from square
        Rectangle fromRect = new Rectangle(
            move.fromCol * TILE_SIZE,
            move.fromRow * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        fromRect.setFill(Color.web("#ffd700", 0.3));
        
        // Highlight to square
        Rectangle toRect = new Rectangle(
            move.toCol * TILE_SIZE,
            move.toRow * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        toRect.setFill(Color.web("#ffd700", 0.5));
        
        highlightLayer.getChildren().addAll(fromRect, toRect);
    }
}