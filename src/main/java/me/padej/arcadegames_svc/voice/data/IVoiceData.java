package me.padej.arcadegames_svc.voice.data;

import me.padej.arcadegames_svc.voice.packet.VoicePacketRegistry;

public interface IVoiceData {
    long pack();
    int getId(); // 0..63

    static long applyId(long packedPayload, int id) {
        if (id < 0 || id >= 64) {
            throw new IllegalArgumentException("Packet ID must be in range 0..63");
        }
        return (packedPayload & ~(0b111111L << 58)) | ((long) id << 58);
    }

    static int extractId(long packed) {
        return (int) ((packed >>> 58) & 0b111111);
    }

    static boolean isValid(long packed) {
        return packed < 0L; // используем флаг для проверки вообще наличия данных
    }

    static IVoiceData unpack(long packed) {
        int id = extractId(packed);
        return VoicePacketRegistry.unpack(id, packed);
    }
}


