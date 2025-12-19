#!/usr/bin/env python3
"""
Chess AI Engine dengan Learning Capability
Berkomunikasi dengan Java melalui JSON di stdin/stdout
"""

import sys
import json
import random
import pickle
from pathlib import Path
from typing import Dict, List, Tuple, Optional
import chess
import chess.engine

class ChessAIEngine:
    def __init__(self):
        self.characters = {}
        self.characters_dir = Path("ai_characters")
        self.characters_dir.mkdir(exist_ok=True)
        
        # Simple evaluation weights
        self.piece_values = {
            chess.PAWN: 100,
            chess.KNIGHT: 320,
            chess.BISHOP: 330,
            chess.ROOK: 500,
            chess.QUEEN: 900,
            chess.KING: 20000
        }
        
        # Position tables untuk evaluasi posisi
        self.pawn_table = [
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
        ]
        
        self.knight_table = [
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
        ]
        
    def get_best_move(self, fen: str, personality: Dict, time_limit: int) -> Optional[str]:
        """Get best move based on position and character personality"""
        try:
            board = chess.Board(fen)
            
            if board.is_game_over():
                return None
            
            # Get all legal moves
            legal_moves = list(board.legal_moves)
            if not legal_moves:
                return None
            
            # Evaluate moves based on personality
            move_scores = []
            for move in legal_moves:
                score = self.evaluate_move(board, move, personality)
                move_scores.append((move, score))
            
            # Sort by score
            move_scores.sort(key=lambda x: x[1], reverse=True)
            
            # Add randomness based on risk_taking
            risk_taking = personality.get('risk_taking', 0.5)
            
            if risk_taking > 0.7:
                # High risk: sometimes pick sub-optimal moves
                if random.random() < 0.2:
                    top_moves = move_scores[:5] if len(move_scores) >= 5 else move_scores
                    selected = random.choice(top_moves)
                else:
                    selected = move_scores[0]
            elif risk_taking < 0.3:
                # Low risk: always pick safe moves
                # Filter out risky moves (captures that lose material)
                safe_moves = [m for m in move_scores if m[1] >= 0]
                selected = safe_moves[0] if safe_moves else move_scores[0]
            else:
                # Normal: pick best move with slight randomness
                if random.random() < 0.1 and len(move_scores) > 1:
                    selected = move_scores[1]
                else:
                    selected = move_scores[0]
            
            return selected[0].uci()
            
        except Exception as e:
            print(f"Error getting move: {e}", file=sys.stderr)
            return None
    
    def evaluate_move(self, board: chess.Board, move: chess.Move, personality: Dict) -> float:
        """Evaluate a move based on multiple factors and personality"""
        score = 0.0
        
        # Make move temporarily
        board.push(move)
        
        # Material evaluation
        material_score = self.evaluate_material(board)
        score += material_score * 1.0
        
        # Position evaluation
        position_score = self.evaluate_position(board)
        score += position_score * personality.get('positional', 0.5)
        
        # Tactical evaluation
        tactical_score = self.evaluate_tactics(board, move)
        score += tactical_score * personality.get('tactical', 0.5)
        
        # Aggression bonus
        if board.is_capture(move):
            score += 50 * personality.get('aggression', 0.5)
        
        # Check bonus
        if board.is_check():
            score += 30 * personality.get('aggression', 0.5)
        
        # King safety
        king_safety = self.evaluate_king_safety(board)
        score += king_safety * personality.get('defensiveness', 0.5)
        
        # Center control
        center_control = self.evaluate_center_control(board, move)
        score += center_control * 20
        
        # Development bonus (early game)
        if board.fullmove_number <= 10:
            if move.from_square in [chess.B1, chess.G1, chess.B8, chess.G8,  # Knights
                                   chess.C1, chess.F1, chess.C8, chess.F8]:  # Bishops
                score += 15
        
        # Undo move
        board.pop()
        
        # Add small random factor
        score += random.uniform(-5, 5)
        
        return score
    
    def evaluate_material(self, board: chess.Board) -> float:
        """Calculate material balance"""
        score = 0
        for piece_type in [chess.PAWN, chess.KNIGHT, chess.BISHOP, 
                          chess.ROOK, chess.QUEEN, chess.KING]:
            score += len(board.pieces(piece_type, chess.WHITE)) * self.piece_values[piece_type]
            score -= len(board.pieces(piece_type, chess.BLACK)) * self.piece_values[piece_type]
        
        return score / 100.0  # Normalize
    
    def evaluate_position(self, board: chess.Board) -> float:
        """Evaluate piece positioning"""
        score = 0
        
        # Pawns
        for square in board.pieces(chess.PAWN, chess.WHITE):
            score += self.pawn_table[square]
        for square in board.pieces(chess.PAWN, chess.BLACK):
            score -= self.pawn_table[chess.square_mirror(square)]
        
        # Knights
        for square in board.pieces(chess.KNIGHT, chess.WHITE):
            score += self.knight_table[square]
        for square in board.pieces(chess.KNIGHT, chess.BLACK):
            score -= self.knight_table[chess.square_mirror(square)]
        
        return score / 100.0
    
    def evaluate_tactics(self, board: chess.Board, last_move: chess.Move) -> float:
        """Look for tactical opportunities"""
        score = 0
        
        # Check for checks
        if board.is_check():
            score += 30
        
        # Count attacked pieces
        for square in chess.SQUARES:
            piece = board.piece_at(square)
            if piece and piece.color != board.turn:
                if board.is_attacked_by(board.turn, square):
                    score += self.piece_values[piece.piece_type] / 200.0
        
        # Checkmate is best
        if board.is_checkmate():
            score += 10000
        
        return score
    
    def evaluate_king_safety(self, board: chess.Board) -> float:
        """Evaluate king safety"""
        score = 0
        
        # Castling rights
        if board.has_kingside_castling_rights(chess.WHITE):
            score += 20
        if board.has_queenside_castling_rights(chess.WHITE):
            score += 10
        if board.has_kingside_castling_rights(chess.BLACK):
            score -= 20
        if board.has_queenside_castling_rights(chess.BLACK):
            score -= 10
        
        return score
    
    def evaluate_center_control(self, board: chess.Board, move: chess.Move) -> float:
        """Evaluate center control"""
        center_squares = [chess.D4, chess.D5, chess.E4, chess.E5]
        score = 0
        
        if move.to_square in center_squares:
            score += 1.0
        
        return score
    
    def evaluate_move_quality(self, fen: str, move_uci: str) -> Dict:
        """Evaluate the quality of a move"""
        try:
            board = chess.Board(fen)
            move = chess.Move.from_uci(move_uci)
            
            # Get best moves
            legal_moves = list(board.legal_moves)
            move_scores = [(m, self.evaluate_move(board, m, {'aggression': 0.5, 'defensiveness': 0.5, 
                                                             'risk_taking': 0.5, 'tactical': 0.5, 
                                                             'positional': 0.5, 'patience': 0.5})) 
                          for m in legal_moves]
            move_scores.sort(key=lambda x: x[1], reverse=True)
            
            # Find played move in list
            played_score = next((score for m, score in move_scores if m == move), 0)
            best_score = move_scores[0][1] if move_scores else 0
            
            # Calculate quality
            if len(move_scores) == 1:
                quality = "normal"
            elif played_score >= best_score - 10:
                quality = "brilliant" if board.is_capture(move) or board.gives_check(move) else "good"
            elif played_score >= best_score - 50:
                quality = "normal"
            elif played_score >= best_score - 100:
                quality = "inaccuracy"
            elif played_score >= best_score - 200:
                quality = "mistake"
            else:
                quality = "blunder"
            
            return {
                "score": played_score,
                "quality": quality,
                "comment": self.get_move_comment(board, move, quality)
            }
            
        except Exception as e:
            print(f"Error evaluating move: {e}", file=sys.stderr)
            return {"score": 0, "quality": "normal", "comment": ""}
    
    def get_move_comment(self, board: chess.Board, move: chess.Move, quality: str) -> str:
        """Generate comment for move"""
        comments = {
            "brilliant": "Excellent move!",
            "good": "Good move",
            "normal": "Decent move",
            "inaccuracy": "Slightly inaccurate",
            "mistake": "This is a mistake",
            "blunder": "This is a blunder!"
        }
        
        comment = comments.get(quality, "")
        
        if board.is_capture(move):
            comment += " (Capture)"
        if board.gives_check(move):
            comment += " (Check)"
        
        return comment
    
    def train_character(self, character_name: str, moves: List[str], result: str):
        """Train character based on game"""
        # This is a placeholder for actual learning
        # In real implementation, you would:
        # 1. Analyze each move
        # 2. Update character's move preferences
        # 3. Adjust personality traits based on successful strategies
        print(f"Training {character_name} with {len(moves)} moves, result: {result}", 
              file=sys.stderr)
    
    def run(self):
        """Main loop - listen for commands from Java"""
        print("Chess AI Engine started", file=sys.stderr)
        sys.stderr.flush()
        
        while True:
            try:
                line = sys.stdin.readline()
                if not line:
                    break
                
                request = json.loads(line.strip())
                command = request.get('command')
                
                if command == 'quit':
                    break
                elif command == 'get_move':
                    fen = request['fen']
                    personality = request.get('personality', {})
                    time_limit = request.get('time_limit', 1000)
                    
                    move = self.get_best_move(fen, personality, time_limit)
                    response = {'move': move if move else 'none'}
                    
                elif command == 'evaluate_move':
                    fen = request['fen']
                    move = request['move']
                    
                    evaluation = self.evaluate_move_quality(fen, move)
                    response = evaluation
                    
                elif command == 'train':
                    character_name = request['character_name']
                    moves = json.loads(request['moves'])
                    result = request['result']
                    
                    self.train_character(character_name, moves, result)
                    response = {'status': 'ok'}
                    
                else:
                    response = {'error': 'Unknown command'}
                
                # Send response
                print(json.dumps(response), flush=True)
                
            except Exception as e:
                print(f"Error processing request: {e}", file=sys.stderr)
                print(json.dumps({'error': str(e)}), flush=True)
        
        print("Chess AI Engine stopped", file=sys.stderr)

if __name__ == '__main__':
    engine = ChessAIEngine()
    engine.run()