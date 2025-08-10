package me.padej.arcadegames_svc.voice.data.lobby;

import me.padej.arcadegames_svc.voice.data.IVoiceData;
import net.minecraft.util.math.BlockPos;

public record CreateLobbyData(BlockPos pos) implements IVoiceData {
    public static final int ID = 2;

    @Override
    public long pack() {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        long xPart = ((long) (x & 0x1FFFFF)) << 37; // 21 бит
        long zPart = ((long) (z & 0x1FFFFF)) << 16; // 21 бит
        long yPart = ((long) (y & 0xFFF)) << 4;     // 12 бит, включая знак

        long packed = xPart | zPart | yPart;
        return IVoiceData.applyId(packed, ID);
    }

    public static CreateLobbyData unpack(long packed) {
        int x = (int) ((packed >>> 37) & 0x1FFFFF);
        int z = (int) ((packed >>> 16) & 0x1FFFFF);
        int y = (int) ((packed >>> 4) & 0xFFF);

        // Восстановление знака
        if ((x & (1 << 20)) != 0) x |= ~0x1FFFFF;
        if ((z & (1 << 20)) != 0) z |= ~0x1FFFFF;
        if ((y & (1 << 11)) != 0) y |= ~0xFFF;

        return new CreateLobbyData(new BlockPos(x, y, z));
    }

    @Override
    public int getId() {
        return ID;
    }
}

