package com.erland.chess.config;

/**
 * Konfigurasi global untuk game
 */
public class GameConfig {
    // Board settings
    public static final int TILE_SIZE = 85;
    public static final int BOARD_SIZE = TILE_SIZE * 8;
    
    // Animation settings
    public static final int MOVE_ANIMATION_DURATION = 300; // ms
    public static final int HIGHLIGHT_FADE_DURATION = 200; // ms
    public static final double DRAG_THRESHOLD = 5.0; // pixels
    
    // Game settings
    public static final boolean ENABLE_SOUNDS = true;
    public static final boolean ENABLE_ANIMATIONS = true;
    public static final boolean SHOW_LEGAL_MOVES = true;
    public static final boolean SHOW_COORDINATES = true;
    
    // AI settings
    public static final int AI_THINK_TIME_MIN = 500; // ms
    public static final int AI_THINK_TIME_MAX = 2000; // ms
    public static final String PYTHON_ENGINE_PATH = "ai_engine/ai_engine.py";
    
    // Network settings
    public static final int DEFAULT_PORT = 5555;
    public static final int NETWORK_TIMEOUT = 30000; // ms
    
    // Paths
    public static final String REVIEW_DIR = "game_reviews";
    public static final String BRIDGE_DIR = "python_bridge";
    public static final String AI_CHARACTERS_DIR = "ai_characters";
    
    // Colors
    public static final String COLOR_LIGHT_TILE = "#f0d9b5";
    public static final String COLOR_DARK_TILE = "#b58863";
    public static final String COLOR_SELECTED = "#f6f669";
    public static final String COLOR_VALID_MOVE = "#27ae60";
    public static final String COLOR_CAPTURE = "#e74c3c";
    public static final String COLOR_CHECK = "#e74c3c";
    
    private GameConfig() {
        // Private constructor to prevent instantiation
    }
}