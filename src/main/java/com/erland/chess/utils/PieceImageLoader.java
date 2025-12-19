package com.erland.chess.util;

import javafx.scene.image.Image;
import com.erland.chess.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton for loading and caching piece images
 * Prevents redundant I/O operations
 */
public class PieceImageLoader {
    private static PieceImageLoader instance;
    private final Map<String, Image> imageCache = new HashMap<>();
    
    private PieceImageLoader() {
        preloadImages();
    }
    
    public static synchronized PieceImageLoader getInstance() {
        if (instance == null) {
            instance = new PieceImageLoader();
        }
        return instance;
    }
    
    /**
     * Get piece image (cached)
     */
    public Image getImage(boolean isWhite, String pieceName) {
        String key = (isWhite ? "w_" : "b_") + pieceName.toLowerCase();
        return imageCache.get(key);
    }
    
    /**
     * Preload all images at startup
     */
    private void preloadImages() {
        String[] colors = {"w", "b"};
        String[] pieces = {"king", "queen", "rook", "bishop", "knight", "pawn"};
        
        for (String color : colors) {
            for (String piece : pieces) {
                String key = color + "_" + piece;
                String path = Constants.IMAGES_PATH + key + ".png";
                
                try {
                    Image img = new Image(
                        getClass().getResourceAsStream(path),
                        Constants.TILE_SIZE - 10,
                        Constants.TILE_SIZE - 10,
                        true,
                        true
                    );
                    imageCache.put(key, img);
                } catch (Exception e) {
                    System.err.println("Failed to load image: " + path);
                }
            }
        }
        
        System.out.println("Loaded " + imageCache.size() + " piece images");
    }
    
    /**
     * Clear cache (for memory management)
     */
    public void clearCache() {
        imageCache.clear();
    }
}