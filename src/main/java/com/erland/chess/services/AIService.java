package com.erland.chess.service;

import com.erland.chess.ai.AICharacter;
import com.erland.chess.ai.AICharacterManager;
import com.erland.chess.ai.AIPlayer;
import com.erland.chess.core.GameEngine;
import com.erland.chess.model.Move;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for AI operations
 * Handles async AI move calculation
 */
public class AIService {
    private final ExecutorService executor;
    private final AICharacterManager characterManager;
    
    public AIService() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AI-Worker");
            t.setDaemon(true);
            return t;
        });
        this.characterManager = AICharacterManager.getInstance();
    }
    
    /**
     * Get AI move asynchronously
     */
    public CompletableFuture<Move> getAIMove(GameEngine engine, AICharacter character) {
        return CompletableFuture.supplyAsync(() -> {
            AIPlayer aiPlayer = new AIPlayer(character);
            return aiPlayer.getMove(engine.getBoard()).join();
        }, executor);
    }
    
    /**
     * Train AI character after game
     */
    public void trainCharacter(AICharacter character, GameResult result) {
        executor.submit(() -> {
            // Training logic
            character.updateElo(result.getOpponentElo(), result.getGameResult());
            characterManager.updateCharacter(character);
        });
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}