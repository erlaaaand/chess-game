package com.erland.chess.view.renderers;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.*;
import com.erland.chess.utils.PieceImageLoader; // Import loader

public class BoardRenderer {
    private static final int TILE_SIZE = 85;
    private static final int BOARD_SIZE = TILE_SIZE * 8;
    
    private final Canvas boardCanvas;
    private final Canvas pieceCanvas;
    private final Board board;
    
    // Hapus pieceImages Map
    private Piece draggedPiece = null;
    
    public BoardRenderer(Canvas boardCanvas, Canvas pieceCanvas, Board board) {
        this.boardCanvas = boardCanvas;
        this.pieceCanvas = pieceCanvas;
        this.board = board;
        
        // Hapus loadPieceImages() karena sudah ditangani PieceImageLoader
        drawBoard();
    }
    
    public void drawBoard() {
        GraphicsContext gc = boardCanvas.getGraphicsContext2D();
        
        // Draw tiles
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 0) {
                    gc.setFill(Color.web("#f0d9b5"));
                } else {
                    gc.setFill(Color.web("#b58863"));
                }
                gc.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        
        // Draw border
        gc.setStroke(Color.web("#8B4513"));
        gc.setLineWidth(3);
        gc.strokeRect(0, 0, BOARD_SIZE, BOARD_SIZE);
    }
    
    public void drawPieces() {
        GraphicsContext gc = pieceCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, BOARD_SIZE, BOARD_SIZE);
        
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                Piece piece = board.getPiece(col, row);
                
                // Don't draw dragged piece (it's on drag layer)
                if (piece != null && piece != draggedPiece) {
                    drawPiece(gc, piece, col, row);
                }
            }
        }
    }
    
    private void drawPiece(GraphicsContext gc, Piece piece, int col, int row) {
        // Gunakan PieceImageLoader untuk mendapatkan gambar
        Image img = PieceImageLoader.getInstance().getImage(piece.isWhite, piece.name);
        
        if (img != null) {
            double x = col * TILE_SIZE + 5;
            double y = row * TILE_SIZE + 5;
            gc.drawImage(img, x, y);
        } else {
            // Fallback: draw text symbol
            drawPieceSymbol(gc, piece, col, row);
        }
    }
    
    private void drawPieceSymbol(GraphicsContext gc, Piece piece, int col, int row) {
        gc.setFill(piece.isWhite ? Color.WHITE : Color.BLACK);
        gc.setStroke(piece.isWhite ? Color.BLACK : Color.WHITE);
        gc.setLineWidth(2);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        
        String symbol = getPieceSymbol(piece);
        double x = col * TILE_SIZE + 20;
        double y = row * TILE_SIZE + 60;
        
        gc.strokeText(symbol, x, y);
        gc.fillText(symbol, x, y);
    }
    
    private String getPieceSymbol(Piece piece) {
        String symbol = "";
        
        if (piece instanceof King) symbol = "♔";
        else if (piece instanceof Queen) symbol = "♕";
        else if (piece instanceof Rook) symbol = "♖";
        else if (piece instanceof Bishop) symbol = "♗";
        else if (piece instanceof Knight) symbol = "♘";
        else if (piece instanceof Pawn) symbol = "♙";
        
        return piece.isWhite ? symbol : symbol.toLowerCase();
    }
    
    public void setDraggedPiece(Piece piece) {
        this.draggedPiece = piece;
    }
    
    // Hapus getPieceImages() karena sudah tidak relevan
    
    public static int getTileSize() {
        return TILE_SIZE;
    }
    
    public static int getBoardSize() {
        return BOARD_SIZE;
    }
}