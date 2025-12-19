#!/usr/bin/env python3
"""
AI Character Module - Defines AI personalities and learning capabilities
Path: ai_engine/ai_character.py
"""

import json
import pickle
from pathlib import Path
from typing import Dict, List, Optional
from dataclasses import dataclass, field


@dataclass
class PersonalityTraits:
    """AI personality characteristics"""
    aggression: float = 0.5      # 0.0 - 1.0
    defensiveness: float = 0.5
    risk_taking: float = 0.5
    patience: float = 0.5
    tactical: float = 0.5
    positional: float = 0.5
    
    def to_dict(self) -> Dict:
        return {
            'aggression': self.aggression,
            'defensiveness': self.defensiveness,
            'risk_taking': self.risk_taking,
            'patience': self.patience,
            'tactical': self.tactical,
            'positional': self.positional
        }


@dataclass
class PerformanceMetrics:
    """Track AI performance statistics"""
    games_played: int = 0
    games_won: int = 0
    games_lost: int = 0
    games_drawn: int = 0
    brilliant_moves: int = 0
    good_moves: int = 0
    mistakes: int = 0
    blunders: int = 0
    average_accuracy: float = 0.0
    
    @property
    def win_rate(self) -> float:
        if self.games_played == 0:
            return 0.0
        return (self.games_won / self.games_played) * 100


class AICharacter:
    """Represents an AI character with learning capabilities"""
    
    def __init__(self, name: str, elo_rating: int = 1000):
        self.name = name
        self.elo_rating = elo_rating
        self.personality = PersonalityTraits()
        self.metrics = PerformanceMetrics()
        
        # Learning data
        self.opening_preferences: Dict[str, float] = {}
        self.move_patterns: Dict[str, int] = {}
        self.favorite_openings: List[str] = []
        
    def update_elo(self, opponent_elo: int, result: str):
        """Update ELO rating based on game result"""
        K = 32  # K-factor
        
        expected = 1.0 / (1.0 + 10 ** ((opponent_elo - self.elo_rating) / 400.0))
        
        actual = {'win': 1.0, 'draw': 0.5, 'loss': 0.0}.get(result.lower(), 0.5)
        
        delta = K * (actual - expected)
        self.elo_rating += int(delta)
        
        # Update metrics
        self.metrics.games_played += 1
        if result.lower() == 'win':
            self.metrics.games_won += 1
        elif result.lower() == 'loss':
            self.metrics.games_lost += 1
        else:
            self.metrics.games_drawn += 1
    
    def learn_from_move(self, move_uci: str, evaluation: float, quality: str):
        """Learn from a move based on its quality"""
        self.move_patterns[move_uci] = self.move_patterns.get(move_uci, 0) + 1
        
        # Update metrics
        if quality == 'brilliant':
            self.metrics.brilliant_moves += 1
            self.personality.tactical = min(1.0, self.personality.tactical + 0.01)
        elif quality == 'good':
            self.metrics.good_moves += 1
        elif quality == 'mistake':
            self.metrics.mistakes += 1
            self.personality.patience = min(1.0, self.personality.patience + 0.01)
        elif quality == 'blunder':
            self.metrics.blunders += 1
            self.personality.risk_taking = max(0.0, self.personality.risk_taking - 0.02)
    
    def learn_opening(self, opening_name: str, success: bool):
        """Learn from opening performance"""
        current = self.opening_preferences.get(opening_name, 0.5)
        
        if success:
            self.opening_preferences[opening_name] = min(1.0, current + 0.05)
        else:
            self.opening_preferences[opening_name] = max(0.0, current - 0.03)
        
        self._update_favorite_openings()
    
    def _update_favorite_openings(self):
        """Update list of favorite openings"""
        sorted_openings = sorted(
            self.opening_preferences.items(),
            key=lambda x: x[1],
            reverse=True
        )
        self.favorite_openings = [name for name, _ in sorted_openings[:5]]
    
    def get_playing_style(self) -> str:
        """Get description of playing style"""
        traits = self.personality
        
        if traits.aggression > 0.7:
            return "Aggressive"
        elif traits.defensiveness > 0.7:
            return "Defensive"
        elif traits.tactical > 0.7:
            return "Tactical"
        elif traits.positional > 0.7:
            return "Positional"
        elif traits.patience > 0.7:
            return "Patient"
        else:
            return "Balanced"
    
    def get_strength_level(self) -> str:
        """Get strength level based on ELO"""
        if self.elo_rating >= 2400:
            return "Grandmaster"
        elif self.elo_rating >= 2200:
            return "International Master"
        elif self.elo_rating >= 2000:
            return "Expert"
        elif self.elo_rating >= 1800:
            return "Advanced"
        elif self.elo_rating >= 1600:
            return "Intermediate"
        elif self.elo_rating >= 1400:
            return "Developing"
        elif self.elo_rating >= 1200:
            return "Beginner"
        else:
            return "Novice"
    
    def save(self, directory: Path):
        """Save character to file"""
        directory.mkdir(parents=True, exist_ok=True)
        filepath = directory / f"{self.name}.pkl"
        
        with open(filepath, 'wb') as f:
            pickle.dump(self, f)
    
    @classmethod
    def load(cls, filepath: Path) -> 'AICharacter':
        """Load character from file"""
        with open(filepath, 'rb') as f:
            return pickle.load(f)
    
    def to_dict(self) -> Dict:
        """Convert to dictionary for JSON serialization"""
        return {
            'name': self.name,
            'elo_rating': self.elo_rating,
            'personality': self.personality.to_dict(),
            'metrics': {
                'games_played': self.metrics.games_played,
                'games_won': self.metrics.games_won,
                'games_lost': self.metrics.games_lost,
                'games_drawn': self.metrics.games_drawn,
                'win_rate': self.metrics.win_rate,
                'brilliant_moves': self.metrics.brilliant_moves,
                'mistakes': self.metrics.mistakes,
                'blunders': self.metrics.blunders
            },
            'playing_style': self.get_playing_style(),
            'strength_level': self.get_strength_level(),
            'favorite_openings': self.favorite_openings
        }
    
    def __str__(self) -> str:
        return (f"{self.name} (ELO: {self.elo_rating}, "
                f"Style: {self.get_playing_style()}, "
                f"Level: {self.get_strength_level()})")