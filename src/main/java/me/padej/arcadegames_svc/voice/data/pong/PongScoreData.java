package me.padej.arcadegames_svc.voice.data.pong;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record PongScoreData(int firstPlayerScore, int secondPlayerScore) implements IVoiceData {
    public static final int ID = 8;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= ((long) firstPlayerScore & 0x1FFFFFFFL) << 29;
        packed |= ((long) secondPlayerScore & 0x1FFFFFFFL);
        return IVoiceData.applyId(packed, getId());
    }

    public static PongScoreData unpack(long packed) {
        long payload = packed & ((1L << 58) - 1);
        int scoreA = (int) ((payload >> 29) & 0x1FFFFFFF);
        int scoreB = (int) (payload & 0x1FFFFFFF);
        return new PongScoreData(scoreA, scoreB);
    }

    @Override
    public int getId() {
        return ID;
    }
}
