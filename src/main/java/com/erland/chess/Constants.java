package com.erland.chess;

/**
 * Application-wide constants
 * Single source of truth for all constants
 */
public final class Constants {
    
    // Board dimensions
    public static final int BOARD_SIZE = 8;
    public static final int TILE_SIZE = 85;
    public static final int BOARD_PIXEL_SIZE = TILE_SIZE * BOARD_SIZE;
    
    // Animation durations (milliseconds)
    public static final int MOVE_ANIMATION_DURATION = 300;
    public static final int CAPTURE_ANIMATION_DURATION = 400;
    public static final int PROMOTION_ANIMATION_DURATION = 500;
    
    // AI settings
    public static final int AI_MIN_THINK_TIME = 500;
    public static final int AI_MAX_THINK_TIME = 3000;
    
    // Network settings
    public static final int DEFAULT_NETWORK_PORT = 5555;
    public static final int NETWORK_TIMEOUT = 30000;
    
    // File paths
    public static final String IMAGES_PATH = "/images/";
    public static final String SOUNDS_PATH = "/sounds/";
    public static final String STYLES_PATH = "/styles/";
    
    // Resource names
    public static final String MAIN_STYLESHEET = "chess.css";
    
    // Directory paths
    public static final String SAVE_DIR = "saved_games";
    public static final String REVIEW_DIR = "game_reviews";
    public static final String AI_CHARACTERS_DIR = "ai_characters";
    public static final String PYTHON_BRIDGE_DIR = "python_bridge";
    public static final String PYTHON_ENGINE_PATH = "ai_engine/ai_engine.py";
    
    // Colors (hex)
    public static final String COLOR_LIGHT_SQUARE = "#f0d9b5";
    public static final String COLOR_DARK_SQUARE = "#b58863";
    public static final String COLOR_SELECTED = "#f6f669";
    public static final String COLOR_LEGAL_MOVE = "#27ae60";
    public static final String COLOR_CAPTURE = "#e74c3c";
    public static final String COLOR_CHECK = "#e74c3c";
    
    // Piece values for evaluation
    public static final int PAWN_VALUE = 100;
    public static final int KNIGHT_VALUE = 320;
    public static final int BISHOP_VALUE = 330;
    public static final int ROOK_VALUE = 500;
    public static final int QUEEN_VALUE = 900;
    public static final int KING_VALUE = 20000;
    
    // Promotion pieces
    public static final String[] PROMOTION_PIECES = {"Queen", "Rook", "Bishop", "Knight"};
    public static final String[] PROMOTION_SYMBOLS = {"♕", "♖", "♗", "♘"};
    
    // Piece symbols
    public static final String KING_SYMBOL = "♔";
    public static final String QUEEN_SYMBOL = "♕";
    public static final String ROOK_SYMBOL = "♖";
    public static final String BISHOP_SYMBOL = "♗";
    public static final String KNIGHT_SYMBOL = "♘";
    public static final String PAWN_SYMBOL = "♙";
    
    private Constants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}