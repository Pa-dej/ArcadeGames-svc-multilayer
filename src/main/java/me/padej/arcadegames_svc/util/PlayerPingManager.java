package me.padej.arcadegames_svc.util;

import java.util.HashMap;
import java.util.Map;

public class PlayerPingManager {
    private static final Map<String, Integer> pingMap = new HashMap<>();

    public static void setPing(String playerName, int ping) {
        pingMap.put(playerName, ping);
    }

    public static int getPing(String playerName) {
        return pingMap.getOrDefault(playerName, Integer.MAX_VALUE);
    }
}

