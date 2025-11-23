package com.chess.server;

import com.chess.model.ChessBoard;
import com.chess.model.PieceColor;
import com.chess.network.ChessMessage;
import com.chess.network.MessageType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChessServer {
    private static final int PORT = 8888;
    private ChessBoard board;
    private List<ClientHandler> clients;
    private int playerCount;

    public ChessServer() {
        board = new ChessBoard();
        clients = new ArrayList<>();
        playerCount = 0;
    }

    public void start() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("    ♔♕ Chess Server Started ♛♚");
        System.out.println("    Port: " + PORT);
        System.out.println("    Waiting for players...");
        System.out.println("═══════════════════════════════════════");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[+] New connection from: " + 
                                 clientSocket.getInetAddress());
                
                if (playerCount < 2) {
                    PieceColor color = (playerCount == 0) ? 
                                      PieceColor.WHITE : PieceColor.BLACK;
                    ClientHandler handler = new ClientHandler(clientSocket, color, this);
                    clients.add(handler);
                    playerCount++;
                    handler.start();
                    
                    System.out.println("[✓] Player assigned: " + color);
                    
                    if (playerCount == 2) {
                        System.out.println("\n═══════════════════════════════════════");
                        System.out.println("    ⚔️  GAME STARTED!  ⚔️");
                        System.out.println("    WHITE vs BLACK");
                        System.out.println("═══════════════════════════════════════\n");
                    }
                } else {
                    System.out.println("[✗] Game is full. Connection rejected.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void handleClientMessage(ChessMessage message) {
        if (message.getType() == MessageType.MOVE) {
            System.out.println("\n========================================");
            System.out.println("[SERVER] MOVE REQUEST RECEIVED");
            System.out.println("  From: (" + message.getFromRow() + "," + message.getFromCol() + ")");
            System.out.println("  To: (" + message.getToRow() + "," + message.getToCol() + ")");
            System.out.println("  Current turn: " + board.getCurrentTurn());
            System.out.println("========================================");
            
            boolean success = board.makeMove(
                message.getFromRow(), message.getFromCol(),
                message.getToRow(), message.getToCol()
            );
            
            if (success) {
                System.out.println("[SERVER] ✓ MOVE VALID - Move #" + board.getMoveCount());
                System.out.println("[SERVER] New turn: " + board.getCurrentTurn());
                
                // Check for check
                boolean whiteInCheck = board.isInCheck(PieceColor.WHITE);
                boolean blackInCheck = board.isInCheck(PieceColor.BLACK);
                
                if (whiteInCheck) {
                    System.out.println("[SERVER] ⚠️ WHITE KING IS IN CHECK!");
                }
                if (blackInCheck) {
                    System.out.println("[SERVER] ⚠️ BLACK KING IS IN CHECK!");
                }
                
                System.out.println("[SERVER] Broadcasting to " + clients.size() + " clients...");
                
                // IMPORTANT: Broadcast to ALL clients immediately
                ChessMessage updateMsg = ChessMessage.createBoardUpdate(board);
                broadcast(updateMsg);
                
                // Send check notification if needed
                if (whiteInCheck) {
                    broadcast(ChessMessage.createCheckNotification(PieceColor.WHITE));
                }
                if (blackInCheck) {
                    broadcast(ChessMessage.createCheckNotification(PieceColor.BLACK));
                }
                
                System.out.println("[SERVER] Broadcast complete\n");
                
                if (board.isGameOver()) {
                    System.out.println("\n[GAME OVER] Winner: " + board.getWinner());
                    broadcast(ChessMessage.createGameOver(board.getWinner()));
                }
            } else {
                System.out.println("[SERVER] ✗ INVALID MOVE - Rejected\n");
            }
        }
    }

    private synchronized void broadcast(ChessMessage message) {
        System.out.println("[BROADCAST] Sending update to " + clients.size() + " clients");
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(message);
                System.out.println("[BROADCAST] Sent to " + client.getPlayerColor());
            } catch (Exception e) {
                System.err.println("[BROADCAST ERROR] Failed to send to " + 
                                 client.getPlayerColor() + ": " + e.getMessage());
            }
        }
    }

    public ChessBoard getBoard() {
        return board;
    }

    public static void main(String[] args) {
        new ChessServer().start();
    }
}