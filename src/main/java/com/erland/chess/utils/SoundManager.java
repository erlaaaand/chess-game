// Path: src/main/java/com/erland/chess/utils/SoundManager.java
package com.erland.chess.utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Manages game sound effects and music
 */
public class SoundManager {
    private static SoundManager instance;
    
    private final Map<SoundType, AudioClip> sounds;
    private MediaPlayer musicPlayer;
    private boolean soundEnabled;
    private boolean musicEnabled;
    private double volume;
    
    /**
     * Sound types available in the game
     */
    public enum SoundType {
        MOVE("move.wav"),
        CAPTURE("capture.wav"),
        CHECK("check.wav"),
        CHECKMATE("checkmate.wav"),
        CASTLE("castle.wav"),
        PROMOTION("promotion.wav"),
        GAME_START("game_start.wav"),
        GAME_END("game_end.wav"),
        BUTTON_CLICK("button.wav"),
        ERROR("error.wav");
        
        private final String filename;
        
        SoundType(String filename) {
            this.filename = filename;
        }
        
        public String getFilename() {
            return filename;
        }
    }
    
    private SoundManager() {
        this.sounds = new HashMap<>();
        this.soundEnabled = true;
        this.musicEnabled = true;
        this.volume = 0.5;
        
        loadSounds();
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    /**
     * Load all sound effects
     */
    private void loadSounds() {
        for (SoundType type : SoundType.values()) {
            try {
                URL soundURL = getClass().getResource("/sounds/" + type.getFilename());
                
                if (soundURL != null) {
                    AudioClip clip = new AudioClip(soundURL.toString());
                    clip.setVolume(volume);
                    sounds.put(type, clip);
                } else {
                    System.out.println("Sound not found: " + type.getFilename());
                }
            } catch (Exception e) {
                System.err.println("Failed to load sound: " + type.getFilename());
            }
        }
        
        System.out.println("Loaded " + sounds.size() + " sound effects");
    }
    
    /**
     * Play a sound effect
     */
    public void playSound(SoundType type) {
        if (!soundEnabled) {
            return;
        }
        
        AudioClip clip = sounds.get(type);
        if (clip != null) {
            try {
                clip.play();
            } catch (Exception e) {
                System.err.println("Error playing sound: " + type);
            }
        }
    }
    
    /**
     * Play sound with custom volume
     */
    public void playSound(SoundType type, double customVolume) {
        if (!soundEnabled) {
            return;
        }
        
        AudioClip clip = sounds.get(type);
        if (clip != null) {
            try {
                double originalVolume = clip.getVolume();
                clip.setVolume(customVolume);
                clip.play();
                
                // Reset volume after playing
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        clip.setVolume(originalVolume);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } catch (Exception e) {
                System.err.println("Error playing sound: " + type);
            }
        }
    }
    
    /**
     * Load and play background music
     */
    public void playBackgroundMusic(String musicFile) {
        if (!musicEnabled) {
            return;
        }
        
        try {
            stopBackgroundMusic();
            
            URL musicURL = getClass().getResource("/music/" + musicFile);
            if (musicURL != null) {
                Media media = new Media(musicURL.toString());
                musicPlayer = new MediaPlayer(media);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setVolume(volume * 0.6); // Lower volume for background music
                musicPlayer.play();
            }
        } catch (Exception e) {
            System.err.println("Failed to play background music: " + e.getMessage());
        }
    }
    
    /**
     * Stop background music
     */
    public void stopBackgroundMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
    }
    
    /**
     * Pause background music
     */
    public void pauseBackgroundMusic() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
    }
    
    /**
     * Resume background music
     */
    public void resumeBackgroundMusic() {
        if (musicPlayer != null && musicEnabled) {
            musicPlayer.play();
        }
    }
    
    /**
     * Set master volume (0.0 to 1.0)
     */
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        
        // Update all loaded sounds
        for (AudioClip clip : sounds.values()) {
            clip.setVolume(this.volume);
        }
        
        // Update music player
        if (musicPlayer != null) {
            musicPlayer.setVolume(this.volume * 0.6);
        }
    }
    
    /**
     * Get current volume
     */
    public double getVolume() {
        return volume;
    }
    
    /**
     * Enable/disable sound effects
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    /**
     * Check if sound is enabled
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Enable/disable background music
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        
        if (!enabled) {
            stopBackgroundMusic();
        }
    }
    
    /**
     * Check if music is enabled
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    /**
     * Play appropriate sound for a chess move
     */
    public void playMoveSound(boolean isCapture, boolean isCheck, 
                              boolean isCheckmate, boolean isCastle) {
        if (isCheckmate) {
            playSound(SoundType.CHECKMATE);
        } else if (isCheck) {
            playSound(SoundType.CHECK);
        } else if (isCastle) {
            playSound(SoundType.CASTLE);
        } else if (isCapture) {
            playSound(SoundType.CAPTURE);
        } else {
            playSound(SoundType.MOVE);
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopBackgroundMusic();
        sounds.clear();
    }
    
    /**
     * Preload all sounds (useful for performance)
     */
    public void preloadAll() {
        for (SoundType type : SoundType.values()) {
            AudioClip clip = sounds.get(type);
            if (clip != null) {
                // Playing at volume 0 to preload
                double originalVolume = clip.getVolume();
                clip.setVolume(0);
                clip.play();
                clip.setVolume(originalVolume);
            }
        }
    }
}