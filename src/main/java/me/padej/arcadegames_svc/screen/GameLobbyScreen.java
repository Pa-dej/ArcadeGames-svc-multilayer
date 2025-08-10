package me.padej.arcadegames_svc.screen;

import me.padej.arcadegames_svc.interact.BlockClickHandler;
import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.util.DrawUtil;
import me.padej.arcadegames_svc.util.PlayerPingManager;
import me.padej.arcadegames_svc.voice.data.lobby.ReadyLobbyData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameLobbyScreen extends Screen {
    private final ArcadeGame game;
    private final BlockPos lobbyPos;

    public GameLobbyScreen(ArcadeGame game, BlockPos lobbyPos) {
        super(Text.of("Game Lobby"));
        this.game = game;
        this.lobbyPos = lobbyPos;
        LobbyManager.setReady(lobbyPos, MinecraftClient.getInstance().player.getName().getString(), false);
    }

    @Override
    public void tick() {
        if (LobbyManager.areAllReady(lobbyPos)) {
            List<String> players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));
            // Найти игрока с минимальным пингом
            String hostName = players.stream()
                    .min(Comparator.comparingInt(PlayerPingManager::getPing))
                    .orElse(players.getFirst());

            // Сортируем игроков так, чтобы хост был первым
            players.remove(hostName);
            players.addFirst(hostName);

            // Передаем в PongScreen список игроков и хоста
            MinecraftClient.getInstance().setScreen(game);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int centerY = height / 2;

        DrawUtil.drawCenteredText(context, game.getName(), centerX, centerY - 60);
        DrawUtil.drawCenteredText(context, "Нажмите ПРОБЕЛ для готовности", centerX, centerY - 45);
        DrawUtil.drawCenteredText(context, "Игроки в лобби:", centerX, centerY - 30);

        List<String> players = LobbyManager.getPlayers(lobbyPos);

        for (int i = 0; i < players.size(); i++) {
            String name = players.get(i);
            boolean ready = LobbyManager.isReady(lobbyPos, name);
            Text nameText = Text.literal(name).formatted(ready ? Formatting.GREEN : Formatting.GRAY);
            DrawUtil.drawCenteredText(context, nameText, centerX, centerY - 10 + i * 15);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            LocalVoicePacket.send(new ReadyLobbyData(lobbyPos));

            // ЛОКАЛЬНО обновляем готовность клиента (по имени игрока)
            String name = MinecraftClient.getInstance().player.getName().getString();
            LobbyManager.toggleReady(lobbyPos, name);

            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        BlockClickHandler.leaveLobby(lobbyPos);
        super.close();
    }
}






