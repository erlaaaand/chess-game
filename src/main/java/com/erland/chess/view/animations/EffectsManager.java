// Path: src/main/java/com/erland/chess/view/animations/EffectsManager.java
package com.erland.chess.view.animations;

import javafx.animation.*;
import javafx.scene.effect.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * Manages visual effects for the chess game
 */
public class EffectsManager {
    private static final int TILE_SIZE = 85;
    
    private final Pane effectsLayer;
    
    public EffectsManager(Pane effectsLayer) {
        this.effectsLayer = effectsLayer;
    }
    
    /**
     * Show explosion effect at square
     */
    public void showExplosion(int col, int row, Color color) {
        double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
        
        // Create multiple particles
        for (int i = 0; i < 12; i++) {
            Circle particle = new Circle(3, color);
            particle.setCenterX(centerX);
            particle.setCenterY(centerY);
            
            effectsLayer.getChildren().add(particle);
            
            double angle = (360.0 / 12) * i;
            double distance = 40 + Math.random() * 20;
            
            TranslateTransition move = new TranslateTransition(
                Duration.millis(600), particle
            );
            move.setToX(Math.cos(Math.toRadians(angle)) * distance);
            move.setToY(Math.sin(Math.toRadians(angle)) * distance);
            
            FadeTransition fade = new FadeTransition(Duration.millis(600), particle);
            fade.setToValue(0);
            
            ScaleTransition scale = new ScaleTransition(Duration.millis(600), particle);
            scale.setToX(0);
            scale.setToY(0);
            
            ParallelTransition parallel = new ParallelTransition(move, fade, scale);
            parallel.setOnFinished(e -> effectsLayer.getChildren().remove(particle));
            parallel.play();
        }
    }
    
    /**
     * Show ripple effect at square
     */
    public void showRipple(int col, int row, Color color) {
        double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
        
        for (int i = 0; i < 3; i++) {
            Circle ripple = new Circle(centerX, centerY, 10);
            ripple.setFill(Color.TRANSPARENT);
            ripple.setStroke(color);
            ripple.setStrokeWidth(3);
            
            effectsLayer.getChildren().add(ripple);
            
            ScaleTransition scale = new ScaleTransition(
                Duration.millis(800), ripple
            );
            scale.setDelay(Duration.millis(i * 150));
            scale.setFromX(0.1);
            scale.setFromY(0.1);
            scale.setToX(3.0);
            scale.setToY(3.0);
            
            FadeTransition fade = new FadeTransition(Duration.millis(800), ripple);
            fade.setDelay(Duration.millis(i * 150));
            fade.setFromValue(0.8);
            fade.setToValue(0);
            
            ParallelTransition parallel = new ParallelTransition(scale, fade);
            parallel.setOnFinished(e -> effectsLayer.getChildren().remove(ripple));
            parallel.play();
        }
    }
    
    /**
     * Show lightning effect between two squares
     */
    public void showLightning(int fromCol, int fromRow, int toCol, int toRow) {
        double x1 = fromCol * TILE_SIZE + TILE_SIZE / 2.0;
        double y1 = fromRow * TILE_SIZE + TILE_SIZE / 2.0;
        double x2 = toCol * TILE_SIZE + TILE_SIZE / 2.0;
        double y2 = toRow * TILE_SIZE + TILE_SIZE / 2.0;
        
        Line lightning = new Line(x1, y1, x2, y2);
        lightning.setStroke(Color.web("#ffed4e"));
        lightning.setStrokeWidth(4);
        lightning.setEffect(new Glow(0.8));
        
        effectsLayer.getChildren().add(lightning);
        
        // Flash effect
        FadeTransition flash = new FadeTransition(Duration.millis(300), lightning);
        flash.setFromValue(1.0);
        flash.setToValue(0.0);
        flash.setCycleCount(3);
        flash.setAutoReverse(true);
        
        flash.setOnFinished(e -> effectsLayer.getChildren().remove(lightning));
        flash.play();
    }
    
