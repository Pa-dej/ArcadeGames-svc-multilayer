package me.padej.arcadegames_svc.voice.data.battleship;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record BattleshipMoveData(int x, int y, int playerId, int shipType, boolean isHorizontal, boolean isSetup) implements IVoiceData {
    public static final int ID = 9;

    public BattleshipMoveData {
        if (x < 0 || x > 9 || y < 0 || y > 9) {
            throw new IllegalArgumentException("Coordinates must be within 0-9");
        }
        if (playerId < 1 || playerId > 2) {
            throw new IllegalArgumentException("Player ID must be 1 or 2");
        }
        if (shipType < 0 || shipType > 5) {
            shipType = 0; // 0 indicates attack move or no ship
        }
    }

    @Override
    public long pack() {
        long packed = 0L;
        packed |= ((long) x & 0b1111) << 16; // 4 bits for x (0-9)
        packed |= ((long) y & 0b1111) << 12; // 4 bits for y (0-9)
        packed |= ((long) playerId & 0b11) << 10; // 2 bits for playerId (1-2)
        packed |= ((long) shipType & 0b111) << 7; // 3 bits for shipType (0-5)
        packed |= ((long) (isHorizontal ? 1 : 0)) << 6; // 1 bit for orientation
        packed |= ((long) (isSetup ? 1 : 0)) << 5; // 1 bit for isSetup
        return IVoiceData.applyId(packed, getId());
    }

    public static BattleshipMoveData unpack(long packed) {
        int x = (int) ((packed >> 16) & 0b1111);
        int y = (int) ((packed >> 12) & 0b1111);
        int playerId = (int) ((packed >> 10) & 0b11);
        int shipType = (int) ((packed >> 7) & 0b111);
        boolean isHorizontal = ((packed >> 6) & 1) == 1;
        boolean isSetup = ((packed >> 5) & 1) == 1;
        return new BattleshipMoveData(x, y, playerId, shipType, isHorizontal, isSetup);
    }

    @Override
    public int getId() {
        return ID;
    }
}
