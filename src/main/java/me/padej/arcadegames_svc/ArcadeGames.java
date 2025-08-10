package me.padej.arcadegames_svc;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcadeGames implements ModInitializer {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static String MOD_ID = "arcadegames_svc";
    public static final Logger LOGGER = LoggerFactory.getLogger("SVC Arcade Games");

    @Override
    public void onInitialize() {
    }
}
