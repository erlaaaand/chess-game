package com.erland.chess.review;

import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.utils.NotationConverter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameReviewer {
    
    private Board recordedBoard;
    
    public void startNewGame() {
        // Reset logic if needed
    }
    
    public void recordMove(Board board) {
        // Kita simpan referensi board terakhir untuk diambil move history-nya saat finish
        this.recordedBoard = board;
    }
    
    public boolean finalizeGame(Board board, String userComment) {
        this.recordedBoard = board;
        return saveGameToFile(userComment);
    }
    
    private boolean saveGameToFile(String comment) {
        if (recordedBoard == null || recordedBoard.moveHistory.isEmpty()) {
            return false;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        String filename = "chess_game_" + timestamp + ".pgn";
        
        File dir = new File("saved_games");
        if (!dir.exists()) dir.mkdir();
        
        try (FileWriter writer = new FileWriter(new File(dir, filename))) {
            // Write PGN Headers
            writer.write("[Event \"Casual Game\"]\n");
            writer.write("[Site \"Local Computer\"]\n");
            writer.write("[Date \"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"]\n");
            writer.write("[White \"Player 1\"]\n");
            writer.write("[Black \"Player 2\"]\n");
            writer.write("[Result \"*\"]\n"); // Bisa diupdate sesuai GameState
            writer.write("\n"); // Empty line separator
            
            // Write Moves
            StringBuilder pgnMoves = new StringBuilder();
            int turn = 1;
            for (int i = 0; i < recordedBoard.moveHistory.size(); i++) {
                Move move = recordedBoard.moveHistory.get(i);
                
                // Jika giliran putih (index genap: 0, 2, 4...)
                if (i % 2 == 0) {
                    pgnMoves.append(turn).append(". ");
                    turn++;
                }
                
                // Gunakan converter untuk notasi yang benar
                // Kita gunakan toAlgebraic sederhana atau buat logic PGN full di Converter
                // Untuk sekarang, kita pakai format sederhana "e2e4" atau UCI
                pgnMoves.append(NotationConverter.toUCI(move)).append(" ");
            }
            
            // Add comment at the end
            if (comment != null && !comment.isEmpty()) {
                pgnMoves.append("\n\n{ ").append(comment).append(" }");
            }
            
            writer.write(pgnMoves.toString());
            System.out.println("Game saved to " + filename);
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}