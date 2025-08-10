package me.padej.arcadegames_svc.voice.data.pong;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record PongMoveBallData(int ballX, int ballY) implements IVoiceData {
    public static final int ID = 7;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= ((long) ballX & 0x1FFFFFFFL) << 29;
        packed |= ((long) ballY & 0x1FFFFFFFL);
        return IVoiceData.applyId(packed, getId());
    }

    public static PongMoveBallData unpack(long packed) {
        long payload = packed & ((1L << 58) - 1);
        int x = (int) ((payload >> 29) & 0x1FFFFFFF);
        int y = (int) (payload & 0x1FFFFFFF);
        return new PongMoveBallData(x, y);
    }

    @Override
    public int getId() {
        return ID;
    }
}