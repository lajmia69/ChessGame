package com.chess.client;

import com.chess.model.ChessBoard;
import com.chess.model.ChessPiece;
import com.chess.model.PieceColor;
import com.chess.network.ChessMessage;
import com.chess.network.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChessClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private ChessBoard board;
    private PieceColor myColor;
    private JButton[][] boardButtons;
    private JLabel statusLabel;
    private JLabel colorLabel;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public ChessClient() {
        setTitle("♔ Online Chess Game ♚");
        setSize(650, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Top panel with status
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(49, 46, 43));
        
        colorLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        colorLabel.setFont(new Font("Arial", Font.BOLD, 18));
        colorLabel.setForeground(Color.WHITE);
        topPanel.add(colorLabel);
        
        statusLabel = new JLabel("Waiting for opponent...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        statusLabel.setForeground(new Color(200, 200, 200));
        topPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);

        // Chess board panel
        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setBorder(BorderFactory.createLineBorder(new Color(49, 46, 43), 10));
        boardButtons = new JButton[8][8];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                JButton button = new JButton();
                button.setFont(new Font("Arial Unicode MS", Font.PLAIN, 48));
                button.setPreferredSize(new Dimension(80, 80));
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                
                // Chess.com style colors
                if ((i + j) % 2 == 0) {
                    button.setBackground(new Color(238, 238, 210)); // Light square
                } else {
                    button.setBackground(new Color(118, 150, 86)); // Dark square
                }
                
                final int row = i;
                final int col = j;
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleSquareClick(row, col);
                    }
                });
                
                boardButtons[i][j] = button;
                boardPanel.add(button);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        // Bottom panel with instructions
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(49, 46, 43));
        JLabel instructionLabel = new JLabel("Click a piece to select, then click destination");
        instructionLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        instructionLabel.setForeground(new Color(180, 180, 180));
        bottomPanel.add(instructionLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        connectToServer();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToServer() {
        try {
            System.out.println("[CLIENT] Connecting to " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("[CLIENT] Connected! Creating streams...");
            
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("[CLIENT] Streams created. Starting receive thread...");
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveMessages();
                }
            }).start();
        } catch (IOException e) {
            System.err.println("[CLIENT] Connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Cannot connect to server at " + SERVER_HOST + ":" + SERVER_PORT + 
                "\n\nMake sure the server is running!", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                ChessMessage message = (ChessMessage) in.readObject();
                
                System.out.println("[CLIENT] Received message type: " + message.getType());
                
                switch (message.getType()) {
                    case PLAYER_ASSIGNED:
                        myColor = message.getPlayerColor();
                        System.out.println("[CLIENT] Assigned color: " + myColor);
                        colorLabel.setText("You are playing as: " + myColor + 
                            (myColor == PieceColor.WHITE ? " ♔" : " ♚"));
                        colorLabel.setForeground(myColor == PieceColor.WHITE ? 
                            Color.WHITE : new Color(100, 100, 100));
                        break;
                        
                    case BOARD_UPDATE:
                        final ChessBoard newBoard = message.getBoard();
                        System.out.println("[CLIENT] Board update received - Move #" + 
                                         newBoard.getMoveCount() + ", Turn: " + 
                                         newBoard.getCurrentTurn());
                        
                        board = newBoard;
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("[CLIENT] Updating GUI...");
                                updateBoard();
                                String turnText = board.getCurrentTurn().toString();
                                boolean isMyTurn = board.getCurrentTurn() == myColor;
                                statusLabel.setText(turnText + "'s turn" + 
                                    (isMyTurn ? " - YOUR MOVE!" : ""));
                                statusLabel.setForeground(isMyTurn ? 
                                    new Color(100, 255, 100) : new Color(200, 200, 200));
                                System.out.println("[CLIENT] GUI updated");
                            }
                        });
                        break;
                        
                    case GAME_OVER:
                        PieceColor winner = message.getWinner();
                        boolean iWon = (winner == myColor);
                        String resultText = iWon ? "🎉 YOU WIN! 🎉" : "You Lose";
                        String fullMessage = winner + " wins!\n\n" + resultText;
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(ChessClient.this, 
                                    fullMessage, 
                                    "Game Over", 
                                    JOptionPane.INFORMATION_MESSAGE);
                            }
                        });
                        break;
                }
            }
        } catch (EOFException e) {
            System.err.println("[CLIENT] Connection closed");
            JOptionPane.showMessageDialog(this, 
                "Connection to server lost!", 
                "Disconnected", 
                JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("[CLIENT] Error receiving message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSquareClick(int row, int col) {
        if (board == null || board.getCurrentTurn() != myColor) {
            if (board != null && board.getCurrentTurn() != myColor) {
                JOptionPane.showMessageDialog(this, 
                    "It's not your turn!", 
                    "Wait", 
                    JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        if (selectedRow == -1) {
            // Select piece
            ChessPiece piece = board.getPiece(row, col);
            if (piece != null && piece.getColor() == myColor) {
                selectedRow = row;
                selectedCol = col;
                highlightSquare(row, col, true);
                System.out.println("[CLIENT] Selected piece at (" + row + "," + col + ")");
            }
        } else {
            // Move piece
            System.out.println("[CLIENT] Sending move from (" + selectedRow + "," + 
                             selectedCol + ") to (" + row + "," + col + ")");
            try {
                ChessMessage move = ChessMessage.createMoveMessage(
                    selectedRow, selectedCol, row, col);
                out.writeObject(move);
                out.flush();
                System.out.println("[CLIENT] Move sent to server");
            } catch (IOException e) {
                System.err.println("[CLIENT] Error sending move: " + e.getMessage());
                e.printStackTrace();
            }
            
            highlightSquare(selectedRow, selectedCol, false);
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private void highlightSquare(int row, int col, boolean highlight) {
        if (highlight) {
            boardButtons[row][col].setBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 0), 4));
        } else {
            boardButtons[row][col].setBorder(null);
        }
    }

    private void updateBoard() {
        System.out.println("[CLIENT GUI] Updating all squares...");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board.getPiece(i, j);
                String symbol = piece != null ? piece.getSymbol() : "";
                boardButtons[i][j].setText(symbol);
                boardButtons[i][j].setBorder(null);
            }
        }
        selectedRow = -1;
        selectedCol = -1;
        repaint(); // Force repaint
        System.out.println("[CLIENT GUI] Board update complete");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChessClient();
            }
        });
    }
}