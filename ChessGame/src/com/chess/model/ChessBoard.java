package com.chess.model;

import java.io.Serializable;

public class ChessBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ChessPiece[][] board;
    private PieceColor currentTurn;
    private boolean gameOver;
    private PieceColor winner;
    private int moveCount;
    private boolean whiteInCheck;
    private boolean blackInCheck;
    
    private int enPassantTargetCol = -1;
    private PieceColor enPassantTargetColor = null;
    
    private boolean whiteKingMoved = false;
    private boolean whiteRookLeftMoved = false;
    private boolean whiteRookRightMoved = false;
    private boolean blackKingMoved = false;
    private boolean blackRookLeftMoved = false;
    private boolean blackRookRightMoved = false;

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
        board[0][0] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
        board[0][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[0][2] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[0][3] = new ChessPiece(PieceType.QUEEN, PieceColor.BLACK);
        board[0][4] = new ChessPiece(PieceType.KING, PieceColor.BLACK);
        board[0][5] = new ChessPiece(PieceType.BISHOP, PieceColor.BLACK);
        board[0][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.BLACK);
        board[0][7] = new ChessPiece(PieceType.ROOK, PieceColor.BLACK);
        
        for (int i = 0; i < 8; i++) {
            board[1][i] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
        }

        board[7][0] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        board[7][1] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[7][2] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[7][3] = new ChessPiece(PieceType.QUEEN, PieceColor.WHITE);
        board[7][4] = new ChessPiece(PieceType.KING, PieceColor.WHITE);
        board[7][5] = new ChessPiece(PieceType.BISHOP, PieceColor.WHITE);
        board[7][6] = new ChessPiece(PieceType.KNIGHT, PieceColor.WHITE);
        board[7][7] = new ChessPiece(PieceType.ROOK, PieceColor.WHITE);
        
        for (int i = 0; i < 8; i++) {
            board[6][i] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
        }
    }

    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        return makeMove(fromRow, fromCol, toRow, toCol, null);
    }

    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol, PieceType promotionType) {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        ChessPiece piece = board[fromRow][fromCol];
        ChessPiece capturedPiece = board[toRow][toCol];
        
        enPassantTargetCol = -1;
        enPassantTargetColor = null;
        
        // Handle en passant capture
        if (piece.getType() == PieceType.PAWN && fromCol != toCol && capturedPiece == null) {
            int capturedPawnRow = (piece.getColor() == PieceColor.WHITE) ? toRow + 1 : toRow - 1;
            board[capturedPawnRow][toCol] = null;
            System.out.println("[BOARD] En passant capture! Removed pawn at row " + capturedPawnRow);
        }
        
        // Handle castling
        if (piece.getType() == PieceType.KING && Math.abs(fromCol - toCol) == 2) {
            if (toCol == 6) {
                ChessPiece rook = board[fromRow][7];
                board[fromRow][5] = rook;
                board[fromRow][7] = null;
                rook.setMoved(true);
                System.out.println("[BOARD] Kingside castling performed");
            } else if (toCol == 2) {
                ChessPiece rook = board[fromRow][0];
                board[fromRow][3] = rook;
                board[fromRow][0] = null;
                rook.setMoved(true);
                System.out.println("[BOARD] Queenside castling performed");
            }
        }
        
        // Track castling rights
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == PieceColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }
        if (piece.getType() == PieceType.ROOK) {
            if (piece.getColor() == PieceColor.WHITE) {
                if (fromCol == 0) whiteRookLeftMoved = true;
                if (fromCol == 7) whiteRookRightMoved = true;
            } else {
                if (fromCol == 0) blackRookLeftMoved = true;
                if (fromCol == 7) blackRookRightMoved = true;
            }
        }

        // Make the move
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        piece.setMoved(true);
        moveCount++;

        // Set en passant opportunity if pawn moved two squares
        if (piece.getType() == PieceType.PAWN && Math.abs(fromRow - toRow) == 2) {
            enPassantTargetCol = fromCol;
            enPassantTargetColor = piece.getColor();
            System.out.println("[BOARD] En passant opportunity created for " + piece.getColor() + 
                             " pawn at column " + enPassantTargetCol);
        }

        // Pawn promotion with choice
        if (piece.getType() == PieceType.PAWN) {
            if ((piece.getColor() == PieceColor.WHITE && toRow == 0) ||
                (piece.getColor() == PieceColor.BLACK && toRow == 7)) {
                
                // Use specified promotion type, default to QUEEN if not specified
                PieceType newType = (promotionType != null) ? promotionType : PieceType.QUEEN;
                board[toRow][toCol] = new ChessPiece(newType, piece.getColor());
                board[toRow][toCol].setMoved(true);
                System.out.println("[BOARD] Pawn promoted to " + newType + "!");
            }
        }

        // Switch turns
        currentTurn = (currentTurn == PieceColor.WHITE) ? 
                      PieceColor.BLACK : PieceColor.WHITE;

        // Check for check and checkmate
        whiteInCheck = isKingInCheck(PieceColor.WHITE);
        blackInCheck = isKingInCheck(PieceColor.BLACK);
        
        if (isInCheck(currentTurn) && isCheckmate(currentTurn)) {
            gameOver = true;
            winner = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }

        return true;
    }

    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
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

        if (isInCheck(currentTurn)) {
            if (!wouldMoveEscapeCheck(fromRow, fromCol, toRow, toCol, currentTurn)) {
                return false;
            }
        } else {
            if (!isMoveSafeForKing(fromRow, fromCol, toRow, toCol, currentTurn)) {
                return false;
            }
        }

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
    
    private boolean isMoveSafeForKing(int fromRow, int fromCol, int toRow, int toCol, PieceColor color) {
        ChessPiece movingPiece = board[fromRow][fromCol];
        ChessPiece capturedPiece = board[toRow][toCol];
        
        ChessPiece enPassantCaptured = null;
        if (movingPiece.getType() == PieceType.PAWN && fromCol != toCol && capturedPiece == null) {
            int capturedPawnRow = (movingPiece.getColor() == PieceColor.WHITE) ? toRow + 1 : toRow - 1;
            enPassantCaptured = board[capturedPawnRow][toCol];
            board[capturedPawnRow][toCol] = null;
        }
        
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        
        boolean kingIsSafe = !isKingInCheck(color);
        
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;
        
        if (enPassantCaptured != null) {
            int capturedPawnRow = (movingPiece.getColor() == PieceColor.WHITE) ? toRow + 1 : toRow - 1;
            board[capturedPawnRow][toCol] = enPassantCaptured;
        }
        
        return kingIsSafe;
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece pawn = board[fromRow][fromCol];
        int direction = (pawn.getColor() == PieceColor.WHITE) ? -1 : 1;
        
        if (fromCol == toCol && board[toRow][toCol] == null) {
            if (toRow == fromRow + direction) {
                return true;
            }
            if (!pawn.hasMoved() && toRow == fromRow + 2 * direction && 
                board[fromRow + direction][fromCol] == null) {
                return true;
            }
        }
        
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            if (board[toRow][toCol] != null) {
                return true;
            }
            
            ChessPiece adjacentPiece = board[fromRow][toCol];
            if (adjacentPiece != null && 
                adjacentPiece.getType() == PieceType.PAWN &&
                adjacentPiece.getColor() != pawn.getColor() &&
                toCol == enPassantTargetCol &&
                adjacentPiece.getColor() == enPassantTargetColor) {
                return true;
            }
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
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        
        if (rowDiff <= 1 && colDiff <= 1) {
            return true;
        }
        
        if (rowDiff == 0 && colDiff == 2 && !board[fromRow][fromCol].hasMoved()) {
            return canCastle(fromRow, fromCol, toRow, toCol);
        }
        
        return false;
    }
    
    private boolean canCastle(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece king = board[fromRow][fromCol];
        
        if (king.hasMoved()) {
            return false;
        }
        
        if (isKingInCheck(king.getColor())) {
            return false;
        }
        
        boolean isWhite = king.getColor() == PieceColor.WHITE;
        
        if (toCol == 6) {
            if (isWhite && whiteRookRightMoved) return false;
            if (!isWhite && blackRookRightMoved) return false;
            
            ChessPiece rook = board[fromRow][7];
            if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) {
                return false;
            }
            
            if (board[fromRow][5] != null || board[fromRow][6] != null) {
                return false;
            }
            
            if (wouldSquareBeThreatened(fromRow, 5, king.getColor()) ||
                wouldSquareBeThreatened(fromRow, 6, king.getColor())) {
                return false;
            }
            
            return true;
        }
        
        if (toCol == 2) {
            if (isWhite && whiteRookLeftMoved) return false;
            if (!isWhite && blackRookLeftMoved) return false;
            
            ChessPiece rook = board[fromRow][0];
            if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) {
                return false;
            }
            
            if (board[fromRow][1] != null || board[fromRow][2] != null || 
                board[fromRow][3] != null) {
                return false;
            }
            
            if (wouldSquareBeThreatened(fromRow, 2, king.getColor()) ||
                wouldSquareBeThreatened(fromRow, 3, king.getColor())) {
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    private boolean wouldSquareBeThreatened(int row, int col, PieceColor kingColor) {
        PieceColor opponentColor = (kingColor == PieceColor.WHITE) ? 
                                    PieceColor.BLACK : PieceColor.WHITE;
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece piece = board[r][c];
                if (piece != null && piece.getColor() == opponentColor) {
                    PieceColor savedTurn = currentTurn;
                    currentTurn = opponentColor;
                    
                    if (isValidMoveIgnoringCheck(r, c, row, col)) {
                        currentTurn = savedTurn;
                        return true;
                    }
                    
                    currentTurn = savedTurn;
                }
            }
        }
        return false;
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
        if (!isKingInCheck(color)) {
            return false;
        }
        
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = board[fromRow][fromCol];
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (wouldMoveEscapeCheck(fromRow, fromCol, toRow, toCol, color)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    private boolean wouldMoveEscapeCheck(int fromRow, int fromCol, int toRow, int toCol, PieceColor color) {
        PieceColor savedTurn = currentTurn;
        currentTurn = color;
        
        if (!isValidMoveIgnoringCheck(fromRow, fromCol, toRow, toCol)) {
            currentTurn = savedTurn;
            return false;
        }
        
        ChessPiece movingPiece = board[fromRow][fromCol];
        ChessPiece capturedPiece = board[toRow][toCol];
        
        ChessPiece enPassantCaptured = null;
        if (movingPiece.getType() == PieceType.PAWN && fromCol != toCol && capturedPiece == null) {
            int capturedPawnRow = (movingPiece.getColor() == PieceColor.WHITE) ? toRow + 1 : toRow - 1;
            if (capturedPawnRow >= 0 && capturedPawnRow < 8) {
                enPassantCaptured = board[capturedPawnRow][toCol];
                board[capturedPawnRow][toCol] = null;
            }
        }
        
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        
        boolean stillInCheck = isKingInCheck(color);
        
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;
        
        if (enPassantCaptured != null) {
            int capturedPawnRow = (movingPiece.getColor() == PieceColor.WHITE) ? toRow + 1 : toRow - 1;
            board[capturedPawnRow][toCol] = enPassantCaptured;
        }
        
        currentTurn = savedTurn;
        
        return !stillInCheck;
    }
    
    private boolean isKingInCheck(PieceColor kingColor) {
        int[] kingPos = findKing(kingColor);
        if (kingPos == null) {
            return false;
        }
        
        int kingRow = kingPos[0];
        int kingCol = kingPos[1];
        
        PieceColor opponentColor = (kingColor == PieceColor.WHITE) ? 
                                    PieceColor.BLACK : PieceColor.WHITE;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.getColor() == opponentColor) {
                    PieceColor savedTurn = currentTurn;
                    currentTurn = opponentColor;
                    
                    if (isValidMoveIgnoringCheck(row, col, kingRow, kingCol)) {
                        currentTurn = savedTurn;
                        return true;
                    }
                    
                    currentTurn = savedTurn;
                }
            }
        }
        
        return false;
    }
    
    private boolean isValidMoveIgnoringCheck(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
            toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }

        ChessPiece piece = board[fromRow][fromCol];
        if (piece == null) {
            return false;
        }

        ChessPiece target = board[toRow][toCol];
        if (target != null && target.getColor() == piece.getColor()) {
            return false;
        }
        
        switch (piece.getType()) {
            case PAWN:
                int direction = (piece.getColor() == PieceColor.WHITE) ? -1 : 1;
                if (fromCol == toCol && target == null) {
                    if (toRow == fromRow + direction) return true;
                    if (!piece.hasMoved() && toRow == fromRow + 2 * direction && 
                        board[fromRow + direction][fromCol] == null) return true;
                }
                if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction && target != null) {
                    return true;
                }
                return false;
            case ROOK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING:
                return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
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