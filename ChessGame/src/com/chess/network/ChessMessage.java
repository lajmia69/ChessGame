package com.chess.network;

import com.chess.model.ChessBoard;
import com.chess.model.PieceColor;
import java.io.Serializable;

public class ChessMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MessageType type;
    private int fromRow, fromCol, toRow, toCol;
    private ChessBoard board;
    private PieceColor playerColor;
    private String chatMessage;
    private PieceColor winner;

    public ChessMessage(MessageType type) {
        this.type = type;
    }

    public static ChessMessage createMoveMessage(int fromRow, int fromCol, 
                                                  int toRow, int toCol) {
        ChessMessage msg = new ChessMessage(MessageType.MOVE);
        msg.fromRow = fromRow;
        msg.fromCol = fromCol;
        msg.toRow = toRow;
        msg.toCol = toCol;
        return msg;
    }

    public static ChessMessage createBoardUpdate(ChessBoard board) {
        ChessMessage msg = new ChessMessage(MessageType.BOARD_UPDATE);
        msg.board = board;
        return msg;
    }

    public static ChessMessage createPlayerAssignment(PieceColor color) {
        ChessMessage msg = new ChessMessage(MessageType.PLAYER_ASSIGNED);
        msg.playerColor = color;
        return msg;
    }

    public static ChessMessage createGameOver(PieceColor winner) {
        ChessMessage msg = new ChessMessage(MessageType.GAME_OVER);
        msg.winner = winner;
        return msg;
    }
    
    public static ChessMessage createCheckNotification(PieceColor colorInCheck) {
        ChessMessage msg = new ChessMessage(MessageType.CHECK_NOTIFICATION);
        msg.playerColor = colorInCheck;
        return msg;
    }

    // Getters
    public MessageType getType() { 
        return type; 
    }
    
    public int getFromRow() { 
        return fromRow; 
    }
    
    public int getFromCol() { 
        return fromCol; 
    }
    
    public int getToRow() { 
        return toRow; 
    }
    
    public int getToCol() { 
        return toCol; 
    }
    
    public ChessBoard getBoard() { 
        return board; 
    }
    
    public PieceColor getPlayerColor() { 
        return playerColor; 
    }
    
    public PieceColor getWinner() { 
        return winner; 
    }
    
    public String getChatMessage() { 
        return chatMessage; 
    }
}