    /**
     * Show glow effect at square
     */
    public void showGlow(int col, int row, Color color, Duration duration) {
        Rectangle glow = new Rectangle(
            col * TILE_SIZE,
            row * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        
        glow.setFill(color);
        glow.setOpacity(0);
        
        Bloom bloom = new Bloom(0.5);
        glow.setEffect(bloom);
        
        effectsLayer.getChildren().add(glow);
        
        FadeTransition fade = new FadeTransition(duration, glow);
        fade.setFromValue(0);
        fade.setToValue(0.6);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);
        
        fade.setOnFinished(e -> effectsLayer.getChildren().remove(glow));
        fade.play();
    }
    
    /**
     * Show sparkle effect
     */
    public void showSparkle(int col, int row) {
        double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
        
        for (int i = 0; i < 8; i++) {
            Star star = new Star(centerX, centerY, 3, 6, 5);
            star.setFill(Color.web("#ffd700"));
            star.setStroke(Color.web("#ffed4e"));
            star.setStrokeWidth(1);
            
            effectsLayer.getChildren().add(star);
            
            double angle = (360.0 / 8) * i;
            double distance = 30;
            
            TranslateTransition move = new TranslateTransition(
                Duration.millis(500), star
            );
            move.setToX(Math.cos(Math.toRadians(angle)) * distance);
            move.setToY(Math.sin(Math.toRadians(angle)) * distance);
            
            FadeTransition fade = new FadeTransition(Duration.millis(500), star);
            fade.setToValue(0);
            
            RotateTransition rotate = new RotateTransition(Duration.millis(500), star);
            rotate.setToAngle(360);
            
            ParallelTransition parallel = new ParallelTransition(move, fade, rotate);
            parallel.setOnFinished(e -> effectsLayer.getChildren().remove(star));
            parallel.play();
        }
    }
    
    /**
     * Show checkmate effect
     */
    public void showCheckmateEffect(int kingCol, int kingRow) {
        // Large explosion
        showExplosion(kingCol, kingRow, Color.web("#e74c3c"));
        
        // Glow
        showGlow(kingCol, kingRow, Color.web("#c0392b"), Duration.millis(1000));
        
        // Ripples
        new Timeline(new KeyFrame(Duration.millis(200), e -> {
            showRipple(kingCol, kingRow, Color.web("#e74c3c"));
        })).play();
    }
    
    /**
     * Show victory celebration effect
     */
    public void showVictoryCelebration() {
        // Create confetti across the board
        for (int i = 0; i < 50; i++) {
            double x = Math.random() * TILE_SIZE * 8;
            double y = -20;
            
            Rectangle confetti = new Rectangle(5, 10);
            confetti.setFill(Color.hsb(Math.random() * 360, 0.7, 0.9));
            confetti.setLayoutX(x);
            confetti.setLayoutY(y);
            
            effectsLayer.getChildren().add(confetti);
            
            TranslateTransition fall = new TranslateTransition(
                Duration.millis(2000 + Math.random() * 1000), confetti
            );
            fall.setToY(TILE_SIZE * 8 + 50);
            fall.setDelay(Duration.millis(Math.random() * 500));
            
            RotateTransition spin = new RotateTransition(
                Duration.millis(1000), confetti
            );
            spin.setToAngle(360 * (2 + Math.random() * 2));
            spin.setCycleCount(Animation.INDEFINITE);
            
            ParallelTransition parallel = new ParallelTransition(fall, spin);
            parallel.setOnFinished(e -> effectsLayer.getChildren().remove(confetti));
            parallel.play();
        }
    }
    
    /**
     * Clear all effects
     */
    public void clearEffects() {
        effectsLayer.getChildren().clear();
    }
    
    /**
     * Inner class for star shape
     */
    private static class Star extends Polygon {
        public Star(double centerX, double centerY, double innerRadius, 
                   double outerRadius, int points) {
            
            double angleStep = Math.PI / points;
            
            for (int i = 0; i < points * 2; i++) {
                double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                double angle = i * angleStep - Math.PI / 2;
                
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                
                getPoints().addAll(x, y);
            }
        }
    }
}