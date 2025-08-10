package me.padej.arcadegames_svc.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SavesStorage {
    private static final String DIR = "config/arcadegames/save/";

    public static final Path ATARI_BREAKOUT_SAVE = Paths.get(DIR + "atari_breakout_save.json");
    public static final Path BRICK_RACING_SAVE   = Paths.get(DIR + "brick_race_save.json");
    public static final Path DOODLE_JUMP_SAVE    = Paths.get(DIR + "doodle_jump_save.json");
    public static final Path FLAPPY_BIRD_SAVE    = Paths.get(DIR + "flappy_bird_save.json");
    public static final Path GOOGLE_DINO_SAVE    = Paths.get(DIR + "google_dino_save.json");
    public static final Path LUMBERJACK_SAVE     = Paths.get(DIR + "lumberjack_save.json");
    public static final Path MINE_SWEEPER_SAVE   = Paths.get(DIR + "minesweeper_save.json");
    public static final Path SNAKE_SAVE          = Paths.get(DIR + "snake_save.json");
    public static final Path SPACE_INVADERS_SAVE = Paths.get(DIR + "space_invaders_save.json");
    public static final Path TETRIS_SAVE         = Paths.get(DIR + "tetris_save.json");
    public static final Path THE_2048_SAVE       = Paths.get(DIR + "2048_save.json");
}
