package com.erland.chess.ai;

import com.erland.chess.config.GameConfig;
import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Bridge to communicate with Python AI engine
 */
public class PythonBridge {
    private static PythonBridge instance;
    private final Gson gson = new Gson();
    private Process pythonProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private boolean isRunning = false;
    
    private PythonBridge() {
        startPythonEngine();
    }
    
    public static PythonBridge getInstance() {
        if (instance == null) {
            instance = new PythonBridge();
        }
        return instance;
    }
    
    /**
     * Start Python AI engine process
     */
    private void startPythonEngine() {
        try {
            File pythonScript = new File(GameConfig.PYTHON_ENGINE_PATH);
            if (!pythonScript.exists()) {
                System.err.println("Python AI engine not found: " + pythonScript.getAbsolutePath());
                return;
            }
            
            ProcessBuilder pb = new ProcessBuilder("python", pythonScript.getAbsolutePath());
            pb.redirectErrorStream(false);
            
            pythonProcess = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
            
            isRunning = true;
            System.out.println("Python AI engine started successfully");
            
        } catch (IOException e) {
            System.err.println("Failed to start Python engine: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get best move from Python engine
     */
    public CompletableFuture<String> getBestMove(Board board, AICharacter character, int timeLimit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning || pythonProcess == null || !pythonProcess.isAlive()) {
                System.err.println("Python engine not running, restarting...");
                startPythonEngine();
            }
            
            try {
                // Create request
                JsonObject request = new JsonObject();
                request.addProperty("command", "get_move");
                request.addProperty("fen", boardToFEN(board));
                request.addProperty("time_limit", timeLimit);
                
                // Add character personality
                JsonObject personality = new JsonObject();
                personality.addProperty("aggression", character.getAggression());
                personality.addProperty("defensiveness", character.getDefensiveness());
                personality.addProperty("risk_taking", character.getRiskTaking());
                personality.addProperty("patience", character.getPatience());
                personality.addProperty("tactical", character.getTactical());
                personality.addProperty("positional", character.getPositional());
                request.add("personality", personality);
                
                // Send request
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
                
                // Read response with timeout
                String response = readWithTimeout(5000);
                if (response != null) {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    return jsonResponse.get("move").getAsString();
                }
                
            } catch (Exception e) {
                System.err.println("Error communicating with Python engine: " + e.getMessage());
                e.printStackTrace();
            }
            
            return null;
        });
    }
    
    /**
     * Evaluate a move
     */
    public CompletableFuture<MoveEvaluation> evaluateMove(Board board, Move move) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) return null;
            
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "evaluate_move");
                request.addProperty("fen", boardToFEN(board));
                request.addProperty("move", move.toUCI());
                
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
                
                String response = readWithTimeout(3000);
                if (response != null) {
                    JsonObject json = gson.fromJson(response, JsonObject.class);
                    
                    MoveEvaluation eval = new MoveEvaluation();
                    eval.score = json.get("score").getAsDouble();
                    eval.quality = json.get("quality").getAsString();
                    eval.comment = json.has("comment") ? json.get("comment").getAsString() : "";
                    
                    return eval;
                }
                
            } catch (Exception e) {
                System.err.println("Error evaluating move: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * Train character with game data
     */
    public void trainCharacter(AICharacter character, List<Move> moves, String result) {
        CompletableFuture.runAsync(() -> {
            if (!isRunning) return;
            
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "train");
                request.addProperty("character_name", character.getName());
                request.addProperty("result", result);
                
                // Convert moves to UCI
                List<String> uciMoves = new ArrayList<>();
                for (Move move : moves) {
                    uciMoves.add(move.toUCI());
                }
                request.addProperty("moves", gson.toJson(uciMoves));
                
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
                
                System.out.println("Training data sent for " + character.getName());
                
            } catch (Exception e) {
                System.err.println("Error training character: " + e.getMessage());
            }
        });
    }
    
    /**
     * Convert board to FEN notation
     */
    private String boardToFEN(Board board) {
        StringBuilder fen = new StringBuilder();
        
        // Piece placement
        for (int row = 0; row < 8; row++) {
            int emptySquares = 0;
            for (int col = 0; col < 8; col++) {
                var piece = board.getPiece(col, row);
                if (piece == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    char pieceChar = getPieceChar(piece.name);
                    fen.append(piece.isWhite ? Character.toUpperCase(pieceChar) : pieceChar);
                }
            }
            if (emptySquares > 0) {
                fen.append(emptySquares);
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        
        // Active color
        fen.append(' ').append(board.isWhiteTurn ? 'w' : 'b');
        
        // Castling availability (simplified)
        fen.append(" KQkq");
        
        // En passant target square
        fen.append(" -");
        
        // Halfmove and fullmove counters
        fen.append(" 0 ").append(board.totalMoves / 2 + 1);
        
        return fen.toString();
    }
    
    private char getPieceChar(String pieceName) {
        switch (pieceName) {
            case "King": return 'k';
            case "Queen": return 'q';
            case "Rook": return 'r';
            case "Bishop": return 'b';
            case "Knight": return 'n';
            case "Pawn": return 'p';
            default: return '?';
        }
    }
    
    /**
     * Read from Python process with timeout
     */
    private String readWithTimeout(long timeoutMs) throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> reader.readLine());
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.println("Python engine timeout");
            return null;
        } catch (Exception e) {
            System.err.println("Error reading from Python: " + e.getMessage());
            return null;
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Shutdown Python engine
     */
    public void shutdown() {
        isRunning = false;
        try {
            if (writer != null) {
                JsonObject request = new JsonObject();
                request.addProperty("command", "quit");
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
                writer.close();
            }
            if (reader != null) reader.close();
            if (pythonProcess != null) pythonProcess.destroy();
            
            System.out.println("Python AI engine shutdown");
        } catch (IOException e) {
            System.err.println("Error shutting down Python engine: " + e.getMessage());
        }
    }
    
    /**
     * Move evaluation result
     */
    public static class MoveEvaluation {
        public double score;
        public String quality; // brilliant, good, normal, inaccuracy, mistake, blunder
        public String comment;
    }
}