package com.erland.chess.ai;

import com.erland.chess.config.GameConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages AI characters - save, load, create, delete
 */
public class AICharacterManager {
    private static AICharacterManager instance;
    private final Gson gson;
    private final Path charactersDir;
    private Map<String, AICharacter> characters = new HashMap<>();
    
    private AICharacterManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.charactersDir = Paths.get(GameConfig.AI_CHARACTERS_DIR);
        
        try {
            Files.createDirectories(charactersDir);
        } catch (IOException e) {
            System.err.println("Failed to create AI characters directory: " + e.getMessage());
        }
        
        loadAllCharacters();
        createDefaultCharacters();
    }
    
    public static AICharacterManager getInstance() {
        if (instance == null) {
            instance = new AICharacterManager();
        }
        return instance;
    }
    
    /**
     * Create default AI characters if none exist
     */
    private void createDefaultCharacters() {
        if (characters.isEmpty()) {
            createCharacter("Rookie", 800);
            createCharacter("Student", 1200);
            createCharacter("Amateur", 1600);
            createCharacter("Professional", 2000);
            createCharacter("Master", 2400);
            
            System.out.println("Created default AI characters");
        }
    }
    
    /**
     * Create new AI character
     */
    public AICharacter createCharacter(String name, int initialElo) {
        if (characters.containsKey(name)) {
            throw new IllegalArgumentException("Character already exists: " + name);
        }
        
        AICharacter character = new AICharacter(name, initialElo);
        characters.put(name, character);
        saveCharacter(character);
        
        return character;
    }
    
    /**
     * Get character by name
     */
    public AICharacter getCharacter(String name) {
        return characters.get(name);
    }
    
    /**
     * Get all characters
     */
    public List<AICharacter> getAllCharacters() {
        return new ArrayList<>(characters.values());
    }
    
    /**
     * Get characters sorted by ELO
     */
    public List<AICharacter> getCharactersByElo() {
        return characters.values().stream()
            .sorted(Comparator.comparingInt(AICharacter::getEloRating).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Update character after game
     */
    public void updateCharacter(AICharacter character) {
        characters.put(character.getName(), character);
        saveCharacter(character);
    }
    
    /**
     * Delete character
     */
    public boolean deleteCharacter(String name) {
        AICharacter removed = characters.remove(name);
        if (removed != null) {
            try {
                Path file = charactersDir.resolve(name + ".json");
                Files.deleteIfExists(file);
                return true;
            } catch (IOException e) {
                System.err.println("Failed to delete character file: " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Save character to file
     */
    private void saveCharacter(AICharacter character) {
        try {
            Path file = charactersDir.resolve(character.getName() + ".json");
            String json = gson.toJson(character);
            Files.writeString(file, json);
        } catch (IOException e) {
            System.err.println("Failed to save character: " + e.getMessage());
        }
    }
    
    /**
     * Load all characters from files
     */
    private void loadAllCharacters() {
        try {
            if (!Files.exists(charactersDir)) {
                return;
            }
            
            Files.list(charactersDir)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadCharacterFromFile);
            
            System.out.println("Loaded " + characters.size() + " AI characters");
        } catch (IOException e) {
            System.err.println("Failed to load characters: " + e.getMessage());
        }
    }
    
    /**
     * Load single character from file
     */
    private void loadCharacterFromFile(Path file) {
        try {
            String json = Files.readString(file);
            AICharacter character = gson.fromJson(json, AICharacter.class);
            characters.put(character.getName(), character);
        } catch (IOException e) {
            System.err.println("Failed to load character from " + file + ": " + e.getMessage());
        }
    }
    
    /**
     * Get character suitable for player ELO
     */
    public AICharacter getMatchingCharacter(int playerElo, int tolerance) {
        return characters.values().stream()
            .filter(c -> Math.abs(c.getEloRating() - playerElo) <= tolerance)
            .min(Comparator.comparingInt(c -> Math.abs(c.getEloRating() - playerElo)))
            .orElse(characters.values().iterator().next()); // Return any if no match
    }
    
    /**
     * Get leaderboard
     */
    public String getLeaderboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Characters Leaderboard ===\n\n");
        
        List<AICharacter> sorted = getCharactersByElo();
        for (int i = 0; i < sorted.size(); i++) {
            AICharacter c = sorted.get(i);
            sb.append(String.format("%2d. %-15s ELO: %4d  Level: %-15s  W/L/D: %d/%d/%d  (%.1f%%)\n",
                i + 1, c.getName(), c.getEloRating(), c.getStrengthLevel(),
                c.getGamesWon(), c.getGamesLost(), c.getGamesDraw(), c.getWinRate()));
        }
        
        return sb.toString();
    }
    
    /**
     * Export character stats
     */
    public void exportStats(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("AI Character Statistics Export");
            writer.println("Generated: " + new Date());
            writer.println();
            writer.println(getLeaderboard());
            
            writer.println("\nDetailed Statistics:");
            for (AICharacter c : getCharactersByElo()) {
                writer.println("\n--- " + c.getName() + " ---");
                writer.println("ELO: " + c.getEloRating());
                writer.println("Level: " + c.getStrengthLevel());
                writer.println("Playing Style: " + c.getPlayingStyle());
                writer.println("Games: " + c.getGamesPlayed());
                writer.println("Win Rate: " + String.format("%.1f%%", c.getWinRate()));
                writer.println("Brilliant Moves: " + c.getBrilliantMoves());
                writer.println("Blunders: " + c.getBlunders());
                writer.println("Average Accuracy: " + String.format("%.1f%%", c.getAverageAccuracy()));
                
                if (!c.getFavoriteOpenings().isEmpty()) {
                    writer.println("Favorite Openings: " + String.join(", ", c.getFavoriteOpenings()));
                }
            }
            
            System.out.println("Stats exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to export stats: " + e.getMessage());
        }
    }
}