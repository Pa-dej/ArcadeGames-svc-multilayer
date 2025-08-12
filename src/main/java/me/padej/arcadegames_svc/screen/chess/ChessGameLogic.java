package me.padej.arcadegames_svc.screen.chess;

public class ChessGameLogic {
    private final int[][] board = new int[8][8];
    private int currentPlayer = 1;
    private int selectedX = -1, selectedY = -1;
    private boolean gameOver = false;
    private int winner = 0;
    private boolean whiteKingMoved = false;
    private boolean whiteRookA1Moved = false;
    private boolean whiteRookH1Moved = false;
    private boolean blackKingMoved = false;
    private boolean blackRookA8Moved = false;
    private boolean blackRookH8Moved = false;
    private int enPassantTargetX = -1; // Track en passant target square
    private int enPassantTargetY = -1;

    public void initBoard() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board[y][x] = 0;
            }
        }
        board[7][0] = board[7][7] = 1; // White rooks
        board[7][1] = board[7][6] = 2; // White knights
        board[7][2] = board[7][5] = 3; // White bishops
        board[7][3] = 4; // White queen
        board[7][4] = 5; // White king
        for (int x = 0; x < 8; x++) board[6][x] = 6; // White pawns
        board[0][0] = board[0][7] = 7; // Black rooks
        board[0][1] = board[0][6] = 8; // Black knights
        board[0][2] = board[0][5] = 9; // Black bishops
        board[0][3] = 10; // Black queen
        board[0][4] = 11; // Black king
        for (int x = 0; x < 8; x++) board[1][x] = 12; // Black pawns
        whiteKingMoved = false;
        whiteRookA1Moved = false;
        whiteRookH1Moved = false;
        blackKingMoved = false;
        blackRookA8Moved = false;
        blackRookH8Moved = false;
        enPassantTargetX = -1;
        enPassantTargetY = -1;
    }

    public void resetGame() {
        initBoard();
        currentPlayer = 1;
        winner = 0;
        gameOver = false;
        clearSelection();
    }

    public boolean processMove(int fromX, int fromY, int toX, int toY, int player) {
        if (!isValidMove(fromX, fromY, toX, toY, player)) return false;

        int piece = board[fromY][fromX];
        int targetPiece = board[toY][toX];
        // Handle en passant capture
        if (piece == 6 && player == 1 && toY == enPassantTargetY && toX == enPassantTargetX && toY == fromY - 1 && Math.abs(toX - fromX) == 1) {
            board[fromY][toX] = 0; // Remove the captured pawn
        } else if (piece == 12 && player == 2 && toY == enPassantTargetY && toX == enPassantTargetX && toY == fromY + 1 && Math.abs(toX - fromX) == 1) {
            board[fromY][toX] = 0; // Remove the captured pawn
        }

        // Move the piece
        board[toY][toX] = piece;
        board[fromY][fromX] = 0;

        // Handle pawn promotion
        if (piece == 6 && toY == 0) {
            board[toY][toX] = 4; // Promote to white queen
        } else if (piece == 12 && toY == 7) {
            board[toY][toX] = 10; // Promote to black queen
        }

        // Update en passant target
        enPassantTargetX = -1;
        enPassantTargetY = -1;
        if (piece == 6 && fromY == 6 && toY == 4 && Math.abs(toX - fromX) == 0) {
            enPassantTargetX = toX;
            enPassantTargetY = 5;
        } else if (piece == 12 && fromY == 1 && toY == 3 && Math.abs(toX - fromX) == 0) {
            enPassantTargetX = toX;
            enPassantTargetY = 2;
        }

        // Handle castling
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        if ((piece == 5 || piece == 11) && dy == 0 && dx == 2) {
            int rookFromX = (toX > fromX) ? 7 : 0;
            int rookToX = (toX > fromX) ? fromX + 1 : fromX - 1;
            board[fromY][rookToX] = board[fromY][rookFromX];
            board[fromY][rookFromX] = 0;
            if (player == 1) {
                if (rookFromX == 0) whiteRookA1Moved = true;
                else whiteRookH1Moved = true;
            } else {
                if (rookFromX == 0) blackRookA8Moved = true;
                else blackRookH8Moved = true;
            }
        }

        // Update movement flags
        if (piece == 5) whiteKingMoved = true;
        else if (piece == 11) blackKingMoved = true;
        else if (piece == 1) {
            if (fromX == 0 && fromY == 7) whiteRookA1Moved = true;
            else if (fromX == 7 && fromY == 7) whiteRookH1Moved = true;
        } else if (piece == 7) {
            if (fromX == 0 && fromY == 0) blackRookA8Moved = true;
            else if (fromX == 7 && fromY == 0) blackRookH8Moved = true;
        }

        // Check for game end
        if ((player == 1 && targetPiece == 11) || (player == 2 && targetPiece == 5)) {
            winner = player;
            gameOver = true;
        } else {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        if (findKing(3 - player) == null) {
            winner = player;
            gameOver = true;
        }
        return true;
    }

    public boolean isValidMove(int fromX, int fromY, int toX, int toY, int player) {
        if (!isValidPiece(fromX, fromY, player)) return false;
        if (fromX == toX && fromY == toY) return false;

        int piece = board[fromY][fromX];
        int targetPiece = board[toY][toX];
        if (player == 1 && targetPiece >= 1 && targetPiece <= 6) return false;
        if (player == 2 && targetPiece >= 7 && targetPiece <= 12) return false;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        int dirY = player == 1 ? -1 : 1; // Direction for pawn movement

        return switch (piece) {
            case 1, 7 -> (dx == 0 || dy == 0) && isPathClear(fromX, fromY, toX, toY); // Rook
            case 2, 8 -> (dx == 2 && dy == 1) || (dx == 1 && dy == 2); // Knight
            case 3, 9 -> dx == dy && isPathClear(fromX, fromY, toX, toY); // Bishop
            case 4, 10 -> (dx == dy || dx == 0 || dy == 0) && isPathClear(fromX, fromY, toX, toY); // Queen
            case 5 -> (dx <= 1 && dy <= 1) || isCastlingWhite(fromX, fromY, toX, toY); // White king
            case 11 -> (dx <= 1 && dy <= 1) || isCastlingBlack(fromX, fromY, toX, toY); // Black king
            case 6 -> // White pawn
                    (toY == fromY + dirY && dx == 0 && targetPiece == 0) || // Forward one
                            (fromY == 6 && toY == 4 && dx == 0 && targetPiece == 0 && board[5][toX] == 0) || // Forward two
                            (toY == fromY + dirY && dx == 1 && (targetPiece >= 7 || (toX == enPassantTargetX && toY == enPassantTargetY))); // Capture or en passant
            case 12 -> // Black pawn
                    (toY == fromY + dirY && dx == 0 && targetPiece == 0) || // Forward one
                            (fromY == 1 && toY == 3 && dx == 0 && targetPiece == 0 && board[2][toX] == 0) || // Forward two
                            (toY == fromY + dirY && dx == 1 && (targetPiece <= 6 && targetPiece > 0 || (toX == enPassantTargetX && toY == enPassantTargetY))); // Capture or en passant
            default -> false;
        };
    }

    public boolean isValidPiece(int x, int y, int player) {
        int piece = board[y][x];
        return (player == 1 && piece >= 1 && piece <= 6) || (player == 2 && piece >= 7 && piece <= 12);
    }

    boolean isCastlingWhite(int fromX, int fromY, int toX, int toY) {
        if (whiteKingMoved || fromY != 7 || fromX != 4 || toY != 7 || Math.abs(toX - fromX) != 2) return false;
        if (toX == 6) return !whiteRookH1Moved && board[7][5] == 0 && board[7][6] == 0;
        else if (toX == 2) return !whiteRookA1Moved && board[7][1] == 0 && board[7][2] == 0 && board[7][3] == 0;
        return false;
    }

    boolean isCastlingBlack(int fromX, int fromY, int toX, int toY) {
        if (blackKingMoved || fromY != 0 || fromX != 4 || toY != 0 || Math.abs(toX - fromX) != 2) return false;
        if (toX == 6) return !blackRookH8Moved && board[0][5] == 0 && board[0][6] == 0;
        else if (toX == 2) return !blackRookA8Moved && board[0][1] == 0 && board[0][2] == 0 && board[0][3] == 0;
        return false;
    }

    private boolean isPathClear(int fromX, int fromY, int toX, int toY) {
        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        int x = fromX + dx;
        int y = fromY + dy;
        while (x != toX || y != toY) {
            if (board[y][x] != 0) return false;
            x += dx;
            y += dy;
        }
        return true;
    }

    private int[] findKing(int player) {
        int king = (player == 1) ? 5 : 11;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board[y][x] == king) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    public int[][] getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getSelectedX() {
        return selectedX;
    }

    public int getSelectedY() {
        return selectedY;
    }

    public void setSelected(int x, int y) {
        this.selectedX = x;
        this.selectedY = y;
    }

    public void clearSelection() {
        this.selectedX = -1;
        this.selectedY = -1;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public boolean isWhiteKingMoved() {
        return whiteKingMoved;
    }

    public void setWhiteKingMoved(boolean whiteKingMoved) {
        this.whiteKingMoved = whiteKingMoved;
    }

    public boolean isWhiteRookA1Moved() {
        return whiteRookA1Moved;
    }

    public void setWhiteRookA1Moved(boolean whiteRookA1Moved) {
        this.whiteRookA1Moved = whiteRookA1Moved;
    }

    public boolean isWhiteRookH1Moved() {
        return whiteRookH1Moved;
    }

    public void setWhiteRookH1Moved(boolean whiteRookH1Moved) {
        this.whiteRookH1Moved = whiteRookH1Moved;
    }

    public boolean isBlackKingMoved() {
        return blackKingMoved;
    }

    public void setBlackKingMoved(boolean blackKingMoved) {
        this.blackKingMoved = blackKingMoved;
    }

    public boolean isBlackRookA8Moved() {
        return blackRookA8Moved;
    }

    public void setBlackRookA8Moved(boolean blackRookA8Moved) {
        this.blackRookA8Moved = blackRookA8Moved;
    }

    public boolean isBlackRookH8Moved() {
        return blackRookH8Moved;
    }

    public void setBlackRookH8Moved(boolean blackRookH8Moved) {
        this.blackRookH8Moved = blackRookH8Moved;
    }

    public int getEnPassantTargetX() {
        return enPassantTargetX;
    }

    public void setEnPassantTargetX(int enPassantTargetX) {
        this.enPassantTargetX = enPassantTargetX;
    }

    public int getEnPassantTargetY() {
        return enPassantTargetY;
    }

    public void setEnPassantTargetY(int enPassantTargetY) {
        this.enPassantTargetY = enPassantTargetY;
    }
}
