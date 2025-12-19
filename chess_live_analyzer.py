#!/usr/bin/env python3
"""
Chess Live Analyzer - Real-time Game Analysis Server
Automatically analyzes every move as the game progresses
"""

import json
import os
import time
import threading
from datetime import datetime
from collections import deque

class LiveChessAnalyzer:
    def __init__(self, bridge_dir="python_bridge"):
        self.bridge_dir = bridge_dir
        self.running = True
        self.current_game = None
        self.move_count = 0
        self.analysis_history = deque(maxlen=100)
        
        # Analysis thresholds
        self.brilliant_threshold = 0.9
        self.good_threshold = 0.7
        self.mistake_threshold = 0.3
        self.blunder_threshold = 0.1
        
        # Ensure directory exists
        os.makedirs(bridge_dir, exist_ok=True)
        
        print("="*70)
        print("CHESS LIVE ANALYZER - STARTED")
        print("="*70)
        print(f"Monitoring directory: {os.path.abspath(bridge_dir)}")
        print(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("Waiting for games to start...")
        print("Press Ctrl+C to stop")
        print("="*70)
        print()
    
    def start(self):
        """Start the live analysis server"""
        monitor_thread = threading.Thread(target=self.monitor_games, daemon=True)
        monitor_thread.start()
        
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n\nShutting down analyzer...")
            self.running = False
    
    def monitor_games(self):
        """Monitor for new games and moves"""
        last_move_count = 0
        current_file = None
        
        while self.running:
            try:
                # Check for game files
                json_files = [f for f in os.listdir(self.bridge_dir) 
                             if f.endswith('.json') and f.startswith('game_')]
                
                if not json_files:
                    time.sleep(1)
                    continue
                
                # Get most recent file
                latest_file = max(json_files, 
                                key=lambda f: os.path.getmtime(os.path.join(self.bridge_dir, f)))
                full_path = os.path.join(self.bridge_dir, latest_file)
                
                # Load game data
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        game_data = json.load(f)
                except json.JSONDecodeError:
                    # File might be being written, skip this iteration
                    time.sleep(0.5)
                    continue
                
                # Check if this is a new game
                if current_file != latest_file:
                    current_file = latest_file
                    last_move_count = 0
                    self.on_new_game(game_data)
                
                # Check for new moves
                moves = game_data.get('moves', [])
                current_moves = len(moves)
                if current_moves > last_move_count:
                    # Analyze new moves
                    for i in range(last_move_count, current_moves):
                        self.analyze_move(game_data, i)
                    last_move_count = current_moves
                
                # Check if game ended
                result = game_data.get('result', 'PLAYING')
                if result not in ['PLAYING', None, '']:
                    self.on_game_end(game_data)
                    current_file = None
                    last_move_count = 0
                
                time.sleep(0.5)  # Check every 0.5 seconds
                
            except Exception as e:
                print(f"Error in monitor: {e}")
                time.sleep(1)
    
    def on_new_game(self, game_data):
        """Called when a new game starts"""
        self.current_game = game_data
        self.move_count = 0
        self.analysis_history.clear()
        
        print("\n" + "="*70)
        print(f"🎮 NEW GAME STARTED")
        print("="*70)
        print(f"Game ID: {game_data.get('timestamp', 'Unknown')}")
        print(f"Time: {datetime.now().strftime('%H:%M:%S')}")
        print("="*70)
        print()
    
    def analyze_move(self, game_data, move_index):
        """Analyze a single move in real-time"""
        moves = game_data.get('moves', [])
        if move_index >= len(moves):
            return
        
        move = moves[move_index]
        move_num = move_index + 1
        color = move.get('color', 'unknown')
        piece = move.get('piece', 'Unknown')
        from_sq = move.get('from', '??')
        to_sq = move.get('to', '??')
        captured = move.get('captured')
        
        # Perform analysis
        analysis = self.evaluate_move(game_data, move_index)
        
        # Print move with analysis
        print(f"Move {move_num:3d}: {color:5s} {piece:6s} {from_sq} → {to_sq}", end="")
        
        if captured:
            print(f" [CAPTURE: {captured}]", end="")
        
        # Add evaluation symbol and comment
        symbol, comment, color_code = self.get_evaluation_display(analysis)
        print(f" {symbol} {color_code}{comment}\033[0m")
        
        # Store analysis
        self.analysis_history.append({
            'move_num': move_num,
            'move': move,
            'analysis': analysis
        })
    
    def evaluate_move(self, game_data, move_index):
        """Evaluate the quality of a move"""
        moves = game_data.get('moves', [])
        move = moves[move_index]
        
        analysis = {
            'type': 'normal',
            'score': 0.5,
            'reasons': []
        }
        
        # Extract move details
        piece = move.get('piece', 'Unknown')
        from_sq = move.get('from', '??')
        to_sq = move.get('to', '??')
        captured = move.get('captured')
        color = move.get('color', 'unknown')
        
        # Opening moves evaluation (first 10 moves)
        if move_index < 10:
            opening_eval = self.evaluate_opening(move, move_index)
            analysis['score'] += opening_eval['score'] - 0.5
            analysis['reasons'].extend(opening_eval['reasons'])
        
        # Capture evaluation
        if captured:
            capture_eval = self.evaluate_capture(move, captured)
            analysis['score'] += capture_eval['score'] - 0.5
            analysis['reasons'].extend(capture_eval['reasons'])
        
        # Piece development
        if piece in ['Knight', 'Bishop'] and move_index < 15:
            analysis['score'] += 0.1
            analysis['reasons'].append("Good piece development")
        
        # Center control
        if to_sq in ['d4', 'd5', 'e4', 'e5']:
            analysis['score'] += 0.15
            analysis['reasons'].append("Controls center")
        
        # Check for common mistakes
        mistakes = self.check_mistakes(game_data, move_index)
        if mistakes:
            analysis['score'] -= 0.3
            analysis['reasons'].extend(mistakes)
        
        # Tactical patterns
        tactics = self.detect_tactics(game_data, move_index)
        if tactics:
            analysis['score'] += 0.2
            analysis['reasons'].extend(tactics)
        
        # Classify move quality
        if analysis['score'] >= 0.9:
            analysis['type'] = 'brilliant'
        elif analysis['score'] >= 0.7:
            analysis['type'] = 'good'
        elif analysis['score'] >= 0.5:
            analysis['type'] = 'normal'
        elif analysis['score'] >= 0.3:
            analysis['type'] = 'inaccuracy'
        elif analysis['score'] >= 0.15:
            analysis['type'] = 'mistake'
        else:
            analysis['type'] = 'blunder'
        
        return analysis
    
    def evaluate_opening(self, move, move_index):
        """Evaluate opening principles"""
        result = {'score': 0.5, 'reasons': []}
        piece = move.get('piece', 'Unknown')
        to_sq = move.get('to', '??')
        
        # Good opening principles
        if move_index == 0 and piece == 'Pawn' and to_sq in ['e4', 'd4', 'e5', 'd5']:
            result['score'] = 0.8
            result['reasons'].append("Excellent opening move!")
        
        if piece == 'Knight' and move_index <= 5:
            result['score'] += 0.2
            result['reasons'].append("Developing knights early")
        
        if piece == 'Bishop' and move_index <= 8:
            result['score'] += 0.15
            result['reasons'].append("Good bishop development")
        
        # Bad opening moves
        if piece == 'Queen' and move_index < 5:
            result['score'] -= 0.3
            result['reasons'].append("⚠️ Queen out too early!")
        
        if piece == 'King' and move_index < 10:
            result['score'] -= 0.4
            result['reasons'].append("⚠️ Moving king early is risky!")
        
        return result
    
    def evaluate_capture(self, move, captured):
        """Evaluate capture moves"""
        result = {'score': 0.6, 'reasons': []}
        
        # Piece values
        values = {
            'Pawn': 1, 'Knight': 3, 'Bishop': 3,
            'Rook': 5, 'Queen': 9, 'King': 0
        }
        
        capturing_piece = move.get('piece', 'Unknown')
        
        if capturing_piece in values and captured in values:
            trade_value = values[captured] - values[capturing_piece]
            
            if trade_value > 0:
                result['score'] += 0.3
                result['reasons'].append(f"✓ Good trade! Won material")
            elif trade_value == 0:
                result['score'] += 0.1
                result['reasons'].append("Equal trade")
            else:
                result['score'] -= 0.2
                result['reasons'].append("⚠️ Bad trade - lost material")
        
        return result
    
    def check_mistakes(self, game_data, move_index):
        """Check for common mistakes"""
        mistakes = []
        moves = game_data.get('moves', [])
        
        if move_index < 1:
            return mistakes
        
        move = moves[move_index]
        prev_move = moves[move_index - 1]
        
        # Moving same piece twice in opening
        if move_index < 10:
            if (move.get('piece') == prev_move.get('piece') and 
                move.get('color') == prev_move.get('color')):
                mistakes.append("⚠️ Moving same piece twice in opening")
        
        # Exposed king
        if move.get('piece') == 'King' and move.get('to') in ['e4', 'd4', 'e5', 'd5']:
            mistakes.append("⚠️ King too exposed in center!")
        
        return mistakes
    
    def detect_tactics(self, game_data, move_index):
        """Detect tactical patterns"""
        tactics = []
        moves = game_data.get('moves', [])
        move = moves[move_index]
        
        piece = move.get('piece', 'Unknown')
        
        # Check for fork potential (knight moves)
        if piece == 'Knight':
            tactics.append("Knight active - fork potential")
        
        # Check for pin potential (bishop/rook/queen on lines)
        if piece in ['Bishop', 'Rook', 'Queen']:
            tactics.append("Long-range piece - pin potential")
        
        # Discovered attack potential
        if move_index > 0:
            prev_move = moves[move_index - 1]
            prev_from = prev_move.get('from', '??')
            curr_from = move.get('from', '??')
            
            if len(prev_from) == 2 and len(curr_from) == 2:
                if prev_from[0] == curr_from[0] or prev_from[1] == curr_from[1]:
                    tactics.append("Possible discovered attack")
        
        return tactics
    
    def get_evaluation_display(self, analysis):
        """Get display symbol and color for evaluation"""
        eval_type = analysis['type']
        
        displays = {
            'brilliant': ('💎', 'BRILLIANT!', '\033[96m'),      # Cyan
            'good': ('✓', 'Good move', '\033[92m'),             # Green
            'normal': ('·', 'Normal', '\033[90m'),              # Gray
            'inaccuracy': ('?', 'Inaccuracy', '\033[93m'),      # Yellow
            'mistake': ('??', 'Mistake', '\033[91m'),           # Red
            'blunder': ('???', 'BLUNDER!', '\033[95m')          # Magenta
        }
        
        symbol, comment, color = displays.get(eval_type, ('·', 'Normal', '\033[90m'))
        
        # Add reasons (max 2)
        if analysis['reasons']:
            comment += f" - {', '.join(analysis['reasons'][:2])}"
        
        return symbol, comment, color
    
    def on_game_end(self, game_data):
        """Called when game ends"""
        print("\n" + "="*70)
        print(f"🏁 GAME ENDED")
        print("="*70)
        
        result = game_data.get('result', 'UNKNOWN')
        moves = game_data.get('moves', [])
        total_moves = len(moves)
        
        print(f"Result: {result}")
        print(f"Total Moves: {total_moves}")
        print(f"Time: {datetime.now().strftime('%H:%M:%S')}")
        
        # Generate game summary
        if self.analysis_history:
            self.generate_game_summary(game_data)
        
        print("="*70)
        print("\nWaiting for next game...")
        print()
    
    def generate_game_summary(self, game_data):
        """Generate summary of the game"""
        moves = game_data.get('moves', [])
        
        # Count move types
        move_types = {
            'brilliant': 0, 'good': 0, 'normal': 0,
            'inaccuracy': 0, 'mistake': 0, 'blunder': 0
        }
        
        white_stats = move_types.copy()
        black_stats = move_types.copy()
        
        for analysis_data in self.analysis_history:
            move_color = analysis_data['move'].get('color', 'unknown')
            move_type = analysis_data['analysis']['type']
            
            if move_color == 'white':
                white_stats[move_type] += 1
            else:
                black_stats[move_type] += 1
        
        print("\n📊 GAME SUMMARY:")
        print("-" * 70)
        
        print("\nWhite Performance:")
        self.print_player_stats(white_stats)
        
        print("\nBlack Performance:")
        self.print_player_stats(black_stats)
        
        # Best and worst moves
        print("\n🌟 Highlights:")
        self.print_highlights()
    
    def print_player_stats(self, stats):
        """Print player statistics"""
        total = sum(stats.values())
        if total == 0:
            print("  No moves recorded")
            return
        
        print(f"  💎 Brilliant moves: {stats['brilliant']}")
        print(f"  ✓ Good moves: {stats['good']}")
        print(f"  · Normal moves: {stats['normal']}")
        print(f"  ? Inaccuracies: {stats['inaccuracy']}")
        print(f"  ?? Mistakes: {stats['mistake']}")
        print(f"  ??? Blunders: {stats['blunder']}")
        
        accuracy = ((stats['brilliant']*1.0 + stats['good']*0.8 + stats['normal']*0.5) / total) * 100
        print(f"  📈 Accuracy: {accuracy:.1f}%")
    
    def print_highlights(self):
        """Print game highlights"""
        if not self.analysis_history:
            return
        
        # Find brilliant moves
        brilliant = [a for a in self.analysis_history if a['analysis']['type'] == 'brilliant']
        if brilliant:
            print("  Best moves:")
            for item in brilliant[:3]:
                m = item['move']
                color = m.get('color', 'unknown')
                piece = m.get('piece', 'Unknown')
                from_sq = m.get('from', '??')
                to_sq = m.get('to', '??')
                print(f"    Move {item['move_num']}: {color} {piece} {from_sq}→{to_sq}")
        
        # Find blunders
        blunders = [a for a in self.analysis_history if a['analysis']['type'] == 'blunder']
        if blunders:
            print("  Critical mistakes:")
            for item in blunders[:3]:
                m = item['move']
                color = m.get('color', 'unknown')
                piece = m.get('piece', 'Unknown')
                from_sq = m.get('from', '??')
                to_sq = m.get('to', '??')
                print(f"    Move {item['move_num']}: {color} {piece} {from_sq}→{to_sq}")

def main():
    print("Chess Live Analyzer v1.0")
    analyzer = LiveChessAnalyzer()
    
    try:
        analyzer.start()
    except KeyboardInterrupt:
        print("\n\nAnalyzer stopped.")
    except Exception as e:
        print(f"\n\nError: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()