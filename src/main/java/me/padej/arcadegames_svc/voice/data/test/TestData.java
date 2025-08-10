package me.padej.arcadegames_svc.voice.data.test;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record TestData(int testField) implements IVoiceData {
    public static final int ID = 0;

    @Override
    public long pack() {
        long packed = 0L;
        packed |= testField & 0xFFFF;
        packed |= Long.MIN_VALUE;
        return IVoiceData.applyId(packed, getId());
    }

    @Override
    public int getId() {
        return ID;
    }

    public static TestData unpack(long packed) {
        int field = (int)(packed & 0xFFFF);
        return new TestData(field);
    }
}


