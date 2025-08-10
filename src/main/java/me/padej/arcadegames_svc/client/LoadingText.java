package me.padej.arcadegames_svc.client;

public final class LoadingText {

    public static String text = "oOo";
    static int tickCounter = 0;

    public static void update() {
        if (tickCounter < 40) tickCounter++;
        switch (tickCounter) {
            case 10 -> text = "Ooo";
            case 20, 40 -> text = "oOo";
            case 30 -> text = "ooO";
        }
        if (tickCounter >= 40) tickCounter = 0;
    }
}
