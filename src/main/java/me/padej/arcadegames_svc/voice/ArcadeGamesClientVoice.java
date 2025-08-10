package me.padej.arcadegames_svc.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import me.padej.arcadegames_svc.ArcadeGames;
import me.padej.arcadegames_svc.voice.data.test.TestData;
import me.padej.arcadegames_svc.voice.data.test.TestData2;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import me.padej.arcadegames_svc.voice.packet.VoicePacketRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public final class ArcadeGamesClientVoice implements VoicechatPlugin, ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VoicePacketRegistry.initPackets();
        ArcadeGames.LOGGER.info("Voice client initialized");
    }

    @Override
    public String getPluginId() {
        return ArcadeGames.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientVoicechatInitialization);
    }

    private void onClientVoicechatInitialization(ClientVoicechatInitializationEvent event) {
        event.setSocketImplementation(new ArcadeGamesSocket(event.getSocketImplementation()));
    }
}