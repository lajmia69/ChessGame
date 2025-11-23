package com.chess.server;

import com.chess.model.PieceColor;
import com.chess.network.ChessMessage;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private PieceColor playerColor;
    private ChessServer server;

    public ClientHandler(Socket socket, PieceColor color, ChessServer server) {
        this.socket = socket;
        this.playerColor = color;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Handler " + playerColor + "] Streams initialized");

            // Assign player color
            sendMessage(ChessMessage.createPlayerAssignment(playerColor));
            
            // Send initial board state
            sendMessage(ChessMessage.createBoardUpdate(server.getBoard()));

            // Listen for messages from client
            while (true) {
                ChessMessage message = (ChessMessage) in.readObject();
                System.out.println("[Handler " + playerColor + "] Received message: " + message.getType());
                server.handleClientMessage(message);
            }
        } catch (EOFException e) {
            System.out.println("[Handler " + playerColor + "] Player disconnected normally");
        } catch (Exception e) {
            System.out.println("[Handler " + playerColor + "] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void sendMessage(ChessMessage message) {
        try {
            System.out.println("[Handler " + playerColor + "] Sending message: " + message.getType());
            out.reset(); // CRITICAL: Clear cache
            out.writeObject(message);
            out.flush();
            System.out.println("[Handler " + playerColor + "] Message sent successfully");
        } catch (IOException e) {
            System.err.println("[Handler " + playerColor + "] ERROR sending: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PieceColor getPlayerColor() {
        return playerColor;
    }
}