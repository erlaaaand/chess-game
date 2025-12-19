#!/usr/bin/env python3
"""
Character Manager - Manages AI character lifecycle
Path: ai_engine/character_manager.py
"""

import json
from pathlib import Path
from typing import Dict, List, Optional
from ai_character import AICharacter


class CharacterManager:
    """Manages collection of AI characters"""
    
    def __init__(self, characters_dir: str = "ai_characters"):
        self.characters_dir = Path(characters_dir)
        self.characters_dir.mkdir(parents=True, exist_ok=True)
        self.characters: Dict[str, AICharacter] = {}
        
        self._load_all_characters()
        self._ensure_default_characters()
    
    def _load_all_characters(self):
        """Load all characters from disk"""
        if not self.characters_dir.exists():
            return
        
        for file in self.characters_dir.glob("*.pkl"):
            try:
                character = AICharacter.load(file)
                self.characters[character.name] = character
            except Exception as e:
                print(f"Error loading {file}: {e}")
    
    def _ensure_default_characters(self):
        """Create default characters if none exist"""
        if not self.characters:
            defaults = [
                ("Rookie", 800),
                ("Student", 1200),
                ("Amateur", 1600),
                ("Professional", 2000),
                ("Master", 2400)
            ]
            
            for name, elo in defaults:
                self.create_character(name, elo)
            
            print(f"Created {len(defaults)} default characters")
    
    def create_character(self, name: str, elo: int = 1000) -> AICharacter:
        """Create new AI character"""
        if name in self.characters:
            raise ValueError(f"Character '{name}' already exists")
        
        character = AICharacter(name, elo)
        self.characters[name] = character
        character.save(self.characters_dir)
        
        return character
    
    def get_character(self, name: str) -> Optional[AICharacter]:
        """Get character by name"""
        return self.characters.get(name)
    
    def get_all_characters(self) -> List[AICharacter]:
        """Get all characters"""
        return list(self.characters.values())
    
    def get_characters_by_elo(self) -> List[AICharacter]:
        """Get characters sorted by ELO rating"""
        return sorted(
            self.characters.values(),
            key=lambda c: c.elo_rating,
            reverse=True
        )
    
    def update_character(self, character: AICharacter):
        """Update character and save to disk"""
        self.characters[character.name] = character
        character.save(self.characters_dir)
    
    def delete_character(self, name: str) -> bool:
        """Delete character"""
        if name not in self.characters:
            return False
        
        character = self.characters.pop(name)
        
        # Delete file
        filepath = self.characters_dir / f"{name}.pkl"
        if filepath.exists():
            filepath.unlink()
        
        return True
    
    def get_matching_character(self, player_elo: int, tolerance: int = 200) -> Optional[AICharacter]:
        """Find character with similar ELO"""
        candidates = [
            c for c in self.characters.values()
            if abs(c.elo_rating - player_elo) <= tolerance
        ]
        
        if not candidates:
            # Return any character if no match
            return next(iter(self.characters.values()), None)
        
        # Return closest match
        return min(candidates, key=lambda c: abs(c.elo_rating - player_elo))
    
    def get_leaderboard(self) -> str:
        """Generate leaderboard text"""
        lines = ["=== AI Characters Leaderboard ===\n"]
        
        sorted_chars = self.get_characters_by_elo()
        
        for i, char in enumerate(sorted_chars, 1):
            metrics = char.metrics
            lines.append(
                f"{i:2d}. {char.name:15s} "
                f"ELO: {char.elo_rating:4d}  "
                f"Level: {char.get_strength_level():15s}  "
                f"W/L/D: {metrics.games_won}/{metrics.games_lost}/{metrics.games_drawn}  "
                f"({metrics.win_rate:.1f}%)"
            )
        
        return "\n".join(lines)
    
    def export_statistics(self, filename: str):
        """Export detailed statistics to file"""
        with open(filename, 'w') as f:
            f.write("AI Character Statistics Export\n")
            f.write("=" * 60 + "\n\n")
            
            f.write(self.get_leaderboard())
            f.write("\n\n" + "=" * 60 + "\n")
            f.write("Detailed Statistics\n")
            f.write("=" * 60 + "\n\n")
            
            for char in self.get_characters_by_elo():
                f.write(f"\n--- {char.name} ---\n")
                f.write(f"ELO: {char.elo_rating}\n")
                f.write(f"Level: {char.get_strength_level()}\n")
                f.write(f"Playing Style: {char.get_playing_style()}\n")
                f.write(f"Games Played: {char.metrics.games_played}\n")
                f.write(f"Win Rate: {char.metrics.win_rate:.1f}%\n")
                f.write(f"Brilliant Moves: {char.metrics.brilliant_moves}\n")
                f.write(f"Blunders: {char.metrics.blunders}\n")
                
                if char.favorite_openings:
                    f.write(f"Favorite Openings: {', '.join(char.favorite_openings)}\n")
        
        print(f"Statistics exported to {filename}")
    
    def export_json(self, filename: str):
        """Export all characters to JSON"""
        data = {
            name: char.to_dict()
            for name, char in self.characters.items()
        }
        
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)