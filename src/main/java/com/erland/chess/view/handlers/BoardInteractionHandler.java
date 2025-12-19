package com.erland.chess.view.handlers;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.Piece;

/**
 * Handles all mouse interactions with the chess board
 * Supports both click-to-move and drag-and-drop
 */
public class BoardInteractionHandler {
    private static final int TILE_SIZE = 85;
    private static final double DRAG_THRESHOLD = 5.0; // pixels
    
    private final Board board;
    private final Pane dragLayer;
    private final MoveExecutor moveExecutor;
    private final HighlightManager highlightManager;
    
    // Interaction state
    private Piece selectedPiece = null;
    private Piece draggedPiece = null;
    private Rectangle draggedPieceVisual = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private boolean isDragging = false;
    private boolean hasMoved = false;
    
    public BoardInteractionHandler(Board board, Pane dragLayer, 
                                  MoveExecutor moveExecutor,
                                  HighlightManager highlightManager) {
        this.board = board;
        this.dragLayer = dragLayer;
        this.moveExecutor = moveExecutor;
        this.highlightManager = highlightManager;
    }
    
    public void handleMousePressed(MouseEvent e) {
        if (!moveExecutor.canPlayerMove()) {
            return;
        }
        
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        if (!isValidSquare(col, row)) {
            return;
        }
        
        // Store initial position for drag detection
        dragStartX = e.getX();
        dragStartY = e.getY();
        isDragging = false;
        hasMoved = false;
        
        Piece clickedPiece = board.getPiece(col, row);
        
        // Handle piece selection
        if (selectedPiece == null) {
            // Select new piece
            if (clickedPiece != null && moveExecutor.isPlayerPiece(clickedPiece)) {
                selectPiece(clickedPiece, e.getX(), e.getY());
            }
        } else {
            // Already have selection
            if (clickedPiece != null && moveExecutor.isPlayerPiece(clickedPiece)) {
                // Clicking another own piece - switch selection
                selectPiece(clickedPiece, e.getX(), e.getY());
            } else {
                // Clicking empty square or enemy piece - try to move (will be executed on release)
                // Don't deselect yet, wait for mouse release
            }
        }
    }
    
    public void handleMouseDragged(MouseEvent e) {
        if (selectedPiece == null) {
            return;
        }
        
        // Check if mouse has moved enough to start drag
        double dx = e.getX() - dragStartX;
        double dy = e.getY() - dragStartY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (!isDragging && distance > DRAG_THRESHOLD) {
            // Start dragging
            isDragging = true;
            draggedPiece = selectedPiece;
            dragOffsetX = dragStartX - (selectedPiece.col * TILE_SIZE);
            dragOffsetY = dragStartY - (selectedPiece.row * TILE_SIZE);
            createDragVisual(e.getX() - dragOffsetX, e.getY() - dragOffsetY);
        }
        
        if (isDragging && draggedPieceVisual != null) {
            // Update drag position
            draggedPieceVisual.setLayoutX(e.getX() - dragOffsetX);
            draggedPieceVisual.setLayoutY(e.getY() - dragOffsetY);
            hasMoved = true;
        }
    }
    
    public void handleMouseReleased(MouseEvent e) {
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        // Clean up drag visual
        clearDragVisual();
        
        if (selectedPiece == null) {
            return;
        }
        
        // Determine if this was a click or drag
        if (isDragging && hasMoved) {
            // Drag and drop
            handleDragDrop(col, row);
        } else {
            // Click to move
            handleClickMove(col, row);
        }
        
        // Reset drag state
        isDragging = false;
        hasMoved = false;
        draggedPiece = null;
    }
    
    private void selectPiece(Piece piece, double mouseX, double mouseY) {
        selectedPiece = piece;
        board.selectedPiece = piece;
        
        // Calculate offset for potential drag
        dragOffsetX = mouseX - (piece.col * TILE_SIZE);
        dragOffsetY = mouseY - (piece.row * TILE_SIZE);
        
        highlightManager.updateHighlights(piece);
        moveExecutor.onPieceSelected(piece);
    }
    
    private void handleDragDrop(int targetCol, int targetRow) {
        if (!isValidSquare(targetCol, targetRow)) {
            // Dropped outside board - cancel
            deselectPiece();
            return;
        }
        
        // Try to execute move
        if (moveExecutor.tryMove(selectedPiece, targetCol, targetRow)) {
            // Move successful
            selectedPiece = null;
            board.selectedPiece = null;
            highlightManager.clearHighlights();
        } else {
            // Move failed - keep selection
            highlightManager.updateHighlights(selectedPiece);
        }
    }
    
    private void handleClickMove(int targetCol, int targetRow) {
        if (!isValidSquare(targetCol, targetRow)) {
            return;
        }
        
        // If clicking same square, deselect
        if (targetCol == selectedPiece.col && targetRow == selectedPiece.row) {
            deselectPiece();
            return;
        }
        
        // Try to execute move
        if (moveExecutor.tryMove(selectedPiece, targetCol, targetRow)) {
            // Move successful
            selectedPiece = null;
            board.selectedPiece = null;
            highlightManager.clearHighlights();
        } else {
            // Move failed - keep selection (unless clicking empty square)
            Piece targetPiece = board.getPiece(targetCol, targetRow);
            if (targetPiece == null) {
                deselectPiece();
            }
        }
    }
    
    private void createDragVisual(double x, double y) {
        if (draggedPiece == null) return;
        
        draggedPieceVisual = new Rectangle(x, y, TILE_SIZE - 10, TILE_SIZE - 10);
        
        // Get piece image
        String color = draggedPiece.isWhite ? "w" : "b";
        String pieceName = draggedPiece.name.toLowerCase();
        
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/images/" + color + "_" + pieceName + ".png"),
                TILE_SIZE - 10, TILE_SIZE - 10, true, true
            );
            draggedPieceVisual.setFill(new javafx.scene.paint.ImagePattern(img));
        } catch (Exception e) {
            // Fallback to colored rectangle
            draggedPieceVisual.setFill(draggedPiece.isWhite ? Color.WHITE : Color.BLACK);
        }
        
        draggedPieceVisual.setEffect(new javafx.scene.effect.DropShadow(15, Color.BLACK));
        draggedPieceVisual.setOpacity(0.9);
        dragLayer.getChildren().add(draggedPieceVisual);
    }
    
    private void clearDragVisual() {
        dragLayer.getChildren().clear();
        draggedPieceVisual = null;
    }
    
    private void deselectPiece() {
        selectedPiece = null;
        board.selectedPiece = null;
        highlightManager.clearHighlights();
        moveExecutor.onPieceDeselected();
    }
    
    private boolean isValidSquare(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }
    
    public void reset() {
        clearDragVisual();
        deselectPiece();
        isDragging = false;
        hasMoved = false;
        draggedPiece = null;
    }
    
    public Piece getSelectedPiece() {
        return selectedPiece;
    }
    
    public Piece getDraggedPiece() {
        return draggedPiece;
    }
    
    public boolean isDragging() {
        return isDragging;
    }
}