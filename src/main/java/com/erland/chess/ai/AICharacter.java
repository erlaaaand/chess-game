package com.erland.chess.ai;

import java.io.Serializable;
import java.util.*;

/**
 * Representasi karakter AI yang dapat belajar
 */
public class AICharacter implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int eloRating;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int gamesDraw;
    
    // Personality traits (0.0 - 1.0)
    private double aggression;      // Seberapa agresif (suka menyerang)
    private double defensiveness;   // Seberapa defensif
    private double riskTaking;      // Seberapa berani ambil resiko
    private double patience;        // Seberapa sabar (endgame skill)
    private double tactical;        // Kemampuan taktik
    private double positional;      // Pemahaman posisi
    
    // Learning data
    private Map<String, Double> openingPreferences = new HashMap<>();
    private Map<String, Integer> movePatterns = new HashMap<>();
    private List<String> favoriteOpenings = new ArrayList<>();
    
    // Performance metrics
    private double averageAccuracy = 0.0;
    private int brilliantMoves = 0;
    private int blunders = 0;
    
    public AICharacter(String name) {
        this(name, 1000); // Default ELO
    }
    
    public AICharacter(String name, int initialElo) {
        this.name = name;
        this.eloRating = initialElo;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.gamesDraw = 0;
        
        // Random personality
        Random rand = new Random();
        this.aggression = 0.3 + rand.nextDouble() * 0.4;
        this.defensiveness = 0.3 + rand.nextDouble() * 0.4;
        this.riskTaking = 0.2 + rand.nextDouble() * 0.6;
        this.patience = 0.3 + rand.nextDouble() * 0.4;
        this.tactical = 0.3 + rand.nextDouble() * 0.4;
        this.positional = 0.3 + rand.nextDouble() * 0.4;
    }
    
    /**
     * Update ELO rating based on game result
     */
    public void updateElo(int opponentElo, GameResult result) {
        int K = 32; // K-factor for ELO calculation
        
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentElo - eloRating) / 400.0));
        
        double actualScore = 0.0;
        switch (result) {
            case WIN:
                actualScore = 1.0;
                gamesWon++;
                break;
            case DRAW:
                actualScore = 0.5;
                gamesDraw++;
                break;
            case LOSS:
                actualScore = 0.0;
                gamesLost++;
                break;
        }
        
        gamesPlayed++;
        int eloDelta = (int) (K * (actualScore - expectedScore));
        eloRating += eloDelta;
        
        System.out.println(name + " ELO: " + eloRating + " (" + (eloDelta >= 0 ? "+" : "") + eloDelta + ")");
    }
    
    /**
     * Learn from a move
     */
    public void learnMove(String moveUCI, double evaluation, String quality) {
        // Update move patterns
        movePatterns.put(moveUCI, movePatterns.getOrDefault(moveUCI, 0) + 1);
        
        // Update personality based on move quality
        if ("brilliant".equals(quality)) {
            brilliantMoves++;
            // Reinforce the traits that led to this move
            if (evaluation > 0) {
                aggression = Math.min(1.0, aggression + 0.01);
                tactical = Math.min(1.0, tactical + 0.01);
            }
        } else if ("blunder".equals(quality)) {
            blunders++;
            // Adjust traits to avoid similar mistakes
            riskTaking = Math.max(0.0, riskTaking - 0.01);
            patience = Math.min(1.0, patience + 0.01);
        }
    }
    
    /**
     * Learn from opening
     */
    public void learnOpening(String opening, boolean successful) {
        double current = openingPreferences.getOrDefault(opening, 0.5);
        if (successful) {
            openingPreferences.put(opening, Math.min(1.0, current + 0.05));
        } else {
            openingPreferences.put(opening, Math.max(0.0, current - 0.03));
        }
        
        // Update favorite openings
        updateFavoriteOpenings();
    }
    
    private void updateFavoriteOpenings() {
        favoriteOpenings.clear();
        openingPreferences.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> favoriteOpenings.add(e.getKey()));
    }
    
    /**
     * Get playing style description
     */
    public String getPlayingStyle() {
        if (aggression > 0.6) return "Aggressive";
        if (defensiveness > 0.6) return "Defensive";
        if (tactical > 0.6) return "Tactical";
        if (positional > 0.6) return "Positional";
        if (patience > 0.6) return "Patient";
        return "Balanced";
    }
    
    /**
     * Get strength level based on ELO
     */
    public String getStrengthLevel() {
        if (eloRating >= 2400) return "Grandmaster";
        if (eloRating >= 2200) return "International Master";
        if (eloRating >= 2000) return "Expert";
        if (eloRating >= 1800) return "Advanced";
        if (eloRating >= 1600) return "Intermediate";
        if (eloRating >= 1400) return "Developing";
        if (eloRating >= 1200) return "Beginner";
        return "Novice";
    }
    
    /**
     * Get win rate
     */
    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return (double) gamesWon / gamesPlayed * 100;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getEloRating() { return eloRating; }
    public void setEloRating(int eloRating) { this.eloRating = eloRating; }
    
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public int getGamesLost() { return gamesLost; }
    public int getGamesDraw() { return gamesDraw; }
    
    public double getAggression() { return aggression; }
    public double getDefensiveness() { return defensiveness; }
    public double getRiskTaking() { return riskTaking; }
    public double getPatience() { return patience; }
    public double getTactical() { return tactical; }
    public double getPositional() { return positional; }
    
    public double getAverageAccuracy() { return averageAccuracy; }
    public void setAverageAccuracy(double accuracy) { this.averageAccuracy = accuracy; }
    
    public int getBrilliantMoves() { return brilliantMoves; }
    public int getBlunders() { return blunders; }
    
    public List<String> getFavoriteOpenings() { return new ArrayList<>(favoriteOpenings); }
    
    public enum GameResult {
        WIN, DRAW, LOSS
    }
    
    @Override
    public String toString() {
        return String.format("%s (ELO: %d, Style: %s, Level: %s)", 
            name, eloRating, getPlayingStyle(), getStrengthLevel());
    }
}