package me.padej.arcadegames_svc.voice.data.chess;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record ChessStateData(int chunkIndex, int[] cellsChunk, int currentPlayer, int winner,
                             boolean whiteKingMoved, boolean blackKingMoved,
                             boolean whiteRookA1Moved, boolean whiteRookH1Moved,
                             boolean blackRookA8Moved, boolean blackRookH8Moved,
                             boolean gameOver, int enPassantTargetX, int enPassantTargetY) implements IVoiceData {
    public static final int ID = 13;
    private static final int CELLS_PER_CHUNK = 11;

    @Override
    public long pack() {
        long packed = 0L;
        // Pack cells (up to 11 cells, 4 bits each = 44 bits)
        for (int i = 0; i < CELLS_PER_CHUNK && i < cellsChunk.length; i++) {
            packed |= ((long) (cellsChunk[i] & 0b1111)) << (i * 4);
        }
        // Pack flags (1 bit each, 6 bits starting at bit 44)
        packed |= ((long) (whiteKingMoved ? 1 : 0)) << 44;
        packed |= ((long) (blackKingMoved ? 1 : 0)) << 45;
        packed |= ((long) (whiteRookA1Moved ? 1 : 0)) << 46;
        packed |= ((long) (whiteRookH1Moved ? 1 : 0)) << 47;
        packed |= ((long) (blackRookA8Moved ? 1 : 0)) << 48;
        packed |= ((long) (blackRookH8Moved ? 1 : 0)) << 49;
        // Pack chunk index (3 bits at bit 50)
        packed |= ((long) (chunkIndex & 0b111)) << 50;
        // Pack currentPlayer (2 bits at bit 53)
        packed |= ((long) (currentPlayer & 0b11)) << 53;
        // Pack winner (2 bits at bit 55)
        packed |= ((long) (winner & 0b11)) << 55;
        // Pack gameOver (1 bit at bit 57)
        packed |= ((long) (gameOver ? 1 : 0)) << 57;
        // Pack enPassantTargetX (4 bits at bit 58): X + 1 (-1 -> 0, 0 -> 1, 7 -> 8)
        int packedX = enPassantTargetX + 1;
        packed |= ((long) (packedX & 0xf)) << 58;
        // Pack enPassantTargetY code (2 bits at bit 62): 0 = -1, 1 = 2, 2 = 5
        int yCode = 0;
        if (enPassantTargetY == 2) yCode = 1;
        else if (enPassantTargetY == 5) yCode = 2;
        packed |= ((long) (yCode & 0b11)) << 62;
        return IVoiceData.applyId(packed, getId());
    }

    public static ChessStateData unpack(long packed) {
        int[] cellsChunk = new int[CELLS_PER_CHUNK];
        for (int i = 0; i < CELLS_PER_CHUNK; i++) {
            cellsChunk[i] = (int) ((packed >> (i * 4)) & 0b1111);
        }
        boolean whiteKingMoved = ((packed >> 44) & 1) == 1;
        boolean blackKingMoved = ((packed >> 45) & 1) == 1;
        boolean whiteRookA1Moved = ((packed >> 46) & 1) == 1;
        boolean whiteRookH1Moved = ((packed >> 47) & 1) == 1;
        boolean blackRookA8Moved = ((packed >> 48) & 1) == 1;
        boolean blackRookH8Moved = ((packed >> 49) & 1) == 1;
        int chunkIndex = (int) ((packed >> 50) & 0b111);
        int currentPlayer = (int) ((packed >> 53) & 0b11);
        int winner = (int) ((packed >> 55) & 0b11);
        boolean gameOver = ((packed >> 57) & 1) == 1;
        // Unpack enPassantTargetX
        int packedX = (int) ((packed >> 58) & 0xf);
        int enPassantTargetX = (packedX == 0) ? -1 : packedX - 1;
        // Unpack enPassantTargetY
        int yCode = (int) ((packed >> 62) & 0b11);
        int enPassantTargetY = (packedX == 0) ? -1 : (yCode == 1 ? 2 : (yCode == 2 ? 5 : -1));
        return new ChessStateData(chunkIndex, cellsChunk, currentPlayer, winner, whiteKingMoved, blackKingMoved,
                whiteRookA1Moved, whiteRookH1Moved, blackRookA8Moved, blackRookH8Moved, gameOver, enPassantTargetX, enPassantTargetY);
    }

    @Override
    public int getId() {
        return ID;
    }
}
