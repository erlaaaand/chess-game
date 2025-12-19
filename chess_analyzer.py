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
        
        # Ensure directory exists
        os.makedirs(bridge_dir, exist_ok=True)
    
    def check_for_new_games(self):
        """Check if there are new games to analyze"""
        flag_file = os.path.join(self.bridge_dir, "new_game.flag")
        if os.path.exists(flag_file):
            try:
                os.remove(flag_file)
                return True
            except Exception as e:
                print(f"Error removing flag file: {e}")
        return False
    
    def load_game(self, json_file):
        """Load a game from JSON file"""
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            print(f"Error parsing JSON from {json_file}: {e}")
            return None
        except Exception as e:
            print(f"Error loading {json_file}: {e}")
            return None
    
    def analyze_game(self, game_data):
        """Analyze a single game"""
        if not game_data:
            return None
        
        analysis = {
            'timestamp': game_data.get('timestamp', 'Unknown'),
            'result': game_data.get('result', 'Unknown'),
            'total_moves': game_data.get('total_moves', 0),
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
        
        moves = game_data.get('moves', [])
        if not moves:
            return analysis
        
        # Analyze moves
        move_times = []
        for i, move in enumerate(moves):
            color = move.get('color', 'unknown')
            piece = move.get('piece', 'Unknown')
            
            # Count moves by color
            if color == 'white':
                analysis['white_moves'] += 1
            else:
                analysis['black_moves'] += 1
            
            # Count captures
            if move.get('captured'):
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
                move_from = move.get('from', '??')
                move_to = move.get('to', '??')
                analysis['opening_moves'].append(f"{move_from}->{move_to}")
            
            # Endgame (last 10 moves)
            if i >= len(moves) - 10:
                move_from = move.get('from', '??')
                move_to = move.get('to', '??')
                analysis['endgame_moves'].append(f"{move_from}->{move_to}")
            
            # Calculate move times if available
            if i > 0 and 'timestamp' in move and 'timestamp' in moves[i-1]:
                try:
                    time_diff = move['timestamp'] - moves[i-1]['timestamp']
                    move_times.append(time_diff / 1000.0)  # Convert to seconds
                except (TypeError, KeyError):
                    pass
        
        if move_times:
            analysis['average_move_time'] = sum(move_times) / len(move_times)
        
        return analysis
    
    def analyze_all_games(self):
        """Analyze all games in bridge directory"""
        json_files = glob.glob(os.path.join(self.bridge_dir, "game_*.json"))
        
        if not json_files:
            print("\nNo games found to analyze.")
            print(f"Games should be in: {os.path.abspath(self.bridge_dir)}/")
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
        
        if all_analyses:
            self.print_overall_stats()
            self.save_analysis_report(all_analyses)
        else:
            print("\nNo valid games to analyze.")
    
    def update_stats(self, game_data, analysis):
        """Update overall statistics"""
        self.stats['total_games'] += 1
        
        result = str(game_data.get('result', '')).upper()
        if 'WHITE_WON' in result or 'WHITE' in result:
            self.stats['white_wins'] += 1
        elif 'BLACK_WON' in result or 'BLACK' in result:
            self.stats['black_wins'] += 1
        elif 'STALEMATE' in result or 'DRAW' in result:
            self.stats['stalemates'] += 1
        
        self.stats['total_moves'] += game_data.get('total_moves', 0)
        self.stats['captures'] += analysis['white_captures'] + analysis['black_captures']
    
    def print_game_summary(self, analysis):
        """Print summary of a single game"""
        print(f"\n--- Game {analysis['timestamp']} ---")
        print(f"Result: {analysis['result']}")
        print(f"Total Moves: {analysis['total_moves']} (White: {analysis['white_moves']}, Black: {analysis['black_moves']})")
        print(f"Captures: White {analysis['white_captures']}, Black {analysis['black_captures']}")
        print(f"Average Move Time: {analysis['average_move_time']:.2f}s")
        
        if analysis['piece_activity']:
            most_active = max(analysis['piece_activity'], key=analysis['piece_activity'].get)
            print(f"Most Active Piece: {most_active} ({analysis['piece_activity'][most_active]} moves)")
        
        if analysis['comment']:
            print(f"Comment: {analysis['comment']}")
    
    def print_overall_stats(self):
        """Print overall statistics"""
        print(f"\n{'='*60}")
        print(f"OVERALL STATISTICS")
        print(f"{'='*60}")
        
        total = max(1, self.stats['total_games'])
        
        print(f"Total Games: {self.stats['total_games']}")
        print(f"White Wins: {self.stats['white_wins']} ({self.stats['white_wins']/total*100:.1f}%)")
        print(f"Black Wins: {self.stats['black_wins']} ({self.stats['black_wins']/total*100:.1f}%)")
        print(f"Stalemates: {self.stats['stalemates']} ({self.stats['stalemates']/total*100:.1f}%)")
        print(f"Total Moves: {self.stats['total_moves']}")
        print(f"Average Game Length: {self.stats['total_moves']/total:.1f} moves")
        print(f"Total Captures: {self.stats['captures']}")
        print(f"Average Captures per Game: {self.stats['captures']/total:.1f}")
        print(f"{'='*60}\n")
    
    def save_analysis_report(self, analyses):
        """Save analysis report to file"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        report_file = f"analysis_report_{timestamp}.txt"
        
        try:
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write("="*60 + "\n")
                f.write("CHESS GAME ANALYSIS REPORT\n")
                f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
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
                f.write(f"Total Moves: {self.stats['total_moves']}\n")
                f.write(f"Total Captures: {self.stats['captures']}\n")
            
            print(f"✓ Analysis report saved: {report_file}")
        except Exception as e:
            print(f"Error saving report: {e}")

def main():
    print("="*60)
    print("Chess Game Analyzer v1.0")
    print("="*60)
    
    analyzer = ChessAnalyzer()
    
    # Check for new games
    if analyzer.check_for_new_games():
        print("New game detected! Starting analysis...")
    
    # Analyze all games
    analyzer.analyze_all_games()
    
    print("\nAnalysis complete!")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nAnalysis interrupted by user.")
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()