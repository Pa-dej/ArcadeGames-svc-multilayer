package me.padej.arcadegames_svc.screen.pong;

import me.padej.arcadegames_svc.ArcadeGames;
import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.util.DrawUtil;
import me.padej.arcadegames_svc.util.NoDelayKeys;
import me.padej.arcadegames_svc.util.PlayerPingManager;
import me.padej.arcadegames_svc.util.sound.SoundLibrary;
import me.padej.arcadegames_svc.voice.data.pong.PongMoveBallData;
import me.padej.arcadegames_svc.voice.data.pong.PongMovePaddleData;
import me.padej.arcadegames_svc.voice.data.pong.PongScoreData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PongScreen extends ArcadeGame {
    // Константы игрового поля и объектов
    private static final int FIELD_WIDTH = 256;
    private static final int FIELD_HEIGHT = 240;

    private static final int PADDLE_HEIGHT = 18;
    private static final int PADDLE_THICKNESS = 3;
    private static final int PADDLE_SPEED = 12; // px за тик (1/20 с)

    private static final int BALL_SIZE = 3;
    private static final int BALL_START_SPEED_X = 6;
    private static final int BALL_START_SPEED_Y = 3;
    private static final int BALL_MAX_SPEED_X = 18;
    private static final float BALL_SPEED_INCREASE = 1.05f;

    // Позиции и скорости мяча и ракеток
    private static int paddle1Y = FIELD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private static int paddle2Y = FIELD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private static float renderPaddle1Y = paddle1Y;
    private static float renderPaddle2Y = paddle2Y;

    private static int ballX = FIELD_WIDTH / 2 - BALL_SIZE / 2;
    private static int ballY = FIELD_HEIGHT / 2 - BALL_SIZE / 2;
    private static float renderBallX = ballX;
    private static float renderBallY = ballY;
    private static float ballVelX = BALL_START_SPEED_X;
    private static float ballVelY = BALL_START_SPEED_Y;

    private static float lastBallVelX = ballVelX;
    private static float lastBallVelY = ballVelY;

    private final String playerName;
    private final List<String> players;
    private final String hostName;

    private int score1 = 0;
    private int score2 = 0;

    private boolean gameEnded = false;
    private String winnerName = null;

    public PongScreen(BlockPos lobbyPos) {
        super("Pong", 9600);

        this.playerName = mc.player.getName().getString();
        this.players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));

        this.hostName = players.stream()
                .min(Comparator.comparingInt(PlayerPingManager::getPing))
                .orElse(players.getFirst());

        players.remove(hostName);
        players.addFirst(hostName);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int xOffset = width / 2 - FIELD_WIDTH / 2;
        int yOffset = height / 2 - FIELD_HEIGHT / 2;

        // Рисуем игровое поле
        context.fill(
                xOffset,
                yOffset,
                FIELD_WIDTH + xOffset,
                FIELD_HEIGHT + yOffset,
                0xED000000
        );

        context.drawTexture(
                RenderLayer::getGuiTextured,
                Identifier.of(ArcadeGames.MOD_ID, "textures/pong/outline.png"),
                xOffset - 9, yOffset - 9,
                0, 0,
                274, 258,
                274, 258,
                274, 258
        );

        if (!gameEnded) {
            renderPaddle1Y = MathHelper.lerp(delta, renderPaddle1Y, paddle1Y);
            renderPaddle2Y = MathHelper.lerp(delta, renderPaddle2Y, paddle2Y);
            renderBallX = MathHelper.lerp(delta, renderBallX, ballX);
            renderBallY = MathHelper.lerp(delta, renderBallY, ballY);

            // Левая ракетка (красная)
            context.fill(
                    10 + xOffset,
                    (int) renderPaddle1Y + yOffset,
                    10 + PADDLE_THICKNESS + xOffset,
                    (int) renderPaddle1Y + PADDLE_HEIGHT + yOffset,
                    0xFFFF0000
            );

            // Правая ракетка (зелёная)
            context.fill(
                    FIELD_WIDTH - 10 - PADDLE_THICKNESS + xOffset,
                    (int) renderPaddle2Y + yOffset,
                    FIELD_WIDTH - 10 + xOffset,
                    (int) renderPaddle2Y + PADDLE_HEIGHT + yOffset,
                    0xFF00FF00
            );

            // Мяч (белый)
            context.fill(
                    (int) renderBallX + xOffset,
                    (int) renderBallY + yOffset,
                    (int) renderBallX + BALL_SIZE + xOffset,
                    (int) renderBallY + BALL_SIZE + yOffset,
                    0xFFFFFFFF
            );

            // Очки
            DrawUtil.drawCenteredText(context, players.getFirst(), FIELD_WIDTH / 4 + xOffset, 10 + yOffset);
            DrawUtil.drawCenteredText(context, String.valueOf(score1), FIELD_WIDTH / 4 + xOffset, 20 + yOffset);

            DrawUtil.drawCenteredText(context, players.get(1), FIELD_WIDTH * 3 / 4 + xOffset, 10 + yOffset);
            DrawUtil.drawCenteredText(context, String.valueOf(score2), FIELD_WIDTH * 3 / 4 + xOffset, 20 + yOffset);
        } else {
            // Отрисовка экрана окончания игры
            String text = winnerName;
            String scoreText = score1 + " : " + score2;

            int centerX = width / 2;
            int centerY = height / 2;

            DrawUtil.drawBigCenteredText(context, text, centerX, centerY - 10);
            DrawUtil.drawBigCenteredText(context, scoreText, centerX, centerY + 10);
        }
    }

    public static void onGetPaddleData(PongMovePaddleData paddleData) {
        if (paddleData.leftPaddle()) {
            paddle1Y = paddleData.y();
        } else {
            paddle2Y = paddleData.y();
        }
    }

    public static void onGetBallData(PongMoveBallData ballData) {
        // Принимаем данные мяча от хоста (у клиента)
        // Если произошло изменение направления по X или Y — играем звук
        boolean bounced = false;

        if ((ballData.ballX() != ballX) || (ballData.ballY() != ballY)) {
            // Определим направление движения
            float newVelX = ballData.ballX() - ballX;
            float newVelY = ballData.ballY() - ballY;

            if ((newVelX * lastBallVelX < 0) || (newVelY * lastBallVelY < 0)) {
                bounced = true;
            }

            ballX = ballData.ballX();
            ballY = ballData.ballY();

            lastBallVelX = newVelX;
            lastBallVelY = newVelY;

            if (bounced) {
                // Запускаем проигрывание звука в основном клиентском потоке
                mc.execute(SoundLibrary::ponkSound);
            }
        }
    }

    public static void onGetScoreData(PongScoreData scoreData) {
        if (!(mc.currentScreen instanceof PongScreen screen)) {
            return;
        }

        screen.score1 = scoreData.firstPlayerScore();
        screen.score2 = scoreData.secondPlayerScore();

        // Если кто-то достиг 21 — отмечаем конец игры
        if (screen.score1 >= 21 || screen.score2 >= 21) {
            screen.gameEnded = true;
            screen.winnerName = screen.score1 >= 21 ? screen.players.get(0) : screen.players.get(1);
        }
    }

    @Override
    public void tick() {
        if (players.isEmpty()) return;
        if (gameEnded) return; // После окончания игры логика не работает

        boolean isHost = playerName.equals(hostName);
        if (!players.contains(playerName)) return;

        // Управление ракетками
        if (isHost) {
            int newPaddle1Y = paddle1Y;
            if (NoDelayKeys.isW_Pressed()) newPaddle1Y = Math.max(0, newPaddle1Y - PADDLE_SPEED);
            if (NoDelayKeys.isS_Pressed())
                newPaddle1Y = Math.min(FIELD_HEIGHT - PADDLE_HEIGHT, newPaddle1Y + PADDLE_SPEED);
            if (newPaddle1Y != paddle1Y) {
                paddle1Y = newPaddle1Y;
                LocalVoicePacket.send(new PongMovePaddleData(true, paddle1Y));
            }
        } else {
            int newPaddle2Y = paddle2Y;
            if (NoDelayKeys.isW_Pressed()) newPaddle2Y = Math.max(0, newPaddle2Y - PADDLE_SPEED);
            if (NoDelayKeys.isS_Pressed())
                newPaddle2Y = Math.min(FIELD_HEIGHT - PADDLE_HEIGHT, newPaddle2Y + PADDLE_SPEED);
            if (newPaddle2Y != paddle2Y) {
                paddle2Y = newPaddle2Y;
                LocalVoicePacket.send(new PongMovePaddleData(false, paddle2Y));
            }
        }

        // Логика мяча — только у хоста
        if (isHost) {
            ballX += (int) ballVelX;
            ballY += (int) ballVelY;

            // Отскок от верхнего/нижнего края
            boolean bounced = false;
            if (ballY <= 0) {
                ballY = 0;
                ballVelY = -ballVelY;
                addRandomnessToVelocity(5);
                bounced = true;
            } else if (ballY + BALL_SIZE >= FIELD_HEIGHT) {
                ballY = FIELD_HEIGHT - BALL_SIZE;
                ballVelY = -ballVelY;
                addRandomnessToVelocity(5);
                bounced = true;
            }

            // Отскок от левой ракетки
            if (ballX <= 10 + PADDLE_THICKNESS &&
                    ballY + BALL_SIZE >= paddle1Y &&
                    ballY <= paddle1Y + PADDLE_HEIGHT) {

                ballX = 10 + PADDLE_THICKNESS;
                ballVelX = Math.min(BALL_MAX_SPEED_X, Math.abs(ballVelX) * BALL_SPEED_INCREASE);

                float hitPos = (ballY + BALL_SIZE / 2f) - paddle1Y;
                float relativePos = (hitPos / PADDLE_HEIGHT) - 0.5f;
                ballVelY = relativePos * BALL_START_SPEED_Y * 2;

                addRandomnessToVelocity(10);
                bounced = true;
            }

            // Отскок от правой ракетки
            if (ballX + BALL_SIZE >= FIELD_WIDTH - 10 - PADDLE_THICKNESS &&
                    ballY + BALL_SIZE >= paddle2Y &&
                    ballY <= paddle2Y + PADDLE_HEIGHT) {

                ballX = FIELD_WIDTH - 10 - PADDLE_THICKNESS - BALL_SIZE;
                ballVelX = -Math.min(BALL_MAX_SPEED_X, Math.abs(ballVelX) * BALL_SPEED_INCREASE);

                float hitPos = (ballY + BALL_SIZE / 2f) - paddle2Y;
                float relativePos = (hitPos / PADDLE_HEIGHT) - 0.5f;
                ballVelY = relativePos * BALL_START_SPEED_Y * 2;

                addRandomnessToVelocity(10);
                bounced = true;
            }

            if (bounced) {
                SoundLibrary.ponkSound();
            }

            // Голы и сброс мяча
            boolean scored = false;
            if (ballX < 0) {
                score2++;
                scored = true;
                resetBall(-1);
            } else if (ballX > FIELD_WIDTH) {
                score1++;
                scored = true;
                resetBall(1);
            }

            if (scored) {
                // Отправляем обновлённый счёт всем
                LocalVoicePacket.send(new PongScoreData(score1, score2));

                // Проверяем окончание игры
                if (score1 >= 21 || score2 >= 21) {
                    gameEnded = true;
                    winnerName = score1 >= 21 ? players.get(0) : players.get(1);
                    return; // Остановить обновления мяча
                }
            }

            LocalVoicePacket.send(new PongMoveBallData(ballX, ballY));
        }
    }

    private void resetBall(int direction) {
        ballX = FIELD_WIDTH / 2 - BALL_SIZE / 2;
        ballY = FIELD_HEIGHT / 2 - BALL_SIZE / 2;
        ballVelX = BALL_START_SPEED_X * direction;
        ballVelY = BALL_START_SPEED_Y * (Math.random() > 0.5 ? 1 : -1);
    }

    private void addRandomnessToVelocity(float maxRandomAngleDegrees) {
        double speed = Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
        double angle = Math.atan2(ballVelY, ballVelX);

        double maxRad = Math.toRadians(maxRandomAngleDegrees);
        double randomAngle = (Math.random() * 2 - 1) * maxRad;

        angle += randomAngle;

        ballVelX = (float) (speed * Math.cos(angle));
        ballVelY = (float) (speed * Math.sin(angle));
    }
}



