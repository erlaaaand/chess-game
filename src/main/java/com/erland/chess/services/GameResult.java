package com.erland.chess.service;

import com.erland.chess.ai.AICharacter;

/**
 * DTO for game results
 */
public class GameResult {
    private final int opponentElo;
    private final AICharacter.GameResult gameResult;
    
    public GameResult(int opponentElo, AICharacter.GameResult gameResult) {
        this.opponentElo = opponentElo;
        this.gameResult = gameResult;
    }
    
    public int getOpponentElo() {
        return opponentElo;
    }
    
    public AICharacter.GameResult getGameResult() {
        return gameResult;
    }
}