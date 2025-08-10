package me.padej.arcadegames_svc.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class ArcadeGame extends Screen {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    private final int gameId;

    public ArcadeGame(String arcadeName, int gameId) {
        super(Text.of(arcadeName));
        this.gameId = gameId;
    }

    public int getGameId() {
        return this.gameId;
    }
    public String getName() {
        return this.getTitle().getString();
    }
}
