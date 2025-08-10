package me.padej.arcadegames_svc.voice.data.tictactoe;

import me.padej.arcadegames_svc.voice.data.IVoiceData;

public record TicTacToeStateData(int[] cells, int currentPlayer, int winner) implements IVoiceData {
    public static final int ID = 11;

    @Override
    public long pack() {
        long packed = 0L;
        for (int i = 0; i < 9; i++) {
            packed |= ((long) (cells[i] & 0b11)) << (i * 2); // каждая клетка 2 бита
        }
        packed |= ((long) (currentPlayer & 0b11)) << 18;
        packed |= ((long) (winner & 0b11)) << 20;
        return IVoiceData.applyId(packed, getId());
    }

    public static TicTacToeStateData unpack(long packed) {
        int[] cells = new int[9];
        for (int i = 0; i < 9; i++) {
            cells[i] = (int) ((packed >> (i * 2)) & 0b11);
        }
        int currentPlayer = (int) ((packed >> 18) & 0b11);
        int winner = (int) ((packed >> 20) & 0b11);
        return new TicTacToeStateData(cells, currentPlayer, winner);
    }

    @Override
    public int getId() {
        return ID;
    }
}

