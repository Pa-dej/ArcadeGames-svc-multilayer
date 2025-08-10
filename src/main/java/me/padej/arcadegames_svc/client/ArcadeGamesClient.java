package me.padej.arcadegames_svc.client;

import me.padej.arcadegames_svc.interact.BlockClickHandler;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.screen.chess.ChessScreen;
import me.padej.arcadegames_svc.screen.tictactoe.TicTacToeScreen;
import me.padej.arcadegames_svc.screen.pong.PongScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Function;

public final class ArcadeGamesClient implements ClientModInitializer {

    public static final Map<String, Function<BlockPos, ArcadeGame>> GAMES = new HashMap<>();
    public static final Set<UUID> OWNERS = new HashSet<>();

    static {
        GAMES.put("Pong", PongScreen::new);
        GAMES.put("Tic Tac Toe", TicTacToeScreen::new);
        GAMES.put("Chess", ChessScreen::new);
    }

    static {
        OWNERS.add(UUID.fromString("8babb7f5-acc8-40e5-bcff-247503d18526"));
        OWNERS.add(UUID.fromString("06593315-c800-4c62-a360-5b36def03762"));
        OWNERS.add(UUID.fromString("fcb83e9a-a3f3-3f7f-89d0-6640a4e78f7e")); // 0
        OWNERS.add(UUID.fromString("7f77fd15-dee9-3fc5-84c6-4b32eed2189e")); // meh_meh_meh
    }

    @Override
    public void onInitializeClient() {
        BlockClickHandler.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onDebugTick);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null) return;

        LoadingText.update();
    }

    private void onDebugTick(MinecraftClient mc) {

        SimpleOption<Integer> guiScale = mc.options.getGuiScale();

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_I) &&
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_1))
            guiScale.setValue(1);
        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_I) &&
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_2))
            guiScale.setValue(2);
        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_I) &&
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_3))
            guiScale.setValue(3);
        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_I) &&
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_4))
            guiScale.setValue(4);
    }

}
