package com.erland.chess.view.handlers;

import com.erland.chess.model.Board;
import com.erland.chess.model.pieces.Piece;
import com.erland.chess.utils.PieceImageLoader;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle; // Pastikan import ini ada

public class BoardInteractionHandler {
    private static final int TILE_SIZE = 85;
    private static final double DRAG_THRESHOLD = 5.0;
    
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
        if (!moveExecutor.canPlayerMove()) return;
        
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        if (!isValidSquare(col, row)) return;
        
        dragStartX = e.getX();
        dragStartY = e.getY();
        isDragging = false;
        hasMoved = false;
        
        Piece clickedPiece = board.getPiece(col, row);
        
        // Logika seleksi yang diperbaiki
        if (clickedPiece != null && moveExecutor.isPlayerPiece(clickedPiece)) {
            selectPiece(clickedPiece);
        } else if (selectedPiece != null && (clickedPiece == null || !moveExecutor.isPlayerPiece(clickedPiece))) {
            // Klik pada kotak kosong atau musuh saat sudah ada yang terpilih -> Biarkan handleMouseReleased mengeksekusi move
        }
    }
    
    public void handleMouseDragged(MouseEvent e) {
        if (selectedPiece == null) return;
        
        // Deteksi threshold drag
        if (!isDragging) {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            if (Math.sqrt(dx * dx + dy * dy) > DRAG_THRESHOLD) {
                isDragging = true;
                draggedPiece = selectedPiece;
                // Buat visual tepat di posisi mouse saat ini (centering)
                createDragVisual(e.getX(), e.getY()); 
                // Sembunyikan highlight seleksi agar lebih bersih saat drag
                highlightManager.clearHighlights(); 
            }
        }
        
        if (isDragging && draggedPieceVisual != null) {
            // Update posisi: Tengahkan visual di kursor mouse
            double halfSize = (TILE_SIZE - 10) / 2.0;
            draggedPieceVisual.setLayoutX(e.getX() - halfSize);
            draggedPieceVisual.setLayoutY(e.getY() - halfSize);
            hasMoved = true;
        }
    }
    
    public void handleMouseReleased(MouseEvent e) {
        // Hapus visual drag SEBELUM logika move
        clearDragVisual();
        
        if (selectedPiece == null) return;

        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);
        
        if (isDragging && hasMoved) {
            // Logic Drag-Drop
            if (isValidSquare(col, row)) {
                boolean success = moveExecutor.tryMove(selectedPiece, col, row);
                if (success) {
                    deselectPiece(); // Move sukses, reset seleksi
                } else {
                    // Move gagal (ilegal), kembalikan highlight ke posisi asal
                    highlightManager.updateHighlights(selectedPiece);
                }
            } else {
                // Drop di luar board
                highlightManager.updateHighlights(selectedPiece);
            }
        } else {
            // Logic Click-Click
            if (isValidSquare(col, row)) {
                // Jika klik piece yang sama -> deselect
                if (col == selectedPiece.col && row == selectedPiece.row) {
                    deselectPiece();
                } else {
                    // Coba move
                    boolean success = moveExecutor.tryMove(selectedPiece, col, row);
                    if (success) {
                        deselectPiece();
                    } else {
                        // Jika klik piece teman lain -> ganti seleksi
                        Piece target = board.getPiece(col, row);
                        if (target != null && moveExecutor.isPlayerPiece(target)) {
                            selectPiece(target);
                        }
                    }
                }
            }
        }
        
        // Reset state
        isDragging = false;
        hasMoved = false;
        draggedPiece = null;
    }
    
    private void selectPiece(Piece piece) {
        selectedPiece = piece;
        board.selectedPiece = piece;
        highlightManager.updateHighlights(piece);
        moveExecutor.onPieceSelected(piece);
    }
    
    private void createDragVisual(double mouseX, double mouseY) {
        if (draggedPiece == null) return;
        
        // Ukuran visual sedikit lebih besar saat di-drag untuk efek tactile
        double size = TILE_SIZE; 
        draggedPieceVisual = new Rectangle(0, 0, size, size);
        
        try {
            javafx.scene.image.Image img = PieceImageLoader.getInstance()
                .getImage(draggedPiece.isWhite, draggedPiece.name);
            if (img != null) {
                draggedPieceVisual.setFill(new javafx.scene.paint.ImagePattern(img));
            } else {
                draggedPieceVisual.setFill(Color.GRAY);
            }
        } catch (Exception e) {
            draggedPieceVisual.setFill(Color.GRAY);
        }
        
        // Posisi awal langsung di tengah mouse
        draggedPieceVisual.setLayoutX(mouseX - size / 2.0);
        draggedPieceVisual.setLayoutY(mouseY - size / 2.0);
        
        draggedPieceVisual.setMouseTransparent(true); // Penting agar tidak menghalangi event mouse
        draggedPieceVisual.setOpacity(0.8);
        draggedPieceVisual.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        
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
}