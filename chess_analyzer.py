#!/usr/bin/env python3
"""
Chess Game Analyzer - Python Side
Analyzes chess games saved by Java application
"""

import json
import os
import glob
from datetime import datetime
from pathlib import Path

class ChessAnalyzer:
    def __init__(self, bridge_dir="python_bridge"):
        self.bridge_dir = bridge_dir
        self.stats = {
            'total_games': 0,
            'white_wins': 0,
            'black_wins': 0,
            'stalemates': 0,
            'total_moves': 0,
            'captures': 0,
            'avg_game_length': 0
        }
    
    def check_for_new_games(self):
        """Check if there are new games to analyze"""
        flag_file = os.path.join(self.bridge_dir, "new_game.flag")
        if os.path.exists(flag_file):
            os.remove(flag_file)
            return True
        return False
    
    def load_game(self, json_file):
        """Load a game from JSON file"""
        try:
            with open(json_file, 'r') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error loading {json_file}: {e}")
            return None
    
    def analyze_game(self, game_data):
        """Analyze a single game"""
        if not game_data:
            return None
        
        analysis = {
            'timestamp': game_data['timestamp'],
            'result': game_data['result'],
            'total_moves': game_data['total_moves'],
            'white_moves': 0,
            'black_moves': 0,
            'white_captures': 0,
            'black_captures': 0,
            'piece_activity': {},
            'opening_moves': [],
            'endgame_moves': [],
            'average_move_time': 0,
            'comment': game_data.get('user_comment', '')
        }
        
        # Analyze moves
        move_times = []
        for i, move in enumerate(game_data['moves']):
            color = move['color']
            piece = move['piece']
            
            # Count moves by color
            if color == 'white':
                analysis['white_moves'] += 1
            else:
                analysis['black_moves'] += 1
            
            # Count captures
            if move['captured']:
                if color == 'white':
                    analysis['white_captures'] += 1
                else:
                    analysis['black_captures'] += 1
            
            # Track piece activity
            if piece not in analysis['piece_activity']:
                analysis['piece_activity'][piece] = 0
            analysis['piece_activity'][piece] += 1
            
            # Opening (first 10 moves)
            if i < 10:
                analysis['opening_moves'].append(f"{move['from']}->{move['to']}")
            
            # Endgame (last 10 moves)
            if i >= len(game_data['moves']) - 10:
                analysis['endgame_moves'].append(f"{move['from']}->{move['to']}")
            
            # Calculate move times if available
            if i > 0:
                time_diff = move['timestamp'] - game_data['moves'][i-1]['timestamp']
                move_times.append(time_diff / 1000.0)  # Convert to seconds
        
        if move_times:
            analysis['average_move_time'] = sum(move_times) / len(move_times)
        
        return analysis
    
    def analyze_all_games(self):
        """Analyze all games in bridge directory"""
        json_files = glob.glob(os.path.join(self.bridge_dir, "game_*.json"))
        
        if not json_files:
            print("No games found to analyze.")
            return
        
        print(f"\n{'='*60}")
        print(f"CHESS GAME ANALYSIS REPORT")
        print(f"{'='*60}")
        print(f"Total games found: {len(json_files)}\n")
        
        all_analyses = []
        
        for json_file in sorted(json_files):
            game_data = self.load_game(json_file)
            if game_data:
                analysis = self.analyze_game(game_data)
                if analysis:
                    all_analyses.append(analysis)
                    self.update_stats(game_data, analysis)
                    self.print_game_summary(analysis)
        
        self.print_overall_stats()
        
        # Save analysis report
        self.save_analysis_report(all_analyses)
    
    def update_stats(self, game_data, analysis):
        """Update overall statistics"""
        self.stats['total_games'] += 1
        
        result = game_data['result'].upper()
        if 'WHITE_WON' in result:
            self.stats['white_wins'] += 1
        elif 'BLACK_WON' in result:
            self.stats['black_wins'] += 1
        elif 'STALEMATE' in result:
            self.stats['stalemates'] += 1
        
        self.stats['total_moves'] += game_data['total_moves']
        self.stats['captures'] += analysis['white_captures'] + analysis['black_captures']
    
    def print_game_summary(self, analysis):
        """Print summary of a single game"""
        print(f"\n--- Game {analysis['timestamp']} ---")
        print(f"Result: {analysis['result']}")
        print(f"Total Moves: {analysis['total_moves']} (White: {analysis['white_moves']}, Black: {analysis['black_moves']})")
        print(f"Captures: White {analysis['white_captures']}, Black {analysis['black_captures']}")
        print(f"Average Move Time: {analysis['average_move_time']:.2f}s")
        print(f"Most Active Piece: {max(analysis['piece_activity'], key=analysis['piece_activity'].get) if analysis['piece_activity'] else 'N/A'}")
        if analysis['comment']:
            print(f"Comment: {analysis['comment']}")
    
    def print_overall_stats(self):
        """Print overall statistics"""
        print(f"\n{'='*60}")
        print(f"OVERALL STATISTICS")
        print(f"{'='*60}")
        print(f"Total Games: {self.stats['total_games']}")
        print(f"White Wins: {self.stats['white_wins']} ({self.stats['white_wins']/max(1,self.stats['total_games'])*100:.1f}%)")
        print(f"Black Wins: {self.stats['black_wins']} ({self.stats['black_wins']/max(1,self.stats['total_games'])*100:.1f}%)")
        print(f"Stalemates: {self.stats['stalemates']} ({self.stats['stalemates']/max(1,self.stats['total_games'])*100:.1f}%)")
        print(f"Total Moves: {self.stats['total_moves']}")
        print(f"Average Game Length: {self.stats['total_moves']/max(1,self.stats['total_games']):.1f} moves")
        print(f"Total Captures: {self.stats['captures']}")
        print(f"{'='*60}\n")
    
    def save_analysis_report(self, analyses):
        """Save analysis report to file"""
        report_file = f"analysis_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        
        with open(report_file, 'w') as f:
            f.write("="*60 + "\n")
            f.write("CHESS GAME ANALYSIS REPORT\n")
            f.write("="*60 + "\n\n")
            
            for analysis in analyses:
                f.write(f"Game {analysis['timestamp']}\n")
                f.write(f"  Result: {analysis['result']}\n")
                f.write(f"  Moves: {analysis['total_moves']}\n")
                f.write(f"  Captures: W{analysis['white_captures']} B{analysis['black_captures']}\n")
                f.write(f"  Avg Move Time: {analysis['average_move_time']:.2f}s\n")
                f.write(f"  Comment: {analysis['comment']}\n")
                f.write("\n")
            
            f.write("\nOVERALL STATISTICS\n")
            f.write(f"Total Games: {self.stats['total_games']}\n")
            f.write(f"White Wins: {self.stats['white_wins']}\n")
            f.write(f"Black Wins: {self.stats['black_wins']}\n")
            f.write(f"Stalemates: {self.stats['stalemates']}\n")
        
        print(f"Analysis report saved: {report_file}")

def main():
    analyzer = ChessAnalyzer()
    
    # Check for new games
    if analyzer.check_for_new_games():
        print("New game detected! Starting analysis...")
    
    # Analyze all games
    analyzer.analyze_all_games()

if __name__ == "__main__":
    main()