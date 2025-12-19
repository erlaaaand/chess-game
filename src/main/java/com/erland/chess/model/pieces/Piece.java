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
    protected Board board; // Tambahan penting!
    private BufferedImage image;

    public Piece(Board board) {
        this.board = board;
    }

    // Load gambar otomatis sesuai nama dan warna
    public void loadImage() {
        try {
            String path = "/images/" + (isWhite ? "w_" : "b_") + name.toLowerCase() + ".png";
            image = ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2, int panelSize) {
        int tileSize = panelSize / 8;
        g2.drawImage(image, col * tileSize, row * tileSize, tileSize, tileSize, null);
    }

    public boolean isWhite() { return isWhite; }

    // Validasi dasar (batas papan & tidak makan teman)
    public boolean canMove(int targetCol, int targetRow) {
        if(targetCol < 0 || targetCol > 7 || targetRow < 0 || targetRow > 7) return false;
        Piece target = board.getPiece(targetCol, targetRow);
        if(target != null && target.isWhite == this.isWhite) return false; // Tabrak teman
        return isValidMovement(targetCol, targetRow);
    }

    public abstract boolean isValidMovement(int newCol, int newRow);
}