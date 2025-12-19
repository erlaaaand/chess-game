package com.erland.chess.ai;

import com.erland.chess.config.GameConfig;
import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.utils.NotationConverter; // Import Converter
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
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
    
    public CompletableFuture<String> getBestMove(Board board, AICharacter character, int timeLimit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning || pythonProcess == null || !pythonProcess.isAlive()) {
                System.err.println("Python engine not running, restarting...");
                startPythonEngine();
            }
            
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "get_move");
                // REFACTORED: Gunakan NotationConverter
                request.addProperty("fen", NotationConverter.toFEN(board));
                request.addProperty("time_limit", timeLimit);
                
                JsonObject personality = new JsonObject();
                personality.addProperty("aggression", character.getAggression());
                personality.addProperty("defensiveness", character.getDefensiveness());
                personality.addProperty("risk_taking", character.getRiskTaking());
                personality.addProperty("patience", character.getPatience());
                personality.addProperty("tactical", character.getTactical());
                personality.addProperty("positional", character.getPositional());
                request.add("personality", personality);
                
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
                
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
    
    public CompletableFuture<MoveEvaluation> evaluateMove(Board board, Move move) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) return null;
            
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "evaluate_move");
                // REFACTORED: Gunakan NotationConverter
                request.addProperty("fen", NotationConverter.toFEN(board));
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
    
    // ... (sisa method seperti trainCharacter, readWithTimeout, shutdown tetap sama)
    
    public void trainCharacter(AICharacter character, List<Move> moves, String result) {
        CompletableFuture.runAsync(() -> {
            if (!isRunning) return;
            try {
                JsonObject request = new JsonObject();
                request.addProperty("command", "train");
                request.addProperty("character_name", character.getName());
                request.addProperty("result", result);
                
                List<String> uciMoves = new ArrayList<>();
                for (Move move : moves) {
                    uciMoves.add(move.toUCI());
                }
                request.addProperty("moves", gson.toJson(uciMoves));
                
                writer.write(gson.toJson(request));
                writer.newLine();
                writer.flush();
            } catch (Exception e) {
                System.err.println("Error training character: " + e.getMessage());
            }
        });
    }

    private String readWithTimeout(long timeoutMs) throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> reader.readLine());
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            executor.shutdown();
        }
    }
    
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Method private boardToFEN() dan getPieceChar() SUDAH DIHAPUS karena digantikan NotationConverter
    
    public static class MoveEvaluation {
        public double score;
        public String quality;
        public String comment;
    }
}