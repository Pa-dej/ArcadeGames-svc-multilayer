package me.padej.arcadegames_svc.interact;

import me.padej.arcadegames_svc.client.ArcadeGamesClient;
import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.mixin.accessor.SkullBlockEntityAccessor;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.screen.GameLobbyScreen;
import me.padej.arcadegames_svc.voice.data.lobby.CreateLobbyData;
import me.padej.arcadegames_svc.voice.data.lobby.JoinLobbyData;
import me.padej.arcadegames_svc.voice.data.lobby.LeaveLobbyData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

public final class BlockClickHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();

            if (block == Blocks.PLAYER_HEAD || block == Blocks.PLAYER_WALL_HEAD) {
                BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());

                if (blockEntity instanceof SkullBlockEntity skullBlockEntity) {
                    Text customName = ((SkullBlockEntityAccessor) skullBlockEntity).getCustomName();

                    if (customName != null) {
                        String name = customName.getString();
                        MinecraftClient client = MinecraftClient.getInstance();
                        BlockPos pos = blockEntity.getPos();

                        if (!(client.currentScreen instanceof GameLobbyScreen)) {
                            if (LobbyManager.exists(pos)) {
                                joinLobby(pos);
                            } else {
                                createLobby(pos);
                            }
                        }

                        openGameScreen(name, blockEntity.getPos());

                        return ActionResult.SUCCESS;
                    }
                }
            }

            return ActionResult.PASS;
        });
    }

    private static void openGameScreen(String gameName, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            Function<BlockPos, ArcadeGame> factory = ArcadeGamesClient.GAMES.get(gameName);
            if (factory == null) return;

            ArcadeGame game = factory.apply(pos);
            client.setScreen(new GameLobbyScreen(game, pos));
        });
    }

    private static void createLobby(BlockPos pos) {
        LocalVoicePacket.send(new CreateLobbyData(pos));
        // Локально добавляем себя
        addSelfToLobby(pos);
    }

    private static void joinLobby(BlockPos pos) {
        LocalVoicePacket.send(new JoinLobbyData(pos));
        // Локально добавляем себя
        addSelfToLobby(pos);
    }

    public static void leaveLobby(BlockPos pos) {
        LocalVoicePacket.send(new LeaveLobbyData(pos));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String playerName = client.player.getName().getString();
            LobbyManager.removePlayer(pos, playerName);
        }
    }

    private static void addSelfToLobby(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String playerName = client.player.getName().getString();
            LobbyManager.addPlayer(pos, playerName);
        }
    }
}



