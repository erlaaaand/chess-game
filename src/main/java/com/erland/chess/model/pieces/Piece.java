package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

public abstract class Piece {
    public int col, row;
    public boolean isWhite;
    public String name;
    public boolean hasMoved = false;
    protected Board board;
    private BufferedImage image;

    public Piece(Board board) {
        this.board = board;
    }

    public void loadImage() {
        String path = "/images/" + (isWhite ? "w_" : "b_") + name.toLowerCase() + ".png";
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Image not found: " + path);
                System.err.println("Note: Place chess piece images in src/main/resources/images/");
                // Create a placeholder colored rectangle instead of failing
                image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            } else {
                image = ImageIO.read(stream);
                stream.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to load image: " + path);
            e.printStackTrace();
            // Create placeholder
            image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public void draw(Graphics2D g2, int panelSize) {
        if(image != null) {
            int tileSize = panelSize / 8;
            int x = col * tileSize;
            int y = row * tileSize;
            g2.drawImage(image, x, y, tileSize, tileSize, null);
        }
    }

    public boolean isWhite() { 
        return isWhite; 
    }

    public boolean canMove(int targetCol, int targetRow) {
        // Boundary check
        if(targetCol < 0 || targetCol > 7 || targetRow < 0 || targetRow > 7) {
            return false;
        }
        
        // Can't move to same position
        if(targetCol == col && targetRow == row) {
            return false;
        }
        
        // Can't capture own piece
        Piece target = board.getPiece(targetCol, targetRow);
        if(target != null && target.isWhite == this.isWhite) {
            return false;
        }
        
        // Check piece-specific movement rules
        return isValidMovement(targetCol, targetRow);
    }

    public abstract boolean isValidMovement(int newCol, int newRow);
}