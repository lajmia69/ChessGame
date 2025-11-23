package com.chess.model;

import java.io.Serializable;

public class ChessBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ChessPiece[][] board;
    private PieceColor currentTurn;
    private boolean gameOver;
    private PieceColor winner;
    private int moveCount; // Track moves to ensure fresh serialization
    private boolean whiteInCheck;
    private boolean blackInCheck;

    public ChessBoard() {
        board = new ChessPiece[8][8];
        currentTurn = PieceColor.WHITE;
        gameOver = false;
        moveCount = 0;
        whiteInCheck = false;
        blackInCheck = false;
        initializeBoard();
    }
    
    public int getMoveCount() {
        return moveCount;
    }
    
    public boolean isInCheck(PieceColor color) {
        return color == PieceColor.WHITE ? whiteInCheck : blackInCheck;
    }

    private void initializeBoard() {
        // Black pieces (row 0)
        board[0][0] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
        board[0][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[0][2] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[0][3] = new ChessPiece(PieceType.QUEEN, PieceColor.BLACK);
        board[0][4] = new ChessPiece(PieceType.KING, PieceColor.BLACK);
        board[0][5] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[0][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[0][7] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
        
        // Black pawns (row 1)
        for (int i = 0; i < 8; i++) {
            board[1][i] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
        }

        // White pieces (row 7)
        board[7][0] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        board[7][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[7][2] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[7][3] = new ChessPiece(PieceType.QUEEN, PieceColor.WHITE);
        board[7][4] = new ChessPiece(PieceType.KING, PieceColor.WHITE);
        board[7][5] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[7][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[7][7] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        
        // White pawns (row 6)
        for (int i = 0; i < 8; i++) {
            board[6][i] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
        }
    }

    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        ChessPiece piece = board[fromRow][fromCol];
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        piece.setMoved(true);
        moveCount++; // Increment move counter

        // Pawn promotion to Queen
        if (piece.getType() == PieceType.PAWN) {
            if ((piece.getColor() == PieceColor.WHITE && toRow == 0) ||
                (piece.getColor() == PieceColor.BLACK && toRow == 7)) {
                board[toRow][toCol] = new ChessPiece(PieceType.QUEEN, piece.getColor());
            }
        }

        // Switch turns first
        currentTurn = (currentTurn == PieceColor.WHITE) ? 
                      PieceColor.BLACK : PieceColor.WHITE;

        // Check for check and checkmate AFTER switching turns
        whiteInCheck = isKingInCheck(PieceColor.WHITE);
        blackInCheck = isKingInCheck(PieceColor.BLACK);
        
        // Check for checkmate
        if (isInCheck(currentTurn) && isCheckmate(currentTurn)) {
            gameOver = true;
            winner = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }

        return true;
    }

    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Check boundaries
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
            toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }

        ChessPiece piece = board[fromRow][fromCol];
        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        ChessPiece target = board[toRow][toCol];
        if (target != null && target.getColor() == piece.getColor()) {
            return false;
        }

        // Check piece-specific movement rules
        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(fromRow, fromCol, toRow, toCol);
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING:
                return isValidKingMove(fromRow, fromCol, toRow, toCol);
        }
        return false;
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece pawn = board[fromRow][fromCol];
        int direction = (pawn.getColor() == PieceColor.WHITE) ? -1 : 1;
        
        // Forward move (one square)
        if (fromCol == toCol && board[toRow][toCol] == null) {
            if (toRow == fromRow + direction) {
                return true;
            }
            // Forward move (two squares from starting position)
            if (!pawn.hasMoved() && toRow == fromRow + 2 * direction && 
                board[fromRow + direction][fromCol] == null) {
                return true;
            }
        }
        
        // Diagonal capture
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            return board[toRow][toCol] != null;
        }
        return false;
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) {
            return false;
        }
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        return isValidRookMove(fromRow, fromCol, toRow, toCol) || 
               isValidBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDir = Integer.compare(toRow, fromRow);
        int colDir = Integer.compare(toCol, fromCol);
        int row = fromRow + rowDir;
        int col = fromCol + colDir;

        while (row != toRow || col != toCol) {
            if (board[row][col] != null) {
                return false;
            }
            row += rowDir;
            col += colDir;
        }
        return true;
    }

    private boolean isCheckmate(PieceColor color) {
        // King must be in check
        if (!isKingInCheck(color)) {
            return false;
        }
        
        // Try all possible moves for this color to see if any can escape check
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = board[fromRow][fromCol];
                if (piece != null && piece.getColor() == color) {
                    // Try all possible destination squares
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (wouldMoveEscapeCheck(fromRow, fromCol, toRow, toCol, color)) {
                                return false; // Found a move that escapes check
                            }
                        }
                    }
                }
            }
        }
        return true; // No moves can escape check = checkmate
    }
    
    private boolean wouldMoveEscapeCheck(int fromRow, int fromCol, int toRow, int toCol, PieceColor color) {
        // Temporarily save current turn
        PieceColor savedTurn = currentTurn;
        currentTurn = color;
        
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
            currentTurn = savedTurn;
            return false;
        }
        
        // Simulate the move
        ChessPiece movingPiece = board[fromRow][fromCol];
        ChessPiece capturedPiece = board[toRow][toCol];
        
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        
        // Check if king is still in check after this move
        boolean stillInCheck = isKingInCheck(color);
        
        // Undo the move
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;
        currentTurn = savedTurn;
        
        return !stillInCheck;
    }
    
    private boolean isKingInCheck(PieceColor kingColor) {
        // Find the king
        int[] kingPos = findKing(kingColor);
        if (kingPos == null) {
            return false; // King not found (captured)
        }
        
        int kingRow = kingPos[0];
        int kingCol = kingPos[1];
        
        // Check if any opponent piece can attack the king
        PieceColor opponentColor = (kingColor == PieceColor.WHITE) ? 
                                    PieceColor.BLACK : PieceColor.WHITE;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.getColor() == opponentColor) {
                    // Temporarily change turn to check if opponent can attack king
                    PieceColor savedTurn = currentTurn;
                    currentTurn = opponentColor;
                    
                    if (isValidMoveIgnoringCheck(row, col, kingRow, kingCol)) {
                        currentTurn = savedTurn;
                        return true; // King is in check
                    }
                    
                    currentTurn = savedTurn;
                }
            }
        }
        
        return false;
    }
    
    private boolean isValidMoveIgnoringCheck(int fromRow, int fromCol, int toRow, int toCol) {
        // Similar to isValidMove but doesn't check turn
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
            toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }

        ChessPiece piece = board[fromRow][fromCol];
        if (piece == null) {
            return false;
        }

        // Don't check if target has same color (we're checking if king can be attacked)
        
        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(fromRow, fromCol, toRow, toCol);
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING:
                return isValidKingMove(fromRow, fromCol, toRow, toCol);
        }
        return false;
    }

    private int[] findKing(PieceColor color) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board[i][j];
                if (piece != null && piece.getType() == PieceType.KING && 
                    piece.getColor() == color) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    public ChessPiece getPiece(int row, int col) {
        return board[row][col];
    }

    public PieceColor getCurrentTurn() { 
        return currentTurn; 
    }
    
    public boolean isGameOver() { 
        return gameOver; 
    }
    
    public PieceColor getWinner() { 
        return winner; 
    }
}