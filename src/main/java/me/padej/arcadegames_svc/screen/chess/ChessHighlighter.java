package me.padej.arcadegames_svc.screen.chess;

public class ChessHighlighter {
    private final ChessGameLogic gameLogic;
    private final boolean[][] possibleMoveHighlights = new boolean[8][8];
    private final boolean[][] captureHighlights = new boolean[8][8];
    private final boolean[][] castlingHighlights = new boolean[8][8];

    public ChessHighlighter(ChessGameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    public void updateHighlights(int player) {
        // Clear all highlights before updating
        clearHighlights();

        int selectedX = gameLogic.getSelectedX();
        int selectedY = gameLogic.getSelectedY();
        if (selectedX == -1 || selectedY == -1) {
            return; // No piece selected, keep highlights cleared
        }

        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                boolean isValid = gameLogic.isValidMove(selectedX, selectedY, tx, ty, player);
                possibleMoveHighlights[ty][tx] = isValid;
                captureHighlights[ty][tx] = isValid && gameLogic.getBoard()[ty][tx] != 0;
                int piece = gameLogic.getBoard()[selectedY][selectedX];
                int dx = Math.abs(tx - selectedX);
                int dy = Math.abs(ty - selectedY);
                // Only highlight castling if the move is explicitly a valid castling move
                if (isValid && (piece == 5 || piece == 11) && dy == 0 && dx == 2) {
                    if (piece == 5 && gameLogic.isCastlingWhite(selectedX, selectedY, tx, ty)) {
                        castlingHighlights[ty][tx] = true;
                    } else if (piece == 11 && gameLogic.isCastlingBlack(selectedX, selectedY, tx, ty)) {
                        castlingHighlights[ty][tx] = true;
                    }
                }
            }
        }
    }

    public void clearHighlights() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                possibleMoveHighlights[y][x] = false;
                captureHighlights[y][x] = false;
                castlingHighlights[y][x] = false;
            }
        }
    }

    public int getHighlightColor(int x, int y) {
        if (possibleMoveHighlights[y][x]) {
            if (castlingHighlights[y][x]) {
                return 0x808000FF; // Blue for castling
            } else if (captureHighlights[y][x]) {
                return 0x80FF0000; // Red for captures
            } else {
                return 0x8000FF00; // Green for regular moves
            }
        }
        return 0;
    }
}
