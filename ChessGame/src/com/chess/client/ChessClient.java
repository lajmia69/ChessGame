package com.chess.client;

import com.chess.model.ChessBoard;
import com.chess.model.ChessPiece;
import com.chess.model.PieceColor;
import com.chess.model.PieceType;
import com.chess.network.ChessMessage;
import com.chess.network.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class ChessClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color SELECTED_HIGHLIGHT = new Color(246, 246, 130);
    private static final Color VALID_MOVE_DOT = new Color(80, 80, 80, 200);
    private static final Color CHECK_HIGHLIGHT = new Color(255, 100, 100);
    private static final Color CHECK_BORDER = new Color(200, 0, 0);
    private static final Color PIECE_COLOR = new Color(50, 50, 50);
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private ChessBoard board;
    private PieceColor myColor;
    private ChessSquarePanel[][] boardSquares;
    private JLabel statusLabel;
    private JLabel colorLabel;
    private JLabel moveCountLabel;
    private JButton musicToggleButton;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<int[]> validMoves = new ArrayList<>();
    
    private Clip musicClip;
    private boolean isMusicPlaying = false;
    
    private int promotionFromRow = -1;
    private int promotionFromCol = -1;
    private int promotionToRow = -1;
    private int promotionToCol = -1;

    public ChessClient() {
        setTitle("♔ Online Chess Game ♚");
        setSize(700, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        getContentPane().setBackground(new Color(40, 40, 40));

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        topPanel.setBackground(new Color(40, 40, 40));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        
        colorLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        colorLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        colorLabel.setForeground(new Color(230, 230, 230));
        topPanel.add(colorLabel);
        
        statusLabel = new JLabel("Please wait...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        statusLabel.setForeground(new Color(180, 180, 180));
        topPanel.add(statusLabel);
        
        moveCountLabel = new JLabel("Move: 0", SwingConstants.CENTER);
        moveCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        moveCountLabel.setForeground(new Color(0, 0, 0));
        topPanel.add(moveCountLabel);
        
        musicToggleButton = new JButton("🔊 Music: ON");
        musicToggleButton.setBackground(Color.BLACK);
musicToggleButton.setForeground(Color.WHITE);

musicToggleButton.setOpaque(true);           
musicToggleButton.setBorderPainted(false);   
musicToggleButton.setContentAreaFilled(true);
musicToggleButton.setFocusPainted(false);
musicToggleButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        musicToggleButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        musicToggleButton.setBackground(new Color(0, 0, 0));
        musicToggleButton.setForeground(Color.WHITE);
        musicToggleButton.setFocusPainted(false);
        musicToggleButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        musicToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        musicToggleButton.addActionListener(e -> toggleMusic());
        topPanel.add(musicToggleButton);
        
        add(topPanel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            BorderFactory.createLineBorder(new Color(100, 100, 100), 4)
        ));
        boardSquares = new ChessSquarePanel[8][8];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Color squareColor = ((i + j) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                ChessSquarePanel square = new ChessSquarePanel(squareColor, i, j);
                
                final int row = i;
                final int col = j;
                square.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSquareClick(row, col);
                    }
                });
                
                boardSquares[i][j] = square;
                boardPanel.add(square);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.setBackground(new Color(40, 40, 40));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        
        JLabel instructionLabel1 = new JLabel("Click your piece to see valid moves (gray dots)", SwingConstants.CENTER);
        instructionLabel1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        instructionLabel1.setForeground(new Color(150, 150, 150));
        bottomPanel.add(instructionLabel1);
        
        JLabel instructionLabel2 = new JLabel("Special moves: Castling | En Passant | Pawn Promotion", SwingConstants.CENTER);
        instructionLabel2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        instructionLabel2.setForeground(new Color(130, 130, 130));
        bottomPanel.add(instructionLabel2);
        
        add(bottomPanel, BorderLayout.SOUTH);

        initializeBackgroundMusic();

        connectToServer();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
