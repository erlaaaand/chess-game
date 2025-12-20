package com.erland.chess.model.pieces;
import com.erland.chess.model.Board;

public class King extends Piece {
    public King(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col; this.row = row; this.isWhite = isWhite;
        this.name = "King";
        // Removed loadImage();
    }
    
    public boolean isValidMovement(int newCol, int newRow) {
        // Normal king movement (one square in any direction)
        if (Math.abs(newCol - col) <= 1 && Math.abs(newRow - row) <= 1) {
            return true;
        }
        
        // Castling
        if (!hasMoved && newRow == row && Math.abs(newCol - col) == 2) {
            return canCastle(newCol);
        }
        
        return false;
    }
    
    private boolean canCastle(int targetCol) {
        // Can't castle if king has moved or is in check
        if (hasMoved || board.isKingInCheck(isWhite)) {
            return false;
        }
        
        int rookCol;
        int direction;
        
        if (targetCol == 6) {
            // Kingside castling (short)
            rookCol = 7;
            direction = 1;
        } else if (targetCol == 2) {
            // Queenside castling (long)
            rookCol = 0;
            direction = -1;
        } else {
            return false;
        }
        
        // Check if rook exists and hasn't moved
        Piece rook = board.getPiece(rookCol, row);
        if (rook == null || !(rook instanceof Rook) || rook.hasMoved || rook.isWhite != isWhite) {
            return false;
        }
        
        // Check if squares between king and rook are empty
        int start = Math.min(col, rookCol);
        int end = Math.max(col, rookCol);
        for (int c = start + 1; c < end; c++) {
            if (board.getPiece(c, row) != null) {
                return false;
            }
        }
        
        // Check if king passes through or lands on attacked square
        int checkCol = col;
        while (checkCol != targetCol) {
            checkCol += direction;
            
            // Temporarily move king to check if square is under attack
            int oldCol = col;
            board.pieceList[oldCol][row] = null;
            board.pieceList[checkCol][row] = this;
            col = checkCol;
            
            boolean underAttack = board.isKingInCheck(isWhite);
            
            // Restore position
            board.pieceList[checkCol][row] = null;
            board.pieceList[oldCol][row] = this;
            col = oldCol;
            
            if (underAttack) {
                return false;
            }
        }
        
        return true;
    }
}