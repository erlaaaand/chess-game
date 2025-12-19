package com.erland.chess.review;

import com.erland.chess.model.Board;
import com.erland.chess.model.Board.Move;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameReviewer {
    private static final String REVIEW_DIR = "game_reviews";
    private static final String BRIDGE_DIR = "python_bridge";
    
    private String currentGameFile = null;
    private String currentTimestamp = null;
    
    public GameReviewer() {
        // Create directories if they don't exist
        File reviewDir = new File(REVIEW_DIR);
        File bridgeDir = new File(BRIDGE_DIR);
        
        if(!reviewDir.exists()) {
            reviewDir.mkdirs();
            System.out.println("Created directory: " + REVIEW_DIR);
        }
        
        if(!bridgeDir.exists()) {
            bridgeDir.mkdirs();
            System.out.println("Created directory: " + BRIDGE_DIR);
        }
    }
    
    /**
     * Start a new game session for live analysis
     */
    public void startNewGame() {
        currentTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        currentGameFile = BRIDGE_DIR + "/game_" + currentTimestamp + ".json";
        
        try {
            FileWriter writer = new FileWriter(currentGameFile);
            writer.write("{\n");
            writer.write("  \"timestamp\": \"" + currentTimestamp + "\",\n");
            writer.write("  \"result\": \"PLAYING\",\n");
            writer.write("  \"total_moves\": 0,\n");
            writer.write("  \"user_comment\": \"\",\n");
            writer.write("  \"moves\": [\n");
            writer.write("  ]\n");
            writer.write("}\n");
            writer.close();
            
            System.out.println("Live analysis started: " + currentGameFile);
        } catch (IOException e) {
            System.err.println("Error starting game review: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update game file with new move in real-time
     */
    public void recordMove(Board board) {
        if (currentGameFile == null || board.moveHistory.isEmpty()) {
            return;
        }
        
        try {
            // Write complete updated file
            FileWriter writer = new FileWriter(currentGameFile);
            
            writer.write("{\n");
            writer.write("  \"timestamp\": \"" + currentTimestamp + "\",\n");
            writer.write("  \"result\": \"" + board.gameState + "\",\n");
            writer.write("  \"total_moves\": " + board.totalMoves + ",\n");
            writer.write("  \"user_comment\": \"Live game in progress\",\n");
            writer.write("  \"moves\": [\n");
            
            for(int i = 0; i < board.moveHistory.size(); i++) {
                Move m = board.moveHistory.get(i);
                writer.write("    {\n");
                writer.write("      \"move_number\": " + (i + 1) + ",\n");
                writer.write("      \"piece\": \"" + m.pieceName + "\",\n");
                writer.write("      \"color\": \"" + (m.pieceIsWhite ? "white" : "black") + "\",\n");
                writer.write("      \"from\": \"" + (char)('a' + m.fromCol) + (8 - m.fromRow) + "\",\n");
                writer.write("      \"to\": \"" + (char)('a' + m.toCol) + (8 - m.toRow) + "\",\n");
                writer.write("      \"captured\": " + (m.capturedPieceName != null ? "\"" + m.capturedPieceName + "\"" : "null") + ",\n");
                writer.write("      \"timestamp\": " + m.timestamp + "\n");
                writer.write("    }" + (i < board.moveHistory.size() - 1 ? "," : "") + "\n");
            }
            
            writer.write("  ]\n");
            writer.write("}\n");
            
            writer.close();
            
        } catch (IOException e) {
            System.err.println("Error recording move: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Finalize game when it ends
     */
    public boolean finalizeGame(Board board, String userComment) {
        if (currentGameFile == null) {
            // If no current game, create one
            startNewGame();
            recordMove(board);
        }
        
        try {
            // Update with final result
            FileWriter writer = new FileWriter(currentGameFile);
            
            writer.write("{\n");
            writer.write("  \"timestamp\": \"" + currentTimestamp + "\",\n");
            writer.write("  \"result\": \"" + board.gameState + "\",\n");
            writer.write("  \"total_moves\": " + board.totalMoves + ",\n");
            writer.write("  \"user_comment\": \"" + escapeJson(userComment) + "\",\n");
            writer.write("  \"moves\": [\n");
            
            for(int i = 0; i < board.moveHistory.size(); i++) {
                Move m = board.moveHistory.get(i);
                writer.write("    {\n");
                writer.write("      \"move_number\": " + (i + 1) + ",\n");
                writer.write("      \"piece\": \"" + m.pieceName + "\",\n");
                writer.write("      \"color\": \"" + (m.pieceIsWhite ? "white" : "black") + "\",\n");
                writer.write("      \"from\": \"" + (char)('a' + m.fromCol) + (8 - m.fromRow) + "\",\n");
                writer.write("      \"to\": \"" + (char)('a' + m.toCol) + (8 - m.toRow) + "\",\n");
                writer.write("      \"captured\": " + (m.capturedPieceName != null ? "\"" + m.capturedPieceName + "\"" : "null") + ",\n");
                writer.write("      \"timestamp\": " + m.timestamp + "\n");
                writer.write("    }" + (i < board.moveHistory.size() - 1 ? "," : "") + "\n");
            }
            
            writer.write("  ]\n");
            writer.write("}\n");
            
            writer.close();
            
            // Also save PGN format
            savePGN(board, userComment);
            
            System.out.println("Game finalized successfully: " + currentGameFile);
            
            // Reset for next game
            currentGameFile = null;
            currentTimestamp = null;
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error finalizing game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void savePGN(Board board, String userComment) {
        try {
            String filename = REVIEW_DIR + "/game_" + currentTimestamp + ".pgn";
            FileWriter writer = new FileWriter(filename);
            
            // Write PGN format header
            writer.write("[Event \"Chess Game\"]\n");
            writer.write("[Site \"Local\"]\n");
            writer.write("[Date \"" + new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "\"]\n");
            writer.write("[Round \"-\"]\n");
            writer.write("[White \"Player 1\"]\n");
            writer.write("[Black \"Player 2\"]\n");
            
            String result = "1-0";
            switch(board.gameState) {
                case WHITE_WON: 
                    result = "1-0"; 
                    break;
                case BLACK_WON: 
                    result = "0-1"; 
                    break;
                case STALEMATE: 
                    result = "1/2-1/2"; 
                    break;
                default:
                    result = "*";
                    break;
            }
            writer.write("[Result \"" + result + "\"]\n");
            writer.write("[Comment \"" + escapeJson(userComment) + "\"]\n");
            writer.write("\n");
            
            // Write moves
            StringBuilder moves = new StringBuilder();
            for(int i = 0; i < board.moveHistory.size(); i++) {
                if(i % 2 == 0) {
                    moves.append((i/2 + 1)).append(". ");
                }
                moves.append(board.moveHistory.get(i).toNotation()).append(" ");
                
                // New line every 2 moves for readability
                if(i % 2 == 1 && i < board.moveHistory.size() - 1) {
                    moves.append("\n");
                }
            }
            moves.append(result);
            writer.write(moves.toString());
            writer.write("\n");
            
            writer.close();
            System.out.println("PGN file saved: " + filename);
            
        } catch (IOException e) {
            System.err.println("Error saving PGN: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String text) {
        if(text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Legacy method for compatibility
     */
    public boolean saveReview(Board board, String userComment) {
        if (currentGameFile != null) {
            return finalizeGame(board, userComment);
        } else {
            // If no live session, create one-time save
            startNewGame();
            recordMove(board);
            return finalizeGame(board, userComment);
        }
    }
}