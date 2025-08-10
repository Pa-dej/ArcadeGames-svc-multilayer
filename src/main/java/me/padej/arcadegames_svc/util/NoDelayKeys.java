package me.padej.arcadegames_svc.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class NoDelayKeys {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final long HANDLE = mc.getWindow().getHandle();

    public static boolean isW_Pressed() {
        return InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_W) || InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_UP);
    }

    public static boolean isA_Pressed() {
        return InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_A) || InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_LEFT);
    }

    public static boolean isS_Pressed() {
        return InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_S) || InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_DOWN);
    }

    public static boolean isD_Pressed() {
        return InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_D) || InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_RIGHT);
    }

    public static boolean isSpace_Pressed() {
        return InputUtil.isKeyPressed(HANDLE, GLFW.GLFW_KEY_SPACE);
    }
}
