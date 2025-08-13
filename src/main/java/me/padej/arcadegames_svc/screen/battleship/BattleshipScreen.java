package me.padej.arcadegames_svc.screen.battleship;

import me.padej.arcadegames_svc.ArcadeGames;
import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.util.DrawUtil;
import me.padej.arcadegames_svc.voice.data.battleship.BattleshipMoveData;
import me.padej.arcadegames_svc.voice.data.battleship.BattleshipStateData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BattleshipScreen extends ArcadeGame {
    private static final int WATER_COLOR = 0xFF_4682B4;
    private static final int SHIP_COLOR = 0xFF_808080;
    private static final int HIT_COLOR = 0xFF_FF0000;
    private static final int MISS_COLOR = 0xFF_FFFFFF;
    private static final int VALID_PREVIEW_COLOR = 0xFF_00FF00;
    private static final int INVALID_PREVIEW_COLOR = 0xFF_FF0000;
    private static final Vector2i BACKGROUND_SCALE = new Vector2i(450, 271);
    private static final Vector2i OUTLINE_SCALE = new Vector2i(468, 289);
    private static final int CELL_SIZE = 30;
    private static final int CELLS_PER_CHUNK = 17;
    private static final int GRID_OFFSET = 50;

    private final BattleshipGameLogic gameLogic;
    private final List<String> players;
    private String player1 = null;
    private String player2 = null;
    private final BlockPos lobbyPos;
    private int offsetX, offsetY;
    private int receivedChunkMask = 0;
    private final int[][] pendingPlayer1Grid = new int[10][10];
    private final int[][] pendingPlayer2Grid = new int[10][10];
    private final int[][] pendingPlayer1View = new int[10][10];
    private final int[][] pendingPlayer2View = new int[10][10];
    private final List<BattleshipMoveData> moveHistory = new ArrayList<>();
    private boolean localSetupComplete = false;

    public BattleshipScreen(BlockPos lobbyPos) {
        super("Battleship", 9603);
        this.lobbyPos = lobbyPos;
        this.players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));
        this.gameLogic = new BattleshipGameLogic();
        assignPlayers();
        initGame();
    }

    private void assignPlayers() {
        player1 = null;
        player2 = null;
        if (!players.isEmpty()) player1 = players.get(0);
        if (players.size() > 1) player2 = players.get(1);
    }

    private void initGame() {
        gameLogic.initGame();
        receivedChunkMask = 0;
        moveHistory.clear();
        localSetupComplete = false;
    }

    @Override
    protected void init() {
        offsetX = (this.width - (CELL_SIZE * 10 * 2 + GRID_OFFSET)) / 2;
        offsetY = (this.height - CELL_SIZE * 10) / 2;
        players.clear();
        players.addAll(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();
        if (isHost()) {
            if (!loadGameState()) {
                initGame();
            }
            broadcastState();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int halfW = width / 2;
        int halfH = height / 2;
        context.fill(
                halfW - BACKGROUND_SCALE.x / 2,
                halfH - BACKGROUND_SCALE.y / 2,
                halfW - BACKGROUND_SCALE.x / 2 + BACKGROUND_SCALE.x,
                halfH - BACKGROUND_SCALE.y / 2 + BACKGROUND_SCALE.y,
                0xDE_2b2d30
        );
        DrawUtil.simpleDrawTexture(
                context,
                BattleshipTextures.OUTLINE,
                halfW - OUTLINE_SCALE.x / 2,
                halfH - OUTLINE_SCALE.y / 2,
                OUTLINE_SCALE.x,
                OUTLINE_SCALE.y
        );

        int localPlayerId = getLocalPlayerId();
        boolean isFlipped = localPlayerId == 2;
        int[][] ownGrid = localPlayerId == 1 ? gameLogic.getPlayerGrid(1) : gameLogic.getPlayerGrid(2);
        int[][] opponentView = localPlayerId == 1 ? gameLogic.getViewGrid(1) : gameLogic.getViewGrid(2);

        // Render own grid (left)
        for (int ry = 0; ry < 10; ry++) {
            int ly = isFlipped ? 9 - ry : ry;
            for (int rx = 0; rx < 10; rx++) {
                int px = offsetX + rx * CELL_SIZE;
                int py = offsetY + ry * CELL_SIZE;
                int cell = ownGrid[ly][rx];
                int color = switch (cell) {
                    case 1, 2, 3, 4, 5 -> SHIP_COLOR;
                    case 6 -> MISS_COLOR;
                    case 7 -> HIT_COLOR;
                    default -> WATER_COLOR;
                };
                context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
            }
        }

        // Render opponent's grid (right)
        for (int ry = 0; ry < 10; ry++) {
            int ly = isFlipped ? 9 - ry : ry;
            for (int rx = 0; rx < 10; rx++) {
                int px = offsetX + (rx + 10 + GRID_OFFSET / CELL_SIZE) * CELL_SIZE;
                int py = offsetY + ry * CELL_SIZE;
                int cell = opponentView[ly][rx];
                int color = switch (cell) {
                    case 6 -> MISS_COLOR;
                    case 7 -> HIT_COLOR;
                    default -> WATER_COLOR;
                };
                context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
            }
        }

        // Render ship placement preview during setup phase
        if (gameLogic.isSetupPhase() && localPlayerId != 0 && !localSetupComplete) {
            int renderGridX = (mouseX - offsetX) / CELL_SIZE;
            int renderGridY = (mouseY - offsetY) / CELL_SIZE;
            int gridY = isFlipped ? 9 - renderGridY : renderGridY;
            if (renderGridX >= 0 && renderGridX < 10 && gridY >= 0 && gridY < 10) {
                int shipType = gameLogic.getCurrentShipType();
                int[] ships = gameLogic.getPlayerShips(localPlayerId);
                int shipSize = ships[shipType];
                boolean isHorizontal = gameLogic.isCurrentShipHorizontal();
                boolean isValid = gameLogic.isValidShipPlacement(renderGridX, gridY, localPlayerId, shipType, isHorizontal);
                int color = isValid ? VALID_PREVIEW_COLOR : INVALID_PREVIEW_COLOR;

                if (isHorizontal) {
                    for (int i = 0; i < shipSize && renderGridX + i < 10; i++) {
                        int px = offsetX + (renderGridX + i) * CELL_SIZE;
                        int py = offsetY + renderGridY * CELL_SIZE;
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
                    }
                } else {
                    for (int i = 0; i < shipSize && renderGridY + i < 10; i++) {
                        int px = offsetX + renderGridX * CELL_SIZE;
                        int py = offsetY + (renderGridY + i) * CELL_SIZE;
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
                    }
                }
            }
        }

        // Render coordinates
        String letters = "ABCDEFGHIJ";
        for (int i = 0; i < 10; i++) {
            String letterStr = String.valueOf(letters.charAt(i));
            int pxLetterOwn = offsetX + i * CELL_SIZE + CELL_SIZE / 2;
            int pyLetterOwn = offsetY - 10;
            context.drawText(this.textRenderer, letterStr, pxLetterOwn, pyLetterOwn, 0xFFFFFF, false);
            int pxLetterOpp = offsetX + (i + 10 + GRID_OFFSET / CELL_SIZE) * CELL_SIZE + CELL_SIZE / 2;
            context.drawText(this.textRenderer, letterStr, pxLetterOpp, pyLetterOwn, 0xFFFFFF, false);
            String numStr = isFlipped ? Integer.toString(i + 1) : Integer.toString(10 - i);
            int pxNumber = offsetX - 20;
            int pyNumber = offsetY + i * CELL_SIZE + CELL_SIZE / 4;
            context.drawText(this.textRenderer, numStr, pxNumber, pyNumber, 0xFFFFFF, false);
            int pxNumberOpp = offsetX + (10 + GRID_OFFSET / CELL_SIZE) * CELL_SIZE - 20;
            context.drawText(this.textRenderer, numStr, pxNumberOpp, pyNumber, 0xFFFFFF, false);
        }

        // Render game status
        if (gameLogic.isSetupPhase()) {
            String status = localSetupComplete ? "Waiting for opponent..." : (
                    switch (gameLogic.getCurrentShipType()) {
                        case 1 -> "Place Carrier (5) (" + (gameLogic.isCurrentShipHorizontal() ? "Horizontal" : "Vertical") + ")";
                        case 2 -> "Place Battleship (4) (" + (gameLogic.isCurrentShipHorizontal() ? "Horizontal" : "Vertical") + ")";
                        case 3 -> "Place Cruiser (3) (" + (gameLogic.isCurrentShipHorizontal() ? "Horizontal" : "Vertical") + ")";
                        case 4 -> "Place Submarine (3) (" + (gameLogic.isCurrentShipHorizontal() ? "Horizontal" : "Vertical") + ")";
                        case 5 -> "Place Destroyer (2) (" + (gameLogic.isCurrentShipHorizontal() ? "Horizontal" : "Vertical") + ")";
                        default -> "";
                    }
            );
            context.drawText(this.textRenderer, status, halfW - status.length() * 3, offsetY + CELL_SIZE * 10 + 5, 0xFFFFFF, false);
        } else {
            String info = gameLogic.isGameOver() ?
                    (gameLogic.getWinner() == 0 ? "Draw" : "Player " + gameLogic.getWinner() + " wins") :
                    "Turn: Player " + gameLogic.getCurrentPlayer();
            context.drawText(this.textRenderer, info, halfW - info.length() * 3, offsetY + CELL_SIZE * 10 + 5, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (gameLogic.isGameOver()) {
            if (isHost()) {
                initGame();
                broadcastState();
            }
            return true;
        }

        int localPlayerId = getLocalPlayerId();
        if (localPlayerId == 0) return true;

        int renderGridX = (int) ((mouseX - offsetX) / CELL_SIZE);
        int renderGridY = (int) ((mouseY - offsetY) / CELL_SIZE);
        boolean isFlipped = localPlayerId == 2;
        int gridY = isFlipped ? 9 - renderGridY : renderGridY;

        if (gameLogic.isSetupPhase() && !localSetupComplete) {
            if (renderGridX >= 0 && renderGridX < 10 && gridY >= 0 && gridY < 10) {
                BattleshipMoveData move = new BattleshipMoveData(renderGridX, gridY, localPlayerId,
                        gameLogic.getCurrentShipType(), gameLogic.isCurrentShipHorizontal(), true);
                if (gameLogic.placeShip(renderGridX, gridY, localPlayerId, move.shipType(), move.isHorizontal())) {
                    gameLogic.advanceSetupPhase(localPlayerId);
                    if (allShipsPlaced(localPlayerId)) {
                        localSetupComplete = true;
                        sendGridToHost(localPlayerId);
                    }
                    if (isHost()) {
                        broadcastState();
                    }
                }
            }
            return true;
        }

        if (!gameLogic.isSetupPhase() && renderGridX >= 10 + GRID_OFFSET / CELL_SIZE && renderGridX < 20 + GRID_OFFSET / CELL_SIZE && gridY >= 0 && gridY < 10) {
            int attackX = renderGridX - (10 + GRID_OFFSET / CELL_SIZE);
            if (localPlayerId == gameLogic.getCurrentPlayer()) {
                BattleshipMoveData move = new BattleshipMoveData(attackX, gridY, localPlayerId, 0, false, false);
                if (isHost()) {
                    if (gameLogic.processMove(attackX, gridY, localPlayerId)) {
                        moveHistory.add(move);
                        broadcastState();
                        saveGameState();
                    }
                } else {
                    LocalVoicePacket.send(move);
                }
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (gameLogic.isSetupPhase() && !localSetupComplete && keyCode == GLFW.GLFW_KEY_R) {
            gameLogic.toggleShipOrientation();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (isHost() && !gameLogic.isGameOver()) {
            saveGameState();
        }
        super.close();
    }

    private void broadcastState() {
        int[][][] grids = {
                gameLogic.getPlayerGrid(1),
                gameLogic.getPlayerGrid(2),
                gameLogic.getViewGrid(1),
                gameLogic.getViewGrid(2)
        };
        for (int g = 0; g < 4; g++) {
            int[] cells = new int[100];
            for (int y = 0; y < 10; y++) {
                System.arraycopy(grids[g][y], 0, cells, y * 10, 10);
            }
            for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
                int start = chunkIndex * CELLS_PER_CHUNK;
                int end = Math.min(start + CELLS_PER_CHUNK, 100);
                int chunkSize = end - start;
                int[] cellsChunk = new int[chunkSize];
                if (chunkSize > 0) {
                    System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
                }
                LocalVoicePacket.send(new BattleshipStateData(
                        chunkIndex + g * 6,
                        cellsChunk,
                        gameLogic.getCurrentPlayer(),
                        gameLogic.getWinner(),
                        gameLogic.isGameOver(),
                        gameLogic.isSetupPhase()
                ));
            }
        }
    }

    private void sendGridToHost(int playerId) {
        int[][] grid = gameLogic.getPlayerGrid(playerId);
        int[] cells = new int[100];
        for (int y = 0; y < 10; y++) {
            System.arraycopy(grid[y], 0, cells, y * 10, 10);
        }
        for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
            int start = chunkIndex * CELLS_PER_CHUNK;
            int end = Math.min(start + CELLS_PER_CHUNK, 100);
            int chunkSize = end - start;
            int[] cellsChunk = new int[chunkSize];
            if (chunkSize > 0) {
                System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
            }
            LocalVoicePacket.send(new BattleshipStateData(
                    chunkIndex + (playerId == 1 ? 0 : 6),
                    cellsChunk,
                    gameLogic.getCurrentPlayer(),
                    gameLogic.getWinner(),
                    gameLogic.isGameOver(),
                    gameLogic.isSetupPhase()
            ));
        }
    }

    private void saveGameState() {
        try {
            Path dir = Paths.get("config/arcadegames_svc/battleship");
            Files.createDirectories(dir);
            Path file = dir.resolve("battleship.dat");
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
                // Save grids
                int[][][] grids = {
                        gameLogic.getPlayerGrid(1),
                        gameLogic.getPlayerGrid(2),
                        gameLogic.getViewGrid(1),
                        gameLogic.getViewGrid(2)
                };
                for (int g = 0; g < 4; g++) {
                    int[] cells = new int[100];
                    for (int y = 0; y < 10; y++) {
                        System.arraycopy(grids[g][y], 0, cells, y * 10, 10);
                    }
                    for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
                        int start = chunkIndex * CELLS_PER_CHUNK;
                        int end = Math.min(start + CELLS_PER_CHUNK, 100);
                        int chunkSize = end - start;
                        int[] cellsChunk = new int[chunkSize];
                        if (chunkSize > 0) {
                            System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
                        }
                        BattleshipStateData state = new BattleshipStateData(
                                chunkIndex + g * 6,
                                cellsChunk,
                                gameLogic.getCurrentPlayer(),
                                gameLogic.getWinner(),
                                gameLogic.isGameOver(),
                                gameLogic.isSetupPhase()
                        );
                        out.writeLong(state.pack());
                    }
                }
                // Save move history
                out.writeInt(moveHistory.size());
                for (BattleshipMoveData move : moveHistory) {
                    out.writeLong(move.pack());
                }
                // Save ready states
                out.writeBoolean(gameLogic.isPlayerReady(1));
                out.writeBoolean(gameLogic.isPlayerReady(2));
            }
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("saveGameState: {}", String.valueOf(e));
        }
    }

    private boolean loadGameState() {
        Path file = Paths.get("config/arcadegames_svc/battleship/battleship.dat");
        if (!Files.exists(file)) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            // Load grids
            for (int g = 0; g < 4; g++) {
                for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
                    long packed = in.readLong();
                    BattleshipStateData state = BattleshipStateData.unpack(packed);
                    if (state.chunkIndex() != chunkIndex + g * 6) {
                        return false;
                    }
                    onRemoteState(state, g);
                }
            }
            // Load move history
            int moveCount = in.readInt();
            moveHistory.clear();
            for (int i = 0; i < moveCount; i++) {
                long packed = in.readLong();
                BattleshipMoveData move = BattleshipMoveData.unpack(packed);
                moveHistory.add(move);
                if (!gameLogic.isSetupPhase() && isHost()) {
                    gameLogic.processMove(move.x(), move.y(), move.playerId());
                }
            }
            // Load ready states
            gameLogic.setPlayerReady(1, in.readBoolean());
            gameLogic.setPlayerReady(2, in.readBoolean());
            localSetupComplete = gameLogic.isPlayerReady(getLocalPlayerId());
            return true;
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("loadGameState: {}", String.valueOf(e));
            return false;
        }
    }

    private void deleteGameState() {
        try {
            Path file = Paths.get("config/arcadegames_svc/battleship/battleship.dat");
            Files.deleteIfExists(file);
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("deleteGameState: {}", String.valueOf(e));
        }
    }

    public void onRemoteMove(BattleshipMoveData move) {
        if (!isHost()) return;
        if (move.isSetup()) {
            int[][] targetGrid = move.playerId() == 1 ? pendingPlayer1Grid : pendingPlayer2Grid;
            int[][] grid = gameLogic.getPlayerGrid(move.playerId());
            if (gameLogic.isSetupPhase() && gameLogic.placeShip(move.x(), move.y(), move.playerId(), move.shipType(), move.isHorizontal())) {
                gameLogic.advanceSetupPhase(move.playerId());
                // Copy grid to pending for state update
                for (int y = 0; y < 10; y++) {
                    System.arraycopy(grid[y], 0, targetGrid[y], 0, 10);
                }
                broadcastState();
                saveGameState();
            }
        } else if (move.playerId() == gameLogic.getCurrentPlayer()) {
            if (gameLogic.processMove(move.x(), move.y(), move.playerId())) {
                moveHistory.add(move);
                broadcastState();
                saveGameState();
            }
        }
    }

    public void onRemoteState(BattleshipStateData state, int gridIndex) {
        int chunkIndex = state.chunkIndex() % 6;
        int start = chunkIndex * CELLS_PER_CHUNK;
        int[] cellsChunk = state.cellsChunk();
        int[][] targetGrid = switch (gridIndex) {
            case 0 -> pendingPlayer1Grid;
            case 1 -> pendingPlayer2Grid;
            case 2 -> pendingPlayer1View;
            case 3 -> pendingPlayer2View;
            default -> throw new IllegalArgumentException("Invalid grid index");
        };
        for (int i = 0; i < cellsChunk.length && start + i < 100; i++) {
            targetGrid[(start + i) / 10][(start + i) % 10] = cellsChunk[i];
        }
        receivedChunkMask |= (1 << state.chunkIndex());
        gameLogic.setCurrentPlayer(state.currentPlayer());
        gameLogic.setWinner(state.winner());
        gameLogic.setGameOver(state.gameOver());
        gameLogic.setSetupPhase(state.setupPhase());
        if (receivedChunkMask == 0xFFFFFF) {
            applyPendingGrids();
            receivedChunkMask = 0;
        }
    }

    private void applyPendingGrids() {
        int[][][] grids = {
                gameLogic.getPlayerGrid(1),
                gameLogic.getPlayerGrid(2),
                gameLogic.getViewGrid(1),
                gameLogic.getViewGrid(2)
        };
        int[][][] pendingGrids = {
                pendingPlayer1Grid,
                pendingPlayer2Grid,
                pendingPlayer1View,
                pendingPlayer2View
        };
        for (int g = 0; g < 4; g++) {
            for (int y = 0; y < 10; y++) {
                System.arraycopy(pendingGrids[g][y], 0, grids[g][y], 0, 10);
            }
        }
    }

    private int getLocalPlayerId() {
        if (mc.player == null) return 0;
        String localName = mc.player.getGameProfile().getName();
        if (localName.equals(player1)) return 1;
        if (localName.equals(player2)) return 2;
        return 0;
    }

    private boolean isHost() {
        if (mc.player == null) return false;
        return players.isEmpty() || players.getFirst().equals(mc.player.getGameProfile().getName());
    }

    private boolean allShipsPlaced(int playerId) {
        int[] ships = gameLogic.getPlayerShips(playerId);
        for (int i = 1; i <= 5; i++) {
            if (ships[i] > 0) return false;
        }
        return true;
    }
}
