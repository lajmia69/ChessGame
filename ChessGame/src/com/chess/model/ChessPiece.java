package com.chess.model;

import java.io.Serializable;

public class ChessPiece implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private PieceType type;
    private PieceColor color;
    private boolean hasMoved;

    public ChessPiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public PieceType getType() { 
        return type; 
    }
    
    public PieceColor getColor() { 
        return color; 
    }
    
    public boolean hasMoved() { 
        return hasMoved; 
    }
    
    public void setMoved(boolean moved) { 
        this.hasMoved = moved; 
    }

    public String getSymbol() {
        String symbol = "";
        switch (type) {
            case KING: 
                symbol = "♔"; 
                break;
            case QUEEN: 
                symbol = "♕"; 
                break;
            case ROOK: 
                symbol = "♖"; 
                break;
            case BISHOP: 
                symbol = "♗"; 
                break;
            case KNIGHT: 
                symbol = "♘"; 
                break;
            case PAWN: 
                symbol = "♙"; 
                break;
        }
        
        if (color == PieceColor.BLACK) {
            switch (type) {
                case KING: return "♚";
                case QUEEN: return "♛";
                case ROOK: return "♜";
                case BISHOP: return "♝";
                case KNIGHT: return "♞";
                case PAWN: return "♟";
            }
        }
        return symbol;
    }
}