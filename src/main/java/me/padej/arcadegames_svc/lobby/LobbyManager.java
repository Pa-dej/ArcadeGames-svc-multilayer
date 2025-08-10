package me.padej.arcadegames_svc.lobby;

import net.minecraft.util.math.BlockPos;

import java.util.*;

public class LobbyManager {
    private static final Map<BlockPos, Map<String, Boolean>> lobbies = new HashMap<>();

    public static void addPlayer(BlockPos pos, String playerName) {
        lobbies.computeIfAbsent(pos, p -> new HashMap<>()).put(playerName, false); // по умолчанию не готов
    }

    public static void removePlayer(BlockPos pos, String playerName) {
        Map<String, Boolean> lobby = lobbies.get(pos);
        if (lobby != null) {
            lobby.remove(playerName);
            if (lobby.isEmpty()) lobbies.remove(pos);
        }
    }

    public static List<String> getPlayers(BlockPos pos) {
        Map<String, Boolean> lobby = lobbies.get(pos);
        return lobby != null ? new ArrayList<>(lobby.keySet()) : Collections.emptyList();
    }

    public static boolean isReady(BlockPos pos, String playerName) {
        Map<String, Boolean> lobby = lobbies.get(pos);
        return lobby != null && Boolean.TRUE.equals(lobby.get(playerName));
    }

    public static void toggleReady(BlockPos pos, String playerName) {
        Map<String, Boolean> lobby = lobbies.get(pos);
        if (lobby != null && lobby.containsKey(playerName)) {
            lobby.put(playerName, !lobby.get(playerName));
        }
    }

    public static void setReady(BlockPos pos, String playerName, boolean ready) {
        Map<String, Boolean> lobby = lobbies.get(pos);
        if (lobby != null && lobby.containsKey(playerName)) {
            lobby.put(playerName, ready);
        }
    }

    public static boolean exists(BlockPos pos) {
        return lobbies.containsKey(pos);
    }

    public static boolean areAllReady(BlockPos pos) {
        List<String> players = getPlayers(pos);
        if (players.isEmpty()) return false;
        for (String player : players) {
            if (!isReady(pos, player)) return false;
        }
        return true;
    }
}


