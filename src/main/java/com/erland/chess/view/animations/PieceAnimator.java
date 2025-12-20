package com.erland.chess.view.animations;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import com.erland.chess.model.Move;
import com.erland.chess.model.pieces.Piece;

/**
 * Handles smooth animations for chess piece movements
 */
public class PieceAnimator {
    private static final int TILE_SIZE = 85;
    private static final Duration MOVE_DURATION = Duration.millis(300);
    private static final Duration CAPTURE_DURATION = Duration.millis(400);
    private static final Duration FADE_DURATION = Duration.millis(200);
    
    private final Pane animationLayer;
    
    public PieceAnimator(Pane animationLayer) {
        this.animationLayer = animationLayer;
    }
    
    /**
     * Animate piece movement from one square to another
     */
    // Fixed: Changed return type to Animation
    public Animation animateMove(Piece piece, int fromCol, int fromRow, 
                                int toCol, int toRow, Runnable onComplete) {
        
        // Create visual representation of the piece
        Rectangle pieceRect = createPieceVisual(piece);
        
        double startX = fromCol * TILE_SIZE + 5;
        double startY = fromRow * TILE_SIZE + 5;
        double endX = toCol * TILE_SIZE + 5;
        double endY = toRow * TILE_SIZE + 5;
        
        pieceRect.setLayoutX(startX);
        pieceRect.setLayoutY(startY);
        
        animationLayer.getChildren().add(pieceRect);
        
        // Create smooth movement animation
        TranslateTransition move = new TranslateTransition(MOVE_DURATION, pieceRect);
        move.setFromX(0);
        move.setFromY(0);
        move.setToX(endX - startX);
        move.setToY(endY - startY);
        move.setInterpolator(Interpolator.EASE_BOTH);
        
        // Add scale effect during movement
        ScaleTransition scale = new ScaleTransition(MOVE_DURATION, pieceRect);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        
        ParallelTransition parallel = new ParallelTransition(move, scale);
        
        parallel.setOnFinished(e -> {
            animationLayer.getChildren().remove(pieceRect);
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        parallel.play();
        return parallel;
    }
    
    /**
     * Animate piece capture with fade out effect
     */
    // Fixed: Changed return type to Animation
    public Animation animateCapture(Piece capturedPiece, int col, int row, 
                                   Runnable onComplete) {
        
        Rectangle pieceRect = createPieceVisual(capturedPiece);
        pieceRect.setLayoutX(col * TILE_SIZE + 5);
        pieceRect.setLayoutY(row * TILE_SIZE + 5);
        
        animationLayer.getChildren().add(pieceRect);
        
        // Fade out and shrink
        FadeTransition fade = new FadeTransition(CAPTURE_DURATION, pieceRect);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        
        ScaleTransition scale = new ScaleTransition(CAPTURE_DURATION, pieceRect);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.3);
        scale.setToY(0.3);
        
        RotateTransition rotate = new RotateTransition(CAPTURE_DURATION, pieceRect);
        rotate.setFromAngle(0);
        rotate.setToAngle(360);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale, rotate);
        
        parallel.setOnFinished(e -> {
            animationLayer.getChildren().remove(pieceRect);
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        parallel.play();
        return parallel;
    }
    
    /**
     * Animate castling move (king and rook)
     */
    public void animateCastling(Piece king, int kingFromCol, int kingToCol, int row,
                               Piece rook, int rookFromCol, int rookToCol,
                               Runnable onComplete) {
        
        Animation kingAnim = animateMove(king, kingFromCol, row, kingToCol, row, null);
        Animation rookAnim = animateMove(rook, rookFromCol, row, rookToCol, row, null);
        
        ParallelTransition parallel = new ParallelTransition(kingAnim, rookAnim);
        parallel.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        parallel.play();
    }
    
    /**
     * Animate pawn promotion
     */
    public void animatePromotion(Piece pawn, int col, int row, 
                                String promotionPiece, Runnable onComplete) {
        
        Rectangle oldPiece = createPieceVisual(pawn);
        oldPiece.setLayoutX(col * TILE_SIZE + 5);
        oldPiece.setLayoutY(row * TILE_SIZE + 5);
        
        animationLayer.getChildren().add(oldPiece);
        
        // Fade out old piece
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, oldPiece);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        ScaleTransition shrink = new ScaleTransition(FADE_DURATION, oldPiece);
        shrink.setToX(0.5);
        shrink.setToY(0.5);
        
        ParallelTransition disappear = new ParallelTransition(fadeOut, shrink);
        
        disappear.setOnFinished(e -> {
            animationLayer.getChildren().remove(oldPiece);
            
            // Create new promoted piece
            Rectangle newPiece = createPromotedPieceVisual(
                pawn.isWhite, promotionPiece, col, row
            );
            newPiece.setOpacity(0);
            newPiece.setScaleX(0.5);
            newPiece.setScaleY(0.5);
            
            animationLayer.getChildren().add(newPiece);
            
            // Fade in new piece
            FadeTransition fadeIn = new FadeTransition(FADE_DURATION, newPiece);
            fadeIn.setToValue(1.0);
            
            ScaleTransition grow = new ScaleTransition(FADE_DURATION, newPiece);
            grow.setToX(1.0);
            grow.setToY(1.0);
            
            ParallelTransition appear = new ParallelTransition(fadeIn, grow);
            appear.setOnFinished(evt -> {
                animationLayer.getChildren().remove(newPiece);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            
            appear.play();
        });
        
        disappear.play();
    }
    
    /**
     * Animate check warning
     */
    public void animateCheck(int kingCol, int kingRow) {
        Rectangle highlight = new Rectangle(
            kingCol * TILE_SIZE,
            kingRow * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        
        highlight.setFill(Color.web("#e74c3c", 0.5));
        highlight.setStroke(Color.web("#c0392b"));
        highlight.setStrokeWidth(4);
        
        animationLayer.getChildren().add(highlight);
        
        // Pulsing animation
        FadeTransition pulse = new FadeTransition(Duration.millis(600), highlight);
        pulse.setFromValue(0.7);
        pulse.setToValue(0.3);
        pulse.setCycleCount(4);
        pulse.setAutoReverse(true);
        
        pulse.setOnFinished(e -> animationLayer.getChildren().remove(highlight));
        pulse.play();
    }
    
    /**
     * Create visual representation of a piece
     */
    private Rectangle createPieceVisual(Piece piece) {
        Rectangle rect = new Rectangle(TILE_SIZE - 10, TILE_SIZE - 10);
        
        String color = piece.isWhite ? "w" : "b";
        String pieceName = piece.name.toLowerCase();
        
        try {
            Image img = new Image(
                getClass().getResourceAsStream("/images/" + color + "_" + pieceName + ".png"),
                TILE_SIZE - 10, TILE_SIZE - 10, true, true
            );
            rect.setFill(new ImagePattern(img));
        } catch (Exception e) {
            rect.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
        }
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        rect.setEffect(shadow);
        
        return rect;
    }
    
    /**
     * Create visual for promoted piece
     */
    private Rectangle createPromotedPieceVisual(boolean isWhite, String pieceType, 
                                               int col, int row) {
        Rectangle rect = new Rectangle(TILE_SIZE - 10, TILE_SIZE - 10);
        rect.setLayoutX(col * TILE_SIZE + 5);
        rect.setLayoutY(row * TILE_SIZE + 5);
        
        String color = isWhite ? "w" : "b";
        String pieceName = pieceType.toLowerCase();
        
        try {
            Image img = new Image(
                getClass().getResourceAsStream("/images/" + color + "_" + pieceName + ".png"),
                TILE_SIZE - 10, TILE_SIZE - 10, true, true
            );
            rect.setFill(new ImagePattern(img));
        } catch (Exception e) {
            rect.setFill(isWhite ? Color.WHITE : Color.BLACK);
        }
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#ffd700"));
        shadow.setRadius(15);
        rect.setEffect(shadow);
        
        return rect;
    }
    
    /**
     * Clear all animations
     */
    public void clearAnimations() {
        animationLayer.getChildren().clear();
    }
    
    /**
     * Animate move with capture
     */
    public void animateMoveWithCapture(Move move, Runnable onComplete) {
        if (move.capturedPiece != null) {
            // First animate capture
            animateCapture(move.capturedPiece, move.toCol, move.toRow, () -> {
                // Then animate move
                animateMove(move.piece, move.fromCol, move.fromRow, 
                          move.toCol, move.toRow, onComplete);
            });
        } else {
            // Just animate move
            animateMove(move.piece, move.fromCol, move.fromRow, 
                      move.toCol, move.toRow, onComplete);
        }
    }
}