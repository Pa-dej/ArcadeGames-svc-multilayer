package me.padej.arcadegames_svc.voice.data.tictactoe;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record TicTacToeMoveData(int cellX, int cellY, int playerId) implements IVoiceData {
    public static final int ID = 10;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= ((long) cellX & 0b11) << 16; // 2 бита
        packed |= ((long) cellY & 0b11) << 14; // 2 бита
        packed |= ((long) playerId & 0b11) << 12; // 2 бита
        packed |= Long.MIN_VALUE;
        return IVoiceData.applyId(packed, getId());
    }

    public static TicTacToeMoveData unpack(long packed) {
        int cellX = (int) ((packed >> 16) & 0b11);
        int cellY = (int) ((packed >> 14) & 0b11);
        int playerId = (int) ((packed >> 12) & 0b11);
        return new TicTacToeMoveData(cellX, cellY, playerId);
    }

    @Override
    public int getId() {
        return ID;
    }
}

