package me.padej.arcadegames_svc.mixin.inject;

import me.padej.arcadegames_svc.client.ArcadeGamesClient;
import me.padej.arcadegames_svc.util.sound.SoundLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {

    @Inject(method = "onRenamed", at = @At("HEAD"), cancellable = true)
    public void onRename(String newName, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || newName == null) return;

        if (ArcadeGamesClient.GAMES.containsKey(newName)) {
            UUID playerUUID = client.player.getUuid();

            if (!ArcadeGamesClient.OWNERS.contains(playerUUID)) {
                client.player.sendMessage(Text.literal("§c[ERROR] Отказано в доступе."), true);
                SoundLibrary.errorSound();

                // Отменяем дальнейшее выполнение onRenamed
                ci.cancel();
            }
        }
    }
}


