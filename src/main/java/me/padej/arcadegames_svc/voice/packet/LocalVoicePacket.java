package me.padej.arcadegames_svc.voice.packet;

import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection;
import de.maxhenkel.voicechat.voice.common.MicPacket;
import de.maxhenkel.voicechat.voice.common.NetworkMessage;
import me.padej.arcadegames_svc.voice.data.IVoiceData;

public final class LocalVoicePacket {
    public static void send(IVoiceData data) {
        ClientVoicechat client = ClientManager.getClient();
        if (client == null) return;

        ClientVoicechatConnection connection = client.getConnection();
        if (connection == null || !connection.isConnected()) return;

        long packed = data.pack();
        connection.sendToServer(new NetworkMessage(new MicPacket(new byte[0], false, packed)));

//        MinecraftClient.getInstance().player.sendMessage(Text.of("Ping | Sent Data: " + packed), false);
    }
}

