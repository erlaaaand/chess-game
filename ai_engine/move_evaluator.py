#!/usr/bin/env python3
"""
Move Evaluator - Evaluates chess move quality
Path: ai_engine/move_evaluator.py
"""

import chess
from typing import Dict, List, Tuple, Optional
from enum import Enum


class MoveQuality(Enum):
    """Move quality categories"""
    BRILLIANT = "brilliant"
    GOOD = "good"
    NORMAL = "normal"
    INACCURACY = "inaccuracy"
    MISTAKE = "mistake"
    BLUNDER = "blunder"


class MoveEvaluator:
    """Evaluates chess moves for quality and provides analysis"""
    
    def __init__(self):
        # Piece values for material calculation
        self.piece_values = {
            chess.PAWN: 100,
            chess.KNIGHT: 320,
            chess.BISHOP: 330,
            chess.ROOK: 500,
            chess.QUEEN: 900,
            chess.KING: 20000
        }
        
        # Position tables for piece-square evaluation
        self._init_position_tables()
    
    def _init_position_tables(self):
        """Initialize piece-square tables"""
        # Pawn position table (white perspective)
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
        
        # Knight position table
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
        
        # Bishop position table
        self.bishop_table = [
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
        ]
    
    def evaluate_position(self, board: chess.Board) -> float:
        """Evaluate board position"""
        if board.is_checkmate():
            return -20000 if board.turn else 20000
        
        if board.is_stalemate() or board.is_insufficient_material():
            return 0
        
        score = 0.0
        
        # Material balance
        score += self._evaluate_material(board)
        
        # Position evaluation
        score += self._evaluate_piece_positions(board)
        
        # Mobility
        score += self._evaluate_mobility(board)
        
        # King safety
        score += self._evaluate_king_safety(board)
        
        # Center control
        score += self._evaluate_center_control(board)
        
        return score if board.turn == chess.WHITE else -score
    
    def _evaluate_material(self, board: chess.Board) -> float:
        """Calculate material balance"""
        score = 0
        
        for piece_type in [chess.PAWN, chess.KNIGHT, chess.BISHOP, 
                          chess.ROOK, chess.QUEEN]:
            white_pieces = len(board.pieces(piece_type, chess.WHITE))
            black_pieces = len(board.pieces(piece_type, chess.BLACK))
            
            score += (white_pieces - black_pieces) * self.piece_values[piece_type]
        
        return score
    
    def _evaluate_piece_positions(self, board: chess.Board) -> float:
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
        
        # Bishops
        for square in board.pieces(chess.BISHOP, chess.WHITE):
            score += self.bishop_table[square]
        for square in board.pieces(chess.BISHOP, chess.BLACK):
            score -= self.bishop_table[chess.square_mirror(square)]
        
        return score
    
    def _evaluate_mobility(self, board: chess.Board) -> float:
        """Evaluate piece mobility"""
        white_mobility = len(list(board.legal_moves))
        
        board.push(chess.Move.null())
        black_mobility = len(list(board.legal_moves))
        board.pop()
        
        return (white_mobility - black_mobility) * 10
    
    def _evaluate_king_safety(self, board: chess.Board) -> float:
        """Evaluate king safety"""
        score = 0
        
        # Castling rights bonus
        if board.has_kingside_castling_rights(chess.WHITE):
            score += 30
        if board.has_queenside_castling_rights(chess.WHITE):
            score += 20
        if board.has_kingside_castling_rights(chess.BLACK):
            score -= 30
        if board.has_queenside_castling_rights(chess.BLACK):
            score -= 20
        
        return score
    
    def _evaluate_center_control(self, board: chess.Board) -> float:
        """Evaluate control of center squares"""
        center_squares = [chess.D4, chess.D5, chess.E4, chess.E5]
        score = 0
        
        for square in center_squares:
            piece = board.piece_at(square)
            if piece:
                value = 20 if piece.color == chess.WHITE else -20
                score += value
            
            # Attackers on center
            white_attackers = len(board.attackers(chess.WHITE, square))
            black_attackers = len(board.attackers(chess.BLACK, square))
            score += (white_attackers - black_attackers) * 5
        
        return score
    
    def evaluate_move(self, board: chess.Board, move: chess.Move) -> Dict:
        """Evaluate a specific move"""
        # Get position before move
        eval_before = self.evaluate_position(board)
        
        # Make move
        board.push(move)
        eval_after = self.evaluate_position(board)
        board.pop()
        
        # Calculate score change
        score_change = eval_after - eval_before
        
        # Determine move quality
        quality = self._classify_move_quality(board, move, score_change)
        
        # Generate comment
        comment = self._generate_move_comment(board, move, quality)
        
        return {
            'score': score_change,
            'quality': quality.value,
            'comment': comment,
            'evaluation': eval_after
        }
    
    def _classify_move_quality(self, board: chess.Board, move: chess.Move, 
                               score_change: float) -> MoveQuality:
        """Classify move quality based on evaluation"""
        # Get best move score for comparison
        best_score = self._get_best_move_score(board)
        score_diff = best_score - score_change
        
        # Special case: forcing moves
        board.push(move)
        is_check = board.is_check()
        is_checkmate = board.is_checkmate()
        board.pop()
        
        if is_checkmate:
            return MoveQuality.BRILLIANT
        
        # Classify based on score difference from best
        if score_diff <= 10:
            if is_check or board.is_capture(move):
                return MoveQuality.BRILLIANT
            return MoveQuality.GOOD
        elif score_diff <= 50:
            return MoveQuality.NORMAL
        elif score_diff <= 100:
            return MoveQuality.INACCURACY
        elif score_diff <= 200:
            return MoveQuality.MISTAKE
        else:
            return MoveQuality.BLUNDER
    
    def _get_best_move_score(self, board: chess.Board, depth: int = 1) -> float:
        """Get score of best move (simple 1-ply search)"""
        best_score = float('-inf')
        
        for move in board.legal_moves:
            board.push(move)
            score = -self.evaluate_position(board)
            board.pop()
            
            best_score = max(best_score, score)
        
        return best_score
    
    def _generate_move_comment(self, board: chess.Board, move: chess.Move, 
                               quality: MoveQuality) -> str:
        """Generate descriptive comment for move"""
        comments = {
            MoveQuality.BRILLIANT: "Excellent move!",
            MoveQuality.GOOD: "Good move",
            MoveQuality.NORMAL: "Decent move",
            MoveQuality.INACCURACY: "Slightly inaccurate",
            MoveQuality.MISTAKE: "This is a mistake",
            MoveQuality.BLUNDER: "This is a blunder!"
        }
        
        comment = comments[quality]
        
        # Add specific details
        if board.is_capture(move):
            comment += " (Capture)"
        
        board.push(move)
        if board.is_check():
            comment += " (Check)"
        if board.is_checkmate():
            comment += " (Checkmate!)"
        board.pop()
        
        return comment
    
    def analyze_game_moves(self, board: chess.Board, moves: List[str]) -> List[Dict]:
        """Analyze all moves in a game"""
        analysis = []
        temp_board = board.copy()
        
        for move_uci in moves:
            try:
                move = chess.Move.from_uci(move_uci)
                if move in temp_board.legal_moves:
                    eval_result = self.evaluate_move(temp_board, move)
                    eval_result['move'] = move_uci
                    analysis.append(eval_result)
                    temp_board.push(move)
            except:
                continue
        
        return analysis