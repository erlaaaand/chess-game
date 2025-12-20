package com.erland.chess.ai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.erland.chess.Constants;
import com.erland.chess.model.Board;
import com.erland.chess.model.Move;
import com.erland.chess.utils.NotationConverter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Bridge to communicate with Python AI engine
 * FIXED: Improved error handling, dynamic path detection, proper resource cleanup
 */
public class PythonBridge {
    private static PythonBridge instance;
    private final Gson gson = new Gson();
    private Process pythonProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private BufferedReader errorReader;
    private volatile boolean isRunning = false;
    private final Object lock = new Object();
    
    private PythonBridge() {
        startPythonEngine();
    }
    
    public static synchronized PythonBridge getInstance() {
        if (instance == null) {
            instance = new PythonBridge();
        }
        return instance;
    }
    
    /**
     * FIXED: Dynamic Python path detection and better error handling
     */
    private void startPythonEngine() {
        try {
            // Find Python executable
            String pythonCmd = findPythonExecutable();
            if (pythonCmd == null) {
                System.err.println("Python executable not found!");
                return;
            }
            
            // Find Python script
            File pythonScript = findPythonScript();
            if (pythonScript == null || !pythonScript.exists()) {
                System.err.println("Python AI engine script not found!");
                return;
            }
            
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, pythonScript.getAbsolutePath());
            pb.redirectErrorStream(false);
            
            pythonProcess = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));
            
            // Start error listener thread
            startErrorListener();
            
            isRunning = true;
            System.out.println("✓ Python AI engine started successfully");
            
        } catch (IOException e) {
            System.err.println("Failed to start Python engine: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * FIXED: Platform-independent Python detection
     */
    private String findPythonExecutable() {
        String[] candidates = {
            "python",
            "python3",
            "py",
            System.getProperty("user.dir") + "/ai_engine/venv/Scripts/python.exe",
            System.getProperty("user.dir") + "/ai_engine/venv/bin/python"
        };
        
        for (String cmd : candidates) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                if (p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {}
        }
        
        return null;
    }
    
    /**
     * FIXED: Better script path detection
     */
    private File findPythonScript() {
        String[] paths = {
            Constants.PYTHON_ENGINE_PATH,
            "ai_engine/ai_engine.py",
            "../ai_engine/ai_engine.py"
        };
        
        for (String path : paths) {
            File script = new File(path);
            if (script.exists()) {
                return script;
            }
        }
        
        return null;
    }
    
    /**
     * FIXED: Error stream monitoring
     */
    private void startErrorListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println("Python AI Error: " + line);
                }
            } catch (IOException e) {
                // Stream closed
            }
        }, "Python-Error-Listener").start();
    }
    
    /**
     * FIXED: Better synchronization and timeout handling
     */
    public CompletableFuture<String> getBestMove(Board board, AICharacter character, int timeLimit) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                if (!isRunning || pythonProcess == null || !pythonProcess.isAlive()) {
                    System.err.println("Python engine not running, attempting restart...");
                    startPythonEngine();
                    
                    if (!isRunning) {
                        return null;
                    }
                }
                
                try {
                    JsonObject request = new JsonObject();
                    request.addProperty("command", "get_move");
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
                    
                    String response = readWithTimeout(timeLimit + 1000);
                    if (response != null) {
                        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                        return jsonResponse.get("move").getAsString();
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error communicating with Python engine: " + e.getMessage());
                }
                
                return null;
            }
        });
    }
    
    /**
     * FIXED: Improved evaluation with error handling
     */
    public CompletableFuture<MoveEvaluation> evaluateMove(Board board, Move move) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                if (!isRunning) return null;
                
                try {
                    JsonObject request = new JsonObject();
                    request.addProperty("command", "evaluate_move");
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
            }
        });
    }
    
    /**
     * FIXED: Non-blocking training
     */
    public void trainCharacter(AICharacter character, List<Move> moves, String result) {
        if (!isRunning) return;
        
        CompletableFuture.runAsync(() -> {
            synchronized (lock) {
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
            }
        });
    }

    /**
     * FIXED: Proper timeout with thread interruption
     */
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
    
    /**
     * FIXED: Proper cleanup
     */
    public void shutdown() {
        synchronized (lock) {
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
                if (errorReader != null) errorReader.close();
                
                if (pythonProcess != null) {
                    pythonProcess.destroy();
                    pythonProcess.waitFor(5, TimeUnit.SECONDS);
                    
                    if (pythonProcess.isAlive()) {
                        pythonProcess.destroyForcibly();
                    }
                }
                
                System.out.println("✓ Python engine shutdown complete");
                
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }
    }
    
    public static class MoveEvaluation {
        public double score;
        public String quality;
        public String comment;
    }
}