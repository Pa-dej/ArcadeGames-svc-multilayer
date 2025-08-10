package me.padej.arcadegames_svc.util;

import net.minecraft.util.Identifier;

public class TextureStorage {
    private static final String MOD_ID = "arcadegames_svc";

    public static Identifier id(String pathToFile) {
        return Identifier.of(MOD_ID, pathToFile);
    }
}
