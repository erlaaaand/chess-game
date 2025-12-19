#!/usr/bin/env python3
"""
Advanced Chess Game Analyzer with Machine Learning
Requires: numpy, pandas, matplotlib (optional)
Install: pip install numpy pandas matplotlib
"""

import json
import glob
import os
from collections import defaultdict, Counter

try:
    import numpy as np
    HAS_NUMPY = True
except ImportError:
    HAS_NUMPY = False
    print("Warning: numpy not installed. Some features disabled.")

try:
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("Warning: matplotlib not installed. Visualization disabled.")

class AdvancedChessAnalyzer:
    def __init__(self, bridge_dir="python_bridge"):
        self.bridge_dir = bridge_dir
        self.games = []
        self.patterns = defaultdict(int)
    
    def load_all_games(self):
        """Load all game data"""
        json_files = glob.glob(os.path.join(self.bridge_dir, "game_*.json"))
        
        for file in json_files:
            try:
                with open(file, 'r') as f:
                    self.games.append(json.load(f))
            except Exception as e:
                print(f"Error loading {file}: {e}")
        
        print(f"Loaded {len(self.games)} games for analysis")
    
    def detect_opening_patterns(self):
        """Detect common opening patterns"""
        print("\n=== OPENING PATTERNS ===")
        
        opening_sequences = []
        for game in self.games:
            if len(game['moves']) >= 4:
                # First 4 moves (2 from each player)
                opening = ' '.join([
                    f"{m['from']}{m['to']}" 
                    for m in game['moves'][:4]
                ])
                opening_sequences.append(opening)
                self.patterns[opening] += 1
        
        # Find most common openings
        most_common = sorted(self.patterns.items(), key=lambda x: x[1], reverse=True)[:5]
        
        for i, (pattern, count) in enumerate(most_common, 1):
            print(f"{i}. {pattern}: {count} times ({count/len(self.games)*100:.1f}%)")
    
    def analyze_player_style(self):
        """Analyze playing style characteristics"""
        print("\n=== PLAYER STYLE ANALYSIS ===")
        
        white_stats = {'aggressive': 0, 'defensive': 0, 'balanced': 0}
        black_stats = {'aggressive': 0, 'defensive': 0, 'balanced': 0}
        
        for game in self.games:
            white_captures = 0
            black_captures = 0
            white_moves = 0
            black_moves = 0
            
            for move in game['moves']:
                if move['color'] == 'white':
                    white_moves += 1
                    if move['captured']:
                        white_captures += 1
                else:
                    black_moves += 1
                    if move['captured']:
                        black_captures += 1
            
            # Classify style based on capture rate
            white_aggression = white_captures / max(white_moves, 1)
            black_aggression = black_captures / max(black_moves, 1)
            
            if white_aggression > 0.3:
                white_stats['aggressive'] += 1
            elif white_aggression < 0.1:
                white_stats['defensive'] += 1
            else:
                white_stats['balanced'] += 1
            
            if black_aggression > 0.3:
                black_stats['aggressive'] += 1
            elif black_aggression < 0.1:
                black_stats['defensive'] += 1
            else:
                black_stats['balanced'] += 1
        
        print("White Player Style:")
        for style, count in white_stats.items():
            print(f"  {style.capitalize()}: {count} games ({count/len(self.games)*100:.1f}%)")
        
        print("\nBlack Player Style:")
        for style, count in black_stats.items():
            print(f"  {style.capitalize()}: {count} games ({count/len(self.games)*100:.1f}%)")
    
    def analyze_piece_effectiveness(self):
        """Analyze which pieces are most effective"""
        print("\n=== PIECE EFFECTIVENESS ===")
        
        piece_captures = defaultdict(int)
        piece_moves = defaultdict(int)
        
        for game in self.games:
            for move in game['moves']:
                piece = move['piece']
                piece_moves[piece] += 1
                if move['captured']:
                    piece_captures[piece] += 1
        
        print("Piece Statistics:")
        for piece in sorted(piece_moves.keys()):
            total = piece_moves[piece]
            captures = piece_captures[piece]
            effectiveness = captures / max(total, 1) * 100
            print(f"  {piece}: {total} moves, {captures} captures ({effectiveness:.1f}% effectiveness)")
    
    def analyze_game_phases(self):
        """Analyze opening, middlegame, and endgame"""
        print("\n=== GAME PHASE ANALYSIS ===")
        
        phase_stats = {
            'opening': {'moves': 0, 'captures': 0},
            'middlegame': {'moves': 0, 'captures': 0},
            'endgame': {'moves': 0, 'captures': 0}
        }
        
        for game in self.games:
            total_moves = len(game['moves'])
            
            for i, move in enumerate(game['moves']):
                if i < 10:
                    phase = 'opening'
                elif i < total_moves - 10:
                    phase = 'middlegame'
                else:
                    phase = 'endgame'
                
                phase_stats[phase]['moves'] += 1
                if move['captured']:
                    phase_stats[phase]['captures'] += 1
        
        for phase, stats in phase_stats.items():
            moves = stats['moves']
            captures = stats['captures']
            capture_rate = captures / max(moves, 1) * 100
            print(f"{phase.capitalize()}:")
            print(f"  Total moves: {moves}")
            print(f"  Captures: {captures} ({capture_rate:.1f}%)")
    
    def predict_game_outcome(self, move_sequence):
        """Simple prediction based on historical patterns"""
        if not HAS_NUMPY:
            print("NumPy required for predictions")
            return None
        
        print("\n=== OUTCOME PREDICTION ===")
        
        # Count wins after similar openings
        white_wins = 0
        black_wins = 0
        total = 0
        
        for game in self.games:
            if len(game['moves']) >= len(move_sequence):
                match = True
                for i, target_move in enumerate(move_sequence):
                    if game['moves'][i]['from'] != target_move[0] or \
                       game['moves'][i]['to'] != target_move[1]:
                        match = False
                        break
                
                if match:
                    total += 1
                    result = game['result'].upper()
                    if 'WHITE' in result:
                        white_wins += 1
                    elif 'BLACK' in result:
                        black_wins += 1
        
        if total > 0:
            print(f"Based on {total} similar games:")
            print(f"  White win probability: {white_wins/total*100:.1f}%")
            print(f"  Black win probability: {black_wins/total*100:.1f}%")
            print(f"  Draw probability: {(total-white_wins-black_wins)/total*100:.1f}%")
        else:
            print("No similar games found for prediction")
    
    def visualize_statistics(self):
        """Create visualization of game statistics"""
        if not HAS_MATPLOTLIB:
            print("Matplotlib required for visualization")
            return
        
        print("\n=== GENERATING VISUALIZATIONS ===")
        
        # Result distribution
        results = {'White': 0, 'Black': 0, 'Draw': 0}
        for game in self.games:
            result = game['result'].upper()
            if 'WHITE_WON' in result:
                results['White'] += 1
            elif 'BLACK_WON' in result:
                results['Black'] += 1
            else:
                results['Draw'] += 1
        
        # Move length distribution
        move_lengths = [len(game['moves']) for game in self.games]
        
        fig, axes = plt.subplots(1, 2, figsize=(12, 5))
        
        # Pie chart for results
        axes[0].pie(results.values(), labels=results.keys(), autopct='%1.1f%%',
                    colors=['lightblue', 'lightcoral', 'lightgray'])
        axes[0].set_title('Game Results Distribution')
        
        # Histogram for game lengths
        axes[1].hist(move_lengths, bins=20, color='steelblue', edgecolor='black')
        axes[1].set_xlabel('Number of Moves')
        axes[1].set_ylabel('Frequency')
        axes[1].set_title('Game Length Distribution')
        axes[1].grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.savefig('chess_statistics.png', dpi=150)
        print("Visualization saved: chess_statistics.png")
        plt.close()
    
    def generate_recommendations(self):
        """Generate improvement recommendations"""
        print("\n=== IMPROVEMENT RECOMMENDATIONS ===")
        
        # Analyze common mistakes
        quick_losses = []
        for game in self.games:
            if len(game['moves']) < 20:
                quick_losses.append(game)
        
        if quick_losses:
            print(f"1. Avoid quick losses: {len(quick_losses)} games ended in under 20 moves")
            print("   Recommendation: Study opening principles more carefully")
        
        # Analyze piece development
        undeveloped_games = 0
        for game in self.games:
            if len(game['moves']) >= 10:
                pieces_moved = set()
                for move in game['moves'][:10]:
                    pieces_moved.add(move['piece'])
                
                if len(pieces_moved) < 4:
                    undeveloped_games += 1
        
        if undeveloped_games > 0:
            print(f"2. Improve piece development: {undeveloped_games} games had poor opening development")
            print("   Recommendation: Move different pieces in the opening, not the same piece repeatedly")
        
        # Check capture rates
        total_captures = 0
        total_moves = 0
        for game in self.games:
            for move in game['moves']:
                total_moves += 1
                if move['captured']:
                    total_captures += 1
        
        capture_rate = total_captures / max(total_moves, 1)
        if capture_rate < 0.15:
            print(f"3. Consider more tactical play: Only {capture_rate*100:.1f}% of moves were captures")
            print("   Recommendation: Look for tactical opportunities and piece exchanges")
    
    def full_analysis(self):
        """Run complete analysis suite"""
        print("="*70)
        print("ADVANCED CHESS GAME ANALYSIS")
        print("="*70)
        
        if not self.games:
            self.load_all_games()
        
        if not self.games:
            print("No games found to analyze!")
            return
        
        self.detect_opening_patterns()
        self.analyze_player_style()
        self.analyze_piece_effectiveness()
        self.analyze_game_phases()
        self.generate_recommendations()
        
        if HAS_MATPLOTLIB:
            self.visualize_statistics()
        
        print("\n" + "="*70)
        print("Analysis complete!")
        print("="*70)

def main():
    analyzer = AdvancedChessAnalyzer()
    analyzer.full_analysis()

if __name__ == "__main__":
    main()