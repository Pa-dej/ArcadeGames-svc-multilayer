package me.padej.arcadegames_svc.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.padej.arcadegames_svc.ArcadeGames;
import me.padej.arcadegames_svc.state.GameState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Сохранение
    public static <T extends GameState> void saveGame(T state, Path saveFile) {
        try {
            Files.createDirectories(saveFile.getParent());
            Files.writeString(saveFile, GSON.toJson(state));
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("Ошибка сохранения игры: {}", e.getMessage());
        }
    }

    // Загрузка
    public static <T extends GameState> T loadGame(Path saveFile, Class<T> clazz) {
        if (Files.exists(saveFile)) {
            try {
                String json = Files.readString(saveFile);
                return GSON.fromJson(json, clazz);
            } catch (IOException e) {
                ArcadeGames.LOGGER.error("Ошибка загрузки игры: {}", e.getMessage());
            }
        }
        return null;
    }

    public static void deleteSave(Path saveFilePath) {
        try {
            if (Files.exists(saveFilePath)) {
                Files.delete(saveFilePath);
            }
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("Ошибка при удалении сохранения: {}", e.getMessage());
        }
    }

    public static void saveBestScore(Path filePath, int score) {
        try {
            Files.writeString(filePath, String.valueOf(score));
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("Ошибка при записи файла лучшего счета: {}", e.getMessage());
        }
    }
}
