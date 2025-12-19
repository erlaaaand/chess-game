package com.erland.chess.model.pieces;

import com.erland.chess.model.Board;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

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
            var stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                throw new IOException("Image not found: " + path);
            }
            image = ImageIO.read(stream);
        } catch (IOException e) {
            System.err.println("Failed to load image: " + path);
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2, int panelSize) {
        if(image != null) {
            int tileSize = panelSize / 8;
            g2.drawImage(image, col * tileSize, row * tileSize, tileSize, tileSize, null);
        }
    }

    public boolean isWhite() { return isWhite; }

    public boolean canMove(int targetCol, int targetRow) {
        if(targetCol < 0 || targetCol > 7 || targetRow < 0 || targetRow > 7) return false;
        if(targetCol == col && targetRow == row) return false;
        
        Piece target = board.getPiece(targetCol, targetRow);
        if(target != null && target.isWhite == this.isWhite) return false;
        
        return isValidMovement(targetCol, targetRow);
    }

    public abstract boolean isValidMovement(int newCol, int newRow);
}