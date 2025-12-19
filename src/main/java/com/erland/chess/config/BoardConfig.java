// Path: src/main/java/com/erland/chess/config/BoardConfig.java
package com.erland.chess.config;

import javafx.scene.paint.Color;

/**
 * Configuration constants for chess board display and behavior
 */
public class BoardConfig {
    
    // Board dimensions
    public static final int TILE_SIZE = 85;
    public static final int BOARD_SIZE = TILE_SIZE * 8;
    public static final int BOARD_PADDING = 20;
    
    // Animation durations (milliseconds)
    public static final int MOVE_ANIMATION_DURATION = 300;
    public static final int CAPTURE_ANIMATION_DURATION = 400;
    public static final int HIGHLIGHT_FADE_DURATION = 200;
    public static final int PROMOTION_ANIMATION_DURATION = 500;
    
    // Interaction settings
    public static final double DRAG_THRESHOLD = 5.0; // pixels
    public static final boolean ENABLE_DRAG_AND_DROP = true;
    public static final boolean ENABLE_CLICK_TO_MOVE = true;
    
    // Visual settings
    public static final boolean ENABLE_ANIMATIONS = true;
    public static final boolean ENABLE_SOUNDS = true;
    public static final boolean SHOW_LEGAL_MOVES = true;
    public static final boolean SHOW_COORDINATES = true;
    public static final boolean SHOW_MOVE_HISTORY = true;
    public static final boolean HIGHLIGHT_LAST_MOVE = true;
    public static final boolean SHOW_CHECK_INDICATOR = true;
    
    // Board colors
    public static final Color LIGHT_SQUARE_COLOR = Color.web("#f0d9b5");
    public static final Color DARK_SQUARE_COLOR = Color.web("#b58863");
    public static final Color SELECTED_SQUARE_COLOR = Color.web("#f6f669", 0.5);
    public static final Color LEGAL_MOVE_COLOR = Color.web("#27ae60", 0.7);
    public static final Color CAPTURE_MOVE_COLOR = Color.web("#e74c3c", 0.7);
    public static final Color CHECK_COLOR = Color.web("#e74c3c", 0.5);
    public static final Color LAST_MOVE_COLOR = Color.web("#ffd700", 0.3);
    
    // Piece colors for fallback
    public static final Color WHITE_PIECE_COLOR = Color.WHITE;
    public static final Color BLACK_PIECE_COLOR = Color.BLACK;
    
    // Border and effects
    public static final Color BOARD_BORDER_COLOR = Color.web("#8B4513");
    public static final int BOARD_BORDER_WIDTH = 3;
    public static final double SHADOW_RADIUS = 15.0;
    public static final Color SHADOW_COLOR = Color.web("#000000", 0.5);
    
    // Highlight sizes
    public static final double MOVE_INDICATOR_RADIUS = TILE_SIZE / 6.0;
    public static final double CAPTURE_INDICATOR_RADIUS = TILE_SIZE / 2.5;
    public static final int CAPTURE_CORNER_SIZE = TILE_SIZE / 5;
    
    // AI settings
    public static final int AI_MINIMUM_THINK_TIME = 500; // ms
    public static final int AI_MAXIMUM_THINK_TIME = 3000; // ms
    public static final boolean AI_SHOWS_THINKING = true;
    
    // Network settings
    public static final int DEFAULT_NETWORK_PORT = 5555;
    public static final int NETWORK_TIMEOUT = 30000; // ms
    public static final int HEARTBEAT_INTERVAL = 5000; // ms
    
    // Game rules
    public static final boolean ALLOW_UNDO = false;
    public static final boolean ALLOW_HINT = false;
    public static final boolean ENFORCE_TIME_LIMIT = false;
    public static final int DEFAULT_TIME_PER_MOVE = 0; // 0 = unlimited
    
    // File paths
    public static final String IMAGES_PATH = "/images/";
    public static final String SOUNDS_PATH = "/sounds/";
    public static final String MUSIC_PATH = "/music/";
    public static final String STYLES_PATH = "/styles/";
    
    // Resource names
    public static final String MAIN_STYLESHEET = "chess.css";
    public static final String CHESS_ICON = "chess_icon.png";
    
    // Save/Load
    public static final String SAVE_DIRECTORY = "saved_games";
    public static final String REVIEW_DIRECTORY = "game_reviews";
    public static final String AI_CHARACTERS_DIRECTORY = "ai_characters";
    public static final String PYTHON_BRIDGE_DIRECTORY = "python_bridge";
    
    // Promotion pieces order
    public static final String[] PROMOTION_PIECES = {"Queen", "Rook", "Bishop", "Knight"};
    public static final String[] PROMOTION_SYMBOLS = {"♕", "♖", "♗", "♘"};
    
    // Move notation
    public static final boolean USE_FIGURINE_NOTATION = false;
    public static final boolean USE_LONG_ALGEBRAIC = false;
    
    // Performance
    public static final boolean USE_HARDWARE_ACCELERATION = true;
    public static final int MAX_FPS = 60;
    public static final boolean CACHE_PIECE_IMAGES = true;
    
    // Debug
    public static final boolean DEBUG_MODE = false;
    public static final boolean SHOW_FPS = false;
    public static final boolean LOG_MOVES = true;
    
    private BoardConfig() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get piece image path
     */
    public static String getPieceImagePath(boolean isWhite, String pieceName) {
        String color = isWhite ? "w" : "b";
        String piece = pieceName.toLowerCase();
        return IMAGES_PATH + color + "_" + piece + ".png";
    }
    
    /**
     * Get sound file path
     */
    public static String getSoundPath(String soundName) {
        return SOUNDS_PATH + soundName;
    }
    
    /**
     * Validate tile coordinates
     */
    public static boolean isValidSquare(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }
    
    /**
     * Convert pixel coordinates to board square
     */
    public static int[] pixelToSquare(double x, double y) {
        int col = (int) (x / TILE_SIZE);
        int row = (int) (y / TILE_SIZE);
        
        if (isValidSquare(col, row)) {
            return new int[]{col, row};
        }
        return null;
    }
    
    /**
     * Convert board square to pixel coordinates (center of square)
     */
    public static double[] squareToPixel(int col, int row) {
        double x = col * TILE_SIZE + TILE_SIZE / 2.0;
        double y = row * TILE_SIZE + TILE_SIZE / 2.0;
        return new double[]{x, y};
    }
}