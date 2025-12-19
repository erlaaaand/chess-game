#!/usr/bin/env python3
"""
Learning Agent - Implements reinforcement learning for AI improvement
Path: ai_engine/learning_agent.py
"""

import chess
import numpy as np
from typing import List, Dict, Tuple, Optional
from collections import defaultdict


class GameExperience:
    """Represents a single game experience for learning"""
    
    def __init__(self):
        self.positions: List[str] = []  # FEN positions
        self.moves: List[str] = []  # UCI moves
        self.evaluations: List[float] = []
        self.result: Optional[str] = None  # 'win', 'draw', 'loss'
    
    def add_position(self, fen: str, move: str, evaluation: float):
        """Add position to experience"""
        self.positions.append(fen)
        self.moves.append(move)
        self.evaluations.append(evaluation)
    
    def set_result(self, result: str):
        """Set game result"""
        self.result = result


class LearningAgent:
    """Reinforcement learning agent for chess AI"""
    
    def __init__(self, learning_rate: float = 0.01, discount_factor: float = 0.95):
        self.learning_rate = learning_rate
        self.discount_factor = discount_factor
        
        # Q-values: position -> move -> value
        self.q_values: Dict[str, Dict[str, float]] = defaultdict(lambda: defaultdict(float))
        
        # Move statistics
        self.move_counts: Dict[str, int] = defaultdict(int)
        self.move_successes: Dict[str, int] = defaultdict(int)
    
    def get_move_value(self, position_fen: str, move_uci: str) -> float:
        """Get Q-value for position-move pair"""
        return self.q_values[position_fen][move_uci]
    
    def update_move_value(self, position_fen: str, move_uci: str, reward: float):
        """Update Q-value using TD learning"""
        old_value = self.q_values[position_fen][move_uci]
        self.q_values[position_fen][move_uci] = old_value + self.learning_rate * (reward - old_value)
    
    def learn_from_game(self, experience: GameExperience):
        """Learn from complete game experience"""
        if not experience.result:
            return
        
        # Calculate rewards based on result
        final_reward = {
            'win': 1.0,
            'draw': 0.0,
            'loss': -1.0
        }.get(experience.result, 0.0)
        
        # Backward propagation of rewards
        rewards = []
        cumulative_reward = final_reward
        
        for i in range(len(experience.positions) - 1, -1, -1):
            rewards.insert(0, cumulative_reward)
            cumulative_reward *= self.discount_factor
        
        # Update Q-values
        for pos, move, reward in zip(experience.positions, experience.moves, rewards):
            self.update_move_value(pos, move, reward)
            
            # Update statistics
            self.move_counts[move] += 1
            if reward > 0:
                self.move_successes[move] += 1
    
    def get_move_probability(self, position_fen: str, legal_moves: List[str]) -> Dict[str, float]:
        """Get probability distribution over legal moves"""
        if not legal_moves:
            return {}
        
        # Get Q-values for all legal moves
        q_vals = [self.get_move_value(position_fen, move) for move in legal_moves]
        
        # Softmax to get probabilities
        exp_vals = np.exp(np.array(q_vals) - np.max(q_vals))
        probabilities = exp_vals / exp_vals.sum()
        
        return dict(zip(legal_moves, probabilities))
    
    def select_move(self, position_fen: str, legal_moves: List[str], 
                   exploration_rate: float = 0.1) -> str:
        """Select move using epsilon-greedy policy"""
        if not legal_moves:
            return None
        
        # Exploration: random move
        if np.random.random() < exploration_rate:
            return np.random.choice(legal_moves)
        
        # Exploitation: best Q-value
        move_values = [(move, self.get_move_value(position_fen, move)) 
                      for move in legal_moves]
        
        return max(move_values, key=lambda x: x[1])[0]
    
    def get_move_statistics(self, move_uci: str) -> Tuple[int, float]:
        """Get statistics for a specific move"""
        count = self.move_counts[move_uci]
        if count == 0:
            return 0, 0.0
        
        success_rate = self.move_successes[move_uci] / count
        return count, success_rate
    
    def analyze_opening(self, opening_moves: List[str]) -> Dict[str, float]:
        """Analyze success rate of opening sequence"""
        if not opening_moves:
            return {}
        
        board = chess.Board()
        position_moves = []
        
        for move_uci in opening_moves:
            try:
                move = chess.Move.from_uci(move_uci)
                if move in board.legal_moves:
                    position_moves.append((board.fen(), move_uci))
                    board.push(move)
                else:
                    break
            except:
                break
        
        # Get Q-values for opening positions
        analysis = {}
        for i, (fen, move) in enumerate(position_moves):
            analysis[f"Move {i+1} ({move})"] = self.get_move_value(fen, move)
        
        return analysis
    
    def get_best_responses(self, position_fen: str, top_n: int = 5) -> List[Tuple[str, float]]:
        """Get top N best moves for position"""
        if position_fen not in self.q_values:
            return []
        
        move_values = list(self.q_values[position_fen].items())
        move_values.sort(key=lambda x: x[1], reverse=True)
        
        return move_values[:top_n]
    
    def save_knowledge(self, filepath: str):
        """Save learned knowledge to file"""
        import pickle
        
        data = {
            'q_values': dict(self.q_values),
            'move_counts': dict(self.move_counts),
            'move_successes': dict(self.move_successes),
            'learning_rate': self.learning_rate,
            'discount_factor': self.discount_factor
        }
        
        with open(filepath, 'wb') as f:
            pickle.dump(data, f)
    
    def load_knowledge(self, filepath: str):
        """Load learned knowledge from file"""
        import pickle
        
        try:
            with open(filepath, 'rb') as f:
                data = pickle.load(f)
            
            self.q_values = defaultdict(lambda: defaultdict(float), data['q_values'])
            self.move_counts = defaultdict(int, data['move_counts'])
            self.move_successes = defaultdict(int, data['move_successes'])
            self.learning_rate = data.get('learning_rate', self.learning_rate)
            self.discount_factor = data.get('discount_factor', self.discount_factor)
            
            return True
        except Exception as e:
            print(f"Error loading knowledge: {e}")
            return False
    
    def get_statistics_summary(self) -> Dict:
        """Get summary of learning statistics"""
        total_positions = len(self.q_values)
        total_moves = len(self.move_counts)
        
        avg_success_rate = 0.0
        if total_moves > 0:
            success_rates = [
                self.move_successes[move] / self.move_counts[move]
                for move in self.move_counts if self.move_counts[move] > 0
            ]
            avg_success_rate = np.mean(success_rates) if success_rates else 0.0
        
        return {
            'total_positions_learned': total_positions,
            'total_moves_tracked': total_moves,
            'average_success_rate': avg_success_rate,
            'most_played_moves': sorted(
                self.move_counts.items(),
                key=lambda x: x[1],
                reverse=True
            )[:10]
        }