private void toggleMusic() {
    if (musicClip != null) {
        if (isMusicPlaying) {
            musicClip.stop();
            isMusicPlaying = false;
            musicToggleButton.setText("🔇 Music: OFF");
        } else {
            musicClip.setFramePosition(0);
            musicClip.start();
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            isMusicPlaying = true;
            musicToggleButton.setText("🔊 Music: ON");
        }

        musicToggleButton.setBackground(Color.BLACK);
        musicToggleButton.setForeground(Color.WHITE);
        musicToggleButton.repaint();
    }
}


    
    private void initializeBackgroundMusic() {
        try {
            File musicFile = new File("C:\\Users\\lajmi\\Downloads\\Conan.wav");
            
            if (!musicFile.exists()) {
                String userHome = System.getProperty("user.home");
                String[] possiblePaths = {
                    userHome + "\\Downloads\\Conan.wav",
                    userHome + "/Downloads/Conan.wav",
                    userHome + "\\Downloads\\conan.wav",
                    userHome + "/Downloads/conan.wav"
                };
                
                for (String path : possiblePaths) {
                    File testFile = new File(path);
                    if (testFile.exists()) {
                        musicFile = testFile;
                        break;
                    }
                }
            }
            
            if (musicFile.exists()) {
                System.out.println("[AUDIO] ✓ Found music file: " + musicFile.getAbsolutePath());
                
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
                AudioFormat format = audioStream.getFormat();
                
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                
                Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
                boolean clipOpened = false;
                
                for (Mixer.Info mixerInfo : mixerInfos) {
                    String mixerName = mixerInfo.getName().toLowerCase();
                    if (mixerName.contains("speakers") || mixerName.contains("primary sound driver")) {
                        try {
                            Mixer mixer = AudioSystem.getMixer(mixerInfo);
                            if (mixer.isLineSupported(info)) {
                                musicClip = (Clip) mixer.getLine(info);
                                audioStream = AudioSystem.getAudioInputStream(musicFile);
                                musicClip.open(audioStream);
                                System.out.println("[AUDIO] ✓ Using mixer: " + mixerInfo.getName());
                                clipOpened = true;
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                
                if (!clipOpened) {
                    musicClip = AudioSystem.getClip();
                    audioStream = AudioSystem.getAudioInputStream(musicFile);
                    musicClip.open(audioStream);
                }
                
                if (musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volume = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                    volume.setValue(volume.getMaximum());
                }
                
                musicClip.setFramePosition(0);
                musicClip.start();
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                isMusicPlaying = true;
                
                System.out.println("[AUDIO] ♪♪♪ MUSIC PLAYBACK STARTED ♪♪♪");
                
            } else {
                System.err.println("[AUDIO] ✗ Music file NOT FOUND!");
                musicToggleButton.setEnabled(false);
                musicToggleButton.setText("🔇 No Music File");
                musicToggleButton.setBackground(new Color(100, 100, 100));
            }
            
        } catch (Exception e) {
            System.err.println("[AUDIO] ✗ Error: " + e.getMessage());
            musicToggleButton.setEnabled(false);
            musicToggleButton.setText("🔇 Music Error");
            musicToggleButton.setBackground(new Color(100, 100, 100));
        }
    }

    class ChessSquarePanel extends JPanel {
        private Color baseColor;
        private Color currentColor;
        private String pieceSymbol = "";
        private boolean showDot = false;
        private boolean isSelected = false;
        private boolean isInCheck = false;
        private int row, col;
        
        public ChessSquarePanel(Color color, int row, int col) {
            this.baseColor = color;
            this.currentColor = color;
            this.row = row;
            this.col = col;
            setPreferredSize(new Dimension(85, 85));
            setBackground(color);
        }
        
        public void setPieceSymbol(String symbol) {
            this.pieceSymbol = symbol;
            repaint();
        }
        
        public void setShowDot(boolean show) {
            this.showDot = show;
            repaint();
        }
        
        public void setSelected(boolean selected) {
            this.isSelected = selected;
            this.currentColor = selected ? SELECTED_HIGHLIGHT : baseColor;
            setBackground(currentColor);
            repaint();
        }
        
        public void setInCheck(boolean inCheck) {
            this.isInCheck = inCheck;
            this.currentColor = inCheck ? CHECK_HIGHLIGHT : baseColor;
            setBackground(currentColor);
            repaint();
        }
        
        public void resetToBase() {
            this.currentColor = baseColor;
            this.showDot = false;
            this.isSelected = false;
            this.isInCheck = false;
            setBackground(baseColor);
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            g2.setColor(currentColor);
            g2.fillRect(0, 0, width, height);
            
            if (isInCheck) {
                g2.setColor(CHECK_BORDER);
                g2.setStroke(new BasicStroke(5));
                g2.drawRect(2, 2, width - 4, height - 4);
            }
            
            if (!pieceSymbol.isEmpty()) {
                g2.setColor(isInCheck ? Color.BLACK : PIECE_COLOR);
                g2.setFont(new Font("Arial Unicode MS", isInCheck ? Font.BOLD : Font.PLAIN, isInCheck ? 56 : 50));
                FontMetrics fm = g2.getFontMetrics();
                int x = (width - fm.stringWidth(pieceSymbol)) / 2;
                int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(pieceSymbol, x, y);
            }
            
            if (showDot && pieceSymbol.isEmpty()) {
                g2.setColor(VALID_MOVE_DOT);
                int dotSize = 18;
                g2.fillOval((width - dotSize) / 2, (height - dotSize) / 2, dotSize, dotSize);
            }
            
            if (showDot && !pieceSymbol.isEmpty()) {
                g2.setColor(VALID_MOVE_DOT);
                g2.setStroke(new BasicStroke(5));
                g2.drawOval(5, 5, width - 10, height - 10);
            }
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            
            new Thread(() -> receiveMessages()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Unable to connect to server.\n\nServer: " + SERVER_HOST + ":" + SERVER_PORT + 
                "\n\nPlease make sure the server is running!", 
                "Connection Failed", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                ChessMessage message = (ChessMessage) in.readObject();
                
                switch (message.getType()) {
                    case PLAYER_ASSIGNED:
                        myColor = message.getPlayerColor();
                        String colorEmoji = myColor == PieceColor.WHITE ? "♔" : "♚";
                        colorLabel.setText("You are playing as " + myColor + " " + colorEmoji);
                        colorLabel.setForeground(myColor == PieceColor.WHITE ? 
                            new Color(255, 255, 255) : new Color(150, 150, 150));
                        statusLabel.setText("Waiting for opponent to join...");
                        break;
                        
                    case BOARD_UPDATE:
                        board = message.getBoard();
                        SwingUtilities.invokeLater(() -> {
                            updateBoard();
                            moveCountLabel.setText("Move: " + board.getMoveCount());
                            
                            boolean isMyTurn = board.getCurrentTurn() == myColor;
                            
                            if (board.isInCheck(board.getCurrentTurn())) {
                                if (isMyTurn) {
                                    statusLabel.setText("YOUR KING IS IN CHECK! Protect your king NOW!");
                                    statusLabel.setForeground(CHECK_HIGHLIGHT);
                                } else {
                                    statusLabel.setText("Nice! Opponent's king is in check");
                                    statusLabel.setForeground(new Color(76, 175, 80));
                                }
                            } else {
                                if (isMyTurn) {
                                    statusLabel.setText("It's your turn - Make your move!");
                                    statusLabel.setForeground(new Color(76, 175, 80));
                                } else {
                                    statusLabel.setText("Opponent is thinking...");
                                    statusLabel.setForeground(new Color(180, 180, 180));
                                }
                            }
                        });
                        break;
                        
                    case CHECK_NOTIFICATION:
                        PieceColor colorInCheck = message.getPlayerColor();
                        String checkTitle = (colorInCheck == myColor) ? 
                            "⚠️ CHECK - Your King is Under Attack!" : "✓ Excellent Move!";
                        String checkMessage = (colorInCheck == myColor) ? 
                            "Your king is in danger!\n\nYou must:\n• Move your king to safety, OR\n• Block the attack, OR\n• Capture the attacking piece" :
                            "You put the opponent's king in check!\n\nThey must respond to save their king.";
                        
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, checkMessage, checkTitle, 
                                colorInCheck == myColor ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                        });
                        break;
                        
                    case GAME_OVER:
                        PieceColor winner = message.getWinner();
                        boolean iWon = (winner == myColor);
                        String gameOverTitle = iWon ? "🏆 CHECKMATE - YOU WIN! 🏆" : "Game Over - Checkmate";
                        String gameOverMessage = iWon ? 
                            "Congratulations!\n\nYou have defeated your opponent!\n" + winner + " wins by CHECKMATE!" :
                            "Your opponent has won.\n\n" + winner + " wins by CHECKMATE.\n\nBetter luck next time!";
                        
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, gameOverMessage, gameOverTitle, 
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                        break;
                }
            }
        } catch (EOFException e) {
            JOptionPane.showMessageDialog(this, 
                "Connection to server was lost.\n\nThe game has ended.", 
                "Disconnected", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSquareClick(int row, int col) {
        if (board == null || board.getCurrentTurn() != myColor) {
            if (selectedRow != -1) {
                clearHighlights();
            }
            statusLabel.setText("Please wait for your turn...");
            return;
        }

        if (selectedRow == -1) {
            ChessPiece piece = board.getPiece(row, col);
            if (piece != null && piece.getColor() == myColor) {
                selectedRow = row;
                selectedCol = col;
                boardSquares[row][col].setSelected(true);
                
                validMoves.clear();
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        if (board.isValidMove(selectedRow, selectedCol, r, c)) {
                            validMoves.add(new int[]{r, c});
                            boardSquares[r][c].setShowDot(true);
                        }
                    }
                }
            }
        } else {
            ChessPiece piece = board.getPiece(selectedRow, selectedCol);
            boolean isPromotion = false;
            
            if (piece != null && piece.getType() == PieceType.PAWN) {
                if ((piece.getColor() == PieceColor.WHITE && row == 0) ||
                    (piece.getColor() == PieceColor.BLACK && row == 7)) {
                    isPromotion = true;
                }
            }
            
            if (isPromotion) {
                promotionFromRow = selectedRow;
                promotionFromCol = selectedCol;
                promotionToRow = row;
                promotionToCol = col;
                showPromotionDialog();
            } else {
                sendMove(selectedRow, selectedCol, row, col, null);
            }
            
            clearHighlights();
        }
    }
    
    private void showPromotionDialog() {
        JDialog dialog = new JDialog(this, "Pawn Promotion", true);
        dialog.setUndecorated(false);
        dialog.setLayout(new GridLayout(2, 2, 10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(new Color(40, 40, 40));
        
        PieceType[] promotionOptions = {PieceType.QUEEN, PieceType.ROOK, 
                                        PieceType.BISHOP, PieceType.KNIGHT};
        String[] symbols = {"♕", "♖", "♗", "♘"};
        String[] names = {"Queen", "Rook", "Bishop", "Knight"};
        
        for (int i = 0; i < promotionOptions.length; i++) {
            final PieceType pieceType = promotionOptions[i];
            JButton button = new JButton("<html><center><font size='6'>" + symbols[i] + 
                                        "</font><br>" + names[i] + "</center></html>");
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));

button.setBackground(Color.BLACK);
button.setForeground(Color.WHITE);

button.setOpaque(true);
button.setContentAreaFilled(true);
button.setBorderPainted(false);
button.setFocusPainted(false);
button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            button.addActionListener(e -> {
                sendMove(promotionFromRow, promotionFromCol, 
                        promotionToRow, promotionToCol, pieceType);
                dialog.dispose();
            });
            
            dialog.add(button);
        }
        
        dialog.setVisible(true);
    }
    
    private void sendMove(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {
        try {
            ChessMessage move = ChessMessage.createMoveMessage(fromRow, fromCol, toRow, toCol);
            if (promotion != null) {
                move.setPromotionType(promotion);
            }
            out.writeObject(move);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearHighlights() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boardSquares[i][j].resetToBase();
            }
        }
        selectedRow = -1;
        selectedCol = -1;
        validMoves.clear();
    }

    private void updateBoard() {
        clearHighlights();
        
        int[] whiteKingPos = board.isInCheck(PieceColor.WHITE) ? findKingPosition(PieceColor.WHITE) : null;
        int[] blackKingPos = board.isInCheck(PieceColor.BLACK) ? findKingPosition(PieceColor.BLACK) : null;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board.getPiece(i, j);
                boardSquares[i][j].setPieceSymbol(piece != null ? piece.getSymbol() : "");
                
                if ((whiteKingPos != null && i == whiteKingPos[0] && j == whiteKingPos[1]) ||
                    (blackKingPos != null && i == blackKingPos[0] && j == blackKingPos[1])) {
                    boardSquares[i][j].setInCheck(true);
                }
            }
        }
    }
    
    private int[] findKingPosition(PieceColor color) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board.getPiece(i, j);
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> new ChessClient());
    }
}