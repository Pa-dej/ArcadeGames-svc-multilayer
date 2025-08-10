package me.padej.arcadegames_svc.voice;

import de.maxhenkel.voicechat.api.ClientVoicechatSocket;
import de.maxhenkel.voicechat.api.RawUdpPacket;
import de.maxhenkel.voicechat.plugins.impl.ClientVoicechatSocketImpl;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientNetworkMessage;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection;
import de.maxhenkel.voicechat.voice.common.NetworkMessage;
import de.maxhenkel.voicechat.voice.common.PlayerSoundPacket;
import me.padej.arcadegames_svc.voice.packet.VoicePacketRegistry;

import java.net.SocketAddress;

public record ArcadeGamesSocket(ClientVoicechatSocket delegate) implements ClientVoicechatSocket {
    public ArcadeGamesSocket {
        if (delegate == null) {
            delegate = new ClientVoicechatSocketImpl();
        }
    }

    @Override
    public void open() throws Exception {
        this.delegate.open();
    }

    @Override
    public RawUdpPacket read() throws Exception {
        RawUdpPacket rawUdpPacket = this.delegate.read();
        ClientVoicechat client = ClientManager.getClient();
        if (client != null) {
            ClientVoicechatConnection connection = client.getConnection();
            if (connection != null) {
                NetworkMessage message = ClientNetworkMessage.readPacketClient(rawUdpPacket, connection);
                if (message != null && message.getPacket() instanceof PlayerSoundPacket playerSoundPacket) {
                    VoicePacketRegistry.handle(playerSoundPacket);
                }
            }
        }
        return rawUdpPacket;
    }

    @Override
    public void send(byte[] data, SocketAddress address) throws Exception {
        this.delegate.send(data, address);
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    @Override
    public boolean isClosed() {
        return this.delegate.isClosed();
    }
}
