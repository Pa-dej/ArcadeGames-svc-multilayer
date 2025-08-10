package me.padej.arcadegames_svc.voice.data.pong;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record PongMovePaddleData(boolean leftPaddle, int y) implements IVoiceData {
    public static final int ID = 6;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= (leftPaddle ? 1L : 0L);
        packed |= ((long)(y & 0xFFFF)) << 1;
        packed |= Long.MIN_VALUE;

        return IVoiceData.applyId(packed, getId());
    }

    public static PongMovePaddleData unpack(long packed) {
        boolean leftPaddle = (packed & 1) != 0;
        int y = (int)((packed >> 1) & 0xFFFF);

        return new PongMovePaddleData(leftPaddle, y);
    }

    @Override
    public int getId() {
        return ID;
    }
}

