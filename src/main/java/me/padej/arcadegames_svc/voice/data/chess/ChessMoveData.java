package me.padej.arcadegames_svc.voice.data.chess;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record ChessMoveData(int fromX, int fromY, int toX, int toY, int playerId) implements IVoiceData {
    public static final int ID = 12;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= ((long) fromX & 0b111) << 16; // 3 bits
        packed |= ((long) fromY & 0b111) << 13; // 3 bits
        packed |= ((long) toX & 0b111) << 10; // 3 bits
        packed |= ((long) toY & 0b111) << 7; // 3 bits
        packed |= ((long) playerId & 0b11) << 5; // 2 bits
        return IVoiceData.applyId(packed, getId());
    }

    public static ChessMoveData unpack(long packed) {
        int fromX = (int) ((packed >> 16) & 0b111);
        int fromY = (int) ((packed >> 13) & 0b111);
        int toX = (int) ((packed >> 10) & 0b111);
        int toY = (int) ((packed >> 7) & 0b111);
        int playerId = (int) ((packed >> 5) & 0b11);
        return new ChessMoveData(fromX, fromY, toX, toY, playerId);
    }

    @Override
    public int getId() {
        return ID;
    }
}
