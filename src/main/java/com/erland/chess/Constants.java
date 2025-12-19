package com.erland.chess;

/**
 * Application-wide constants - Single Source of Truth
 */
public final class Constants {
    
    // ==================== BOARD CONFIGURATION ====================
    public static final int BOARD_SIZE = 8;
    public static final int TILE_SIZE = 85;
    public static final int BOARD_PIXEL_SIZE = TILE_SIZE * BOARD_SIZE;
    
    // ==================== ANIMATION SETTINGS ====================
    public static final int MOVE_ANIMATION_DURATION = 300;
    public static final int CAPTURE_ANIMATION_DURATION = 400;
    public static final int PROMOTION_ANIMATION_DURATION = 500;
    public static final int HIGHLIGHT_FADE_DURATION = 200;
    
    // ==================== AI SETTINGS ====================
    public static final int AI_MIN_THINK_TIME = 500;
    public static final int AI_MAX_THINK_TIME = 3000;
    public static final int AI_DEFAULT_TIME_LIMIT = 1000;
    
    // ==================== NETWORK SETTINGS ====================
    public static final int DEFAULT_NETWORK_PORT = 5555;
    public static final int NETWORK_TIMEOUT = 30000;
    public static final int HEARTBEAT_INTERVAL = 5000;
    
    // ==================== FILE PATHS ====================
    public static final String IMAGES_PATH = "/images/";
    public static final String SOUNDS_PATH = "/sounds/";
    public static final String STYLES_PATH = "/styles/";
    public static final String MAIN_STYLESHEET = "chess.css";
    
    // ==================== DIRECTORY PATHS ====================
    public static final String SAVE_DIR = "saved_games";
    public static final String REVIEW_DIR = "game_reviews";
    public static final String AI_CHARACTERS_DIR = "ai_characters";
    public static final String PYTHON_ENGINE_PATH = "ai_engine/ai_engine.py";
    
    // ==================== COLORS (HEX) ====================
    public static final String COLOR_LIGHT_SQUARE = "#f0d9b5";
    public static final String COLOR_DARK_SQUARE = "#b58863";
    public static final String COLOR_SELECTED = "#f6f669";
    public static final String COLOR_LEGAL_MOVE = "#27ae60";
    public static final String COLOR_CAPTURE = "#e74c3c";
    public static final String COLOR_CHECK = "#e74c3c";
    public static final String COLOR_LAST_MOVE = "#ffd700";
    
    // ==================== PIECE VALUES ====================
    public static final int PAWN_VALUE = 100;
    public static final int KNIGHT_VALUE = 320;
    public static final int BISHOP_VALUE = 330;
    public static final int ROOK_VALUE = 500;
    public static final int QUEEN_VALUE = 900;
    public static final int KING_VALUE = 20000;
    
    // ==================== PROMOTION ====================
    public static final String[] PROMOTION_PIECES = {"Queen", "Rook", "Bishop", "Knight"};
    public static final String[] PROMOTION_SYMBOLS = {"♕", "♖", "♗", "♘"};
    
    // ==================== PIECE SYMBOLS ====================
    public static final String KING_SYMBOL = "♔";
    public static final String QUEEN_SYMBOL = "♕";
    public static final String ROOK_SYMBOL = "♖";
    public static final String BISHOP_SYMBOL = "♗";
    public static final String KNIGHT_SYMBOL = "♘";
    public static final String PAWN_SYMBOL = "♙";
    
    // ==================== INTERACTION ====================
    public static final double DRAG_THRESHOLD = 5.0;
    public static final boolean ENABLE_DRAG_AND_DROP = true;
    public static final boolean ENABLE_CLICK_TO_MOVE = true;
    
    // ==================== VISUAL SETTINGS ====================
    public static final boolean ENABLE_ANIMATIONS = true;
    public static final boolean ENABLE_SOUNDS = true;
    public static final boolean SHOW_LEGAL_MOVES = true;
    public static final boolean SHOW_COORDINATES = true;
    public static final boolean HIGHLIGHT_LAST_MOVE = true;
    
    // ==================== UTILITY METHODS ====================
    
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
        return col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE;
    }
    
    /**
     * Convert pixel coordinates to board square
     */
    public static int[] pixelToSquare(double x, double y) {
        int col = (int) (x / TILE_SIZE);
        int row = (int) (y / TILE_SIZE);
        
        return isValidSquare(col, row) ? new int[]{col, row} : null;
    }
    
    /**
     * Convert board square to pixel coordinates (center)
     */
    public static double[] squareToPixelCenter(int col, int row) {
        double x = col * TILE_SIZE + TILE_SIZE / 2.0;
        double y = row * TILE_SIZE + TILE_SIZE / 2.0;
        return new double[]{x, y};
    }
    
    private Constants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}