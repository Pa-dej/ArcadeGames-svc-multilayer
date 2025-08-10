package me.padej.arcadegames_svc.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BestScoreStorage {
    private static final String DIR = "config/arcadegames/";

    public static final Path ATARI_BREAKOUT_PB = Paths.get(DIR + "atari_breakout_pb");
    public static final Path BRICK_RACING_PB   = Paths.get(DIR + "brick_racing_pb");
    public static final Path DOODLE_JUMP_PB    = Paths.get(DIR + "doodle_jump_pb");
    public static final Path FLAPPY_BIRD       = Paths.get(DIR + "flappy_bird_pb");
    public static final Path LUMBERJACK_PB     = Paths.get(DIR + "lumberjack_pb");
    public static final Path GOOGLE_DINO       = Paths.get(DIR + "google_dino_pb");
    public static final Path MINESWEEPER_PB    = Paths.get(DIR + "minesweeper_pb");
    public static final Path SNAKE_PB          = Paths.get(DIR + "snake_pb");
    public static final Path SPACE_INVADERS_PB = Paths.get(DIR + "space_invaders_pb");
    public static final Path TETRIS_PB         = Paths.get(DIR + "tetris_pb");
    public static final Path THE_2048_PB       = Paths.get(DIR + "the2048");

    public static final Path[] FILES = {
            ATARI_BREAKOUT_PB,
            BRICK_RACING_PB,
            DOODLE_JUMP_PB,
            FLAPPY_BIRD,
            GOOGLE_DINO,
            LUMBERJACK_PB,
            MINESWEEPER_PB,
            SNAKE_PB,
            SPACE_INVADERS_PB,
            TETRIS_PB,
            THE_2048_PB
    };
}
