package me.padej.arcadegames_svc.util.sound;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

import static me.padej.arcadegames_svc.ArcadeGames.mc;

public class SoundLibrary {

    public static void playRandomPitchSound(SoundEvent soundEvent, float minPitch, float maxPitch, float volume) {
        if (mc.player != null && mc.world != null) {
            Random random = Random.create();
            float pitch = minPitch + random.nextFloat() * (maxPitch - minPitch);

            mc.world.playSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    soundEvent,
                    SoundCategory.RECORDS,
                    volume,
                    pitch,
                    false
            );
        }
    }

    public static void chopSound() {
        playRandomPitchSound(SoundEvents.BLOCK_WOOD_BREAK, 0.1F, 1.73F, 1.7f);
    }

    public static void errorSound() {
        playRandomPitchSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.1F, 0.34F, 1.3f);
    }

    public static void gameOverSound() {
        playRandomPitchSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.1F, 0.24F, 0.6f);
        playRandomPitchSound(SoundEvents.ENTITY_PLAYER_HURT, 0.2F, 0.23F, 1);
    }

    public static void ponkSound() {
        playRandomPitchSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 1.0F, 2.0F, 0.6f);
    }

    public static void cartridgeSound() {
        playRandomPitchSound(SoundEvents.BLOCK_CRAFTER_CRAFT, 1.1F, 2F, 10.3f);
    }
}
