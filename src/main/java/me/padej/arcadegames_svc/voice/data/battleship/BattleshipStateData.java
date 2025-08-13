package me.padej.arcadegames_svc.voice.data.battleship;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record BattleshipStateData(int chunkIndex, int[] cellsChunk, int currentPlayer, int winner, boolean gameOver, boolean setupPhase) implements IVoiceData {
    public static final int ID = 14;
    private static final int CELLS_PER_CHUNK = 17;

    public BattleshipStateData {
        if (chunkIndex < 0 || chunkIndex >= 24) {
            throw new IllegalArgumentException("Chunk index must be 0-23");
        }
        if (cellsChunk.length > CELLS_PER_CHUNK) {
            throw new IllegalArgumentException("Cells chunk size exceeds maximum");
        }
    }

    @Override
    public long pack() {
        long packed = 0L;
        for (int i = 0; i < CELLS_PER_CHUNK && i < cellsChunk.length; i++) {
            packed |= ((long) (cellsChunk[i] & 0b111)) << (i * 3);
        }
        packed |= ((long) (chunkIndex & 0b11111)) << 51;
        packed |= ((long) (currentPlayer & 0b11)) << 56;
        packed |= ((long) (winner & 0b11)) << 58;
        packed |= ((long) (gameOver ? 1 : 0)) << 60;
        packed |= ((long) (setupPhase ? 1 : 0)) << 61;
        return IVoiceData.applyId(packed, getId());
    }

    public static BattleshipStateData unpack(long packed) {
        int[] cellsChunk = new int[CELLS_PER_CHUNK];
        for (int i = 0; i < CELLS_PER_CHUNK; i++) {
            cellsChunk[i] = (int) ((packed >> (i * 3)) & 0b111);
        }
        int chunkIndex = (int) ((packed >> 51) & 0b11111);
        int currentPlayer = (int) ((packed >> 56) & 0b11);
        int winner = (int) ((packed >> 58) & 0b11);
        boolean gameOver = ((packed >> 60) & 1) == 1;
        boolean setupPhase = ((packed >> 61) & 1) == 1;
        return new BattleshipStateData(chunkIndex, cellsChunk, currentPlayer, winner, gameOver, setupPhase);
    }

    @Override
    public int getId() {
        return ID;
    }
}
