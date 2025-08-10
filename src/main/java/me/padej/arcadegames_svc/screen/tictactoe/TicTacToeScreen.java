package me.padej.arcadegames_svc.screen.tictactoe;

import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.util.DrawUtil;
import me.padej.arcadegames_svc.voice.data.tictactoe.TicTacToeMoveData;
import me.padej.arcadegames_svc.voice.data.tictactoe.TicTacToeStateData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeScreen extends ArcadeGame {
    private final int[][] board = new int[3][3];
    private int currentPlayer = 1;
    private final int cellSize = 60;
    private int offsetX, offsetY;
    private boolean gameOver = false;
    private int winner = 0;

    private final List<String> players;
    private String playerX = null;
    private String playerO = null;
    private final BlockPos lobbyPos;

    public TicTacToeScreen(BlockPos lobbyPos) {
        super("Tic Tac Toe", 9601);
        this.lobbyPos = lobbyPos;
        this.players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();
    }

    private void assignPlayers() {
        playerX = null;
        playerO = null;
        if (!players.isEmpty()) playerX = players.get(0);
        if (players.size() > 1) playerO = players.get(1);
    }

    @Override
    protected void init() {
        offsetX = (this.width - cellSize * 3) / 2;
        offsetY = (this.height - cellSize * 3) / 2;

        // Обновляем список игроков при каждом заходе
        players.clear();
        players.addAll(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();

        clearBoard();
    }

    private void clearBoard() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                board[y][x] = 0;
            }
        }
        currentPlayer = 1;
        winner = 0;
        gameOver = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        DrawUtil.simpleDrawTexture(context, TicTacToeTextures.OUTLINE, width / 2 - 99, height / 2 - 99, 198, 198);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int px = offsetX + x * cellSize;
                int py = offsetY + y * cellSize;
                context.fill(px, py, px + cellSize, py + cellSize, 0xFFCCCCCC);

                int cellValue = board[y][x];
                if (cellValue == 1) {
                    // Рисуем X с масштабом 4
                    int texSize = 7 * 4; // 28 пикселей
                    int drawX = px + (cellSize - texSize) / 2;
                    int drawY = py + (cellSize - texSize) / 2;
                    DrawUtil.simpleDrawTexture(context, TicTacToeTextures.X, drawX, drawY, texSize, texSize);
                } else if (cellValue == 2) {
                    // Рисуем O с масштабом 4
                    int texSize = 7 * 4;
                    int drawX = px + (cellSize - texSize) / 2;
                    int drawY = py + (cellSize - texSize) / 2;
                    DrawUtil.simpleDrawTexture(context, TicTacToeTextures.O, drawX, drawY, texSize, texSize);
                }
            }
        }

        String info;
        if (gameOver) {
            info = (winner == 3) ? "Ничья" : ((winner == 1) ? "X победил" : "O победил");
        } else {
            info = "Ход: " + (currentPlayer == 1 ? "X" : "O");
        }
        context.drawText(this.textRenderer, info, this.width / 2 - info.length() * 3, offsetY + cellSize * 3 + 10, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (gameOver) {
            if (isHost()) {
                clearBoard();
                broadcastState();
            }
            return true;
        }

        int gridX = (int) ((mouseX - offsetX) / cellSize);
        int gridY = (int) ((mouseY - offsetY) / cellSize);

        if (gridX >= 0 && gridX < 3 && gridY >= 0 && gridY < 3) {
            int localPlayerId = getLocalPlayerId();
            if (localPlayerId == 0) return true; // не участвующий

            if (isHost()) {
                if (localPlayerId == currentPlayer) {
                    processMove(gridX, gridY, currentPlayer);
                }
            } else {
                if (localPlayerId == currentPlayer) {
                    LocalVoicePacket.send(new TicTacToeMoveData(gridX, gridY, localPlayerId));
                }
            }
        }
        return true;
    }

    private void processMove(int x, int y, int player) {
        if (board[y][x] != 0 || player != currentPlayer || gameOver) return;

        board[y][x] = player;

        if (checkWin(player)) {
            winner = player;
            gameOver = true;
        } else if (isBoardFull()) {
            winner = 3;
            gameOver = true;
        } else {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        broadcastState();
    }

    private boolean checkWin(int player) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) return true;
            if (board[0][i] == player && board[1][i] == player && board[2][i] == player) return true;
        }
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) return true;
        return board[0][2] == player && board[1][1] == player && board[2][0] == player;
    }

    private boolean isBoardFull() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (board[y][x] == 0) return false;
            }
        }
        return true;
    }

    private void broadcastState() {
        int[] cells = new int[9];
        for (int i = 0; i < 9; i++) {
            cells[i] = board[i / 3][i % 3];
        }
        LocalVoicePacket.send(new TicTacToeStateData(cells, currentPlayer, winner));
    }

    public void onRemoteMove(TicTacToeMoveData move) {
        if (!isHost()) return;
        if (move.playerId() != currentPlayer) return;
        processMove(move.cellX(), move.cellY(), move.playerId());
    }

    public void onRemoteState(TicTacToeStateData state) {
        for (int i = 0; i < 9; i++) {
            board[i / 3][i % 3] = state.cells()[i];
        }
        currentPlayer = state.currentPlayer();
        winner = state.winner();
        gameOver = (winner != 0);
    }

    private int getLocalPlayerId() {
        String localName = mc.player.getGameProfile().getName();
        if (localName.equals(playerX)) return 1;
        if (localName.equals(playerO)) return 2;
        return 0;
    }

    private boolean isHost() {
        return players.isEmpty() || players.getFirst().equals(mc.player.getGameProfile().getName());
    }
}

