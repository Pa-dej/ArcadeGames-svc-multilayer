package me.padej.arcadegames_svc.voice.packet;

import de.maxhenkel.voicechat.voice.common.PlayerSoundPacket;
import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.screen.battleship.BattleshipScreen;
import me.padej.arcadegames_svc.screen.chess.ChessScreen;
import me.padej.arcadegames_svc.screen.tictactoe.TicTacToeScreen;
import me.padej.arcadegames_svc.screen.pong.PongScreen;
import me.padej.arcadegames_svc.voice.data.IVoiceData;
import me.padej.arcadegames_svc.voice.data.battleship.BattleshipMoveData;
import me.padej.arcadegames_svc.voice.data.battleship.BattleshipStateData;
import me.padej.arcadegames_svc.voice.data.chess.ChessMoveData;
import me.padej.arcadegames_svc.voice.data.chess.ChessStateData;
import me.padej.arcadegames_svc.voice.data.lobby.CreateLobbyData;
import me.padej.arcadegames_svc.voice.data.lobby.JoinLobbyData;
import me.padej.arcadegames_svc.voice.data.lobby.LeaveLobbyData;
import me.padej.arcadegames_svc.voice.data.lobby.ReadyLobbyData;
import me.padej.arcadegames_svc.voice.data.pong.PongMoveBallData;
import me.padej.arcadegames_svc.voice.data.pong.PongMovePaddleData;
import me.padej.arcadegames_svc.voice.data.pong.PongScoreData;
import me.padej.arcadegames_svc.voice.data.tictactoe.TicTacToeMoveData;
import me.padej.arcadegames_svc.voice.data.tictactoe.TicTacToeStateData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class VoicePacketRegistry {
    private static final Map<Integer, Function<Long, IVoiceData>> UNPACKERS = new HashMap<>();
    private static final Map<Integer, BiConsumer<IVoiceData, UUID>> HANDLERS = new HashMap<>();

    public static void register(int id, Function<Long, IVoiceData> unpacker, BiConsumer<IVoiceData, UUID> handler) {
        UNPACKERS.put(id, unpacker);
        HANDLERS.put(id, handler);
    }

    public static IVoiceData unpack(int id, long packed) {
        Function<Long, IVoiceData> unpacker = UNPACKERS.get(id);
        return unpacker != null ? unpacker.apply(packed) : null;
    }

    public static void handle(PlayerSoundPacket packet) {
        if (packet.getData().length != 0) return;

        long packed = packet.getSequenceNumber();
        int id = IVoiceData.extractId(packed);

        IVoiceData data = unpack(id, packed);
        if (data != null) {
            BiConsumer<IVoiceData, UUID> handler = HANDLERS.get(id);
            if (handler != null) handler.accept(data, packet.getSender());
        }
    }

    public static void initPackets() {
        register(CreateLobbyData.ID, CreateLobbyData::unpack, (data, sender) -> {
            BlockPos pos = ((CreateLobbyData) data).pos();
            String playerName = getPlayerName(sender);

            if (!LobbyManager.exists(pos)) {
                LobbyManager.addPlayer(pos, playerName);
            }
        });

        register(JoinLobbyData.ID, JoinLobbyData::unpack, (data, sender) -> {
            BlockPos pos = ((JoinLobbyData) data).pos();
            String playerName = getPlayerName(sender);

            if (LobbyManager.exists(pos)) {
                LobbyManager.addPlayer(pos, playerName);
            }
        });

        register(LeaveLobbyData.ID, LeaveLobbyData::unpack, (data, sender) -> {
            BlockPos pos = ((LeaveLobbyData) data).pos();
            String playerName = getPlayerName(sender);

            if (LobbyManager.exists(pos)) {
                LobbyManager.removePlayer(pos, playerName);
            }
        });

        register(ReadyLobbyData.ID, ReadyLobbyData::unpack, (data, sender) -> {
            BlockPos pos = ((ReadyLobbyData) data).pos();
            String playerName = getPlayerName(sender);
            LobbyManager.toggleReady(pos, playerName);
        });

        register(PongMovePaddleData.ID, PongMovePaddleData::unpack, (data, sender) -> PongScreen.onGetPaddleData((PongMovePaddleData) data));

        register(PongMoveBallData.ID, PongMoveBallData::unpack, (data, sender) -> PongScreen.onGetBallData((PongMoveBallData) data));

        register(PongScoreData.ID, PongScoreData::unpack, (data, sender) -> PongScreen.onGetScoreData((PongScoreData) data));

        register(TicTacToeMoveData.ID, TicTacToeMoveData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof TicTacToeScreen screen) {
                TicTacToeMoveData move = (TicTacToeMoveData) data;
                screen.onRemoteMove(move);
            }
        });

        register(TicTacToeStateData.ID, TicTacToeStateData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof TicTacToeScreen screen) {
                TicTacToeStateData state = (TicTacToeStateData) data;
                screen.onRemoteState(state);
            }
        });

        register(ChessMoveData.ID, ChessMoveData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof ChessScreen screen) {
                ChessMoveData move = (ChessMoveData) data;
                screen.onRemoteMove(move);
            }
        });

        register(ChessStateData.ID, ChessStateData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof ChessScreen screen) {
                ChessStateData state = (ChessStateData) data;
                screen.onRemoteState(state);
            }
        });

        register(BattleshipMoveData.ID, BattleshipMoveData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof BattleshipScreen screen) {
                BattleshipMoveData move = (BattleshipMoveData) data;
                screen.onRemoteMove(move);
            }
        });

        register(BattleshipStateData.ID, BattleshipStateData::unpack, (data, sender) -> {
            if (MinecraftClient.getInstance().currentScreen instanceof BattleshipScreen screen) {
                BattleshipStateData state = (BattleshipStateData) data;
                screen.onRemoteState(state, state.chunkIndex() / 6); // Simplified grid index
            }
        });
    }

    private static String getPlayerName(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            PlayerEntity player = client.world.getPlayerByUuid(uuid);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return uuid.toString();
    }
}
