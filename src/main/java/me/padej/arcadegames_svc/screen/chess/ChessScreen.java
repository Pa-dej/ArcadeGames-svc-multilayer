package me.padej.arcadegames_svc.screen.chess;

import me.padej.arcadegames_svc.lobby.LobbyManager;
import me.padej.arcadegames_svc.screen.ArcadeGame;
import me.padej.arcadegames_svc.util.DrawUtil;
import me.padej.arcadegames_svc.voice.data.chess.ChessMoveData;
import me.padej.arcadegames_svc.voice.data.chess.ChessStateData;
import me.padej.arcadegames_svc.voice.packet.LocalVoicePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ChessScreen extends ArcadeGame {
    private static final int WHITE_CELL_COLOR = 0xFF_c3d7d8;
    private static final int BLACK_CELL_COLOR = 0xFF_4e5e80;
    private static final int CELL_SIZE = 30;
    private static final int CELLS_PER_CHUNK = 11;

    private final int[][] board = new int[8][8];
    private final float[][] pieceRenderX = new float[8][8];
    private final float[][] pieceRenderY = new float[8][8];
    private final int[][] pendingBoard = new int[8][8];
    private int receivedChunkMask = 0;

    private int currentPlayer = 1;
    private int selectedX = -1, selectedY = -1;
    private boolean gameOver = false;
    private int winner = 0;
    private boolean whiteKingMoved = false;
    private boolean whiteRookA1Moved = false;
    private boolean whiteRookH1Moved = false;
    private boolean blackKingMoved = false;
    private boolean blackRookA8Moved = false;
    private boolean blackRookH8Moved = false;
    private final List<String> players;
    private String whitePlayer = null;
    private String blackPlayer = null;
    private final BlockPos lobbyPos;
    private int offsetX, offsetY;
    private final boolean[][] possibleMoveHighlights = new boolean[8][8];
    private final boolean[][] captureHighlights = new boolean[8][8];
    private final boolean[][] castlingHighlights = new boolean[8][8];

    public ChessScreen(BlockPos lobbyPos) {
        super("Chess", 9602);
        this.lobbyPos = lobbyPos;
        this.players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();
        initBoard();
    }

    private void assignPlayers() {
        whitePlayer = null;
        blackPlayer = null;
        if (!players.isEmpty()) whitePlayer = players.get(0);
        if (players.size() > 1) blackPlayer = players.get(1);
    }

    private void initBoard() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board[y][x] = 0;
                pieceRenderX[y][x] = x;
                pieceRenderY[y][x] = y;
            }
        }
        board[7][0] = board[7][7] = 1;
        board[7][1] = board[7][6] = 2;
        board[7][2] = board[7][5] = 3;
        board[7][3] = 4;
        board[7][4] = 5;
        for (int x = 0; x < 8; x++) board[6][x] = 6;
        board[0][0] = board[0][7] = 7;
        board[0][1] = board[0][6] = 8;
        board[0][2] = board[0][5] = 9;
        board[0][3] = 10;
        board[0][4] = 11;
        for (int x = 0; x < 8; x++) board[1][x] = 12;
        whiteKingMoved = false;
        whiteRookA1Moved = false;
        whiteRookH1Moved = false;
        blackKingMoved = false;
        blackRookA8Moved = false;
        blackRookH8Moved = false;
    }

    @Override
    protected void init() {
        offsetX = (this.width - CELL_SIZE * 8) / 2;
        offsetY = (this.height - CELL_SIZE * 8) / 2;
        players.clear();
        players.addAll(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();
        if (isHost()) {
            initBoard();
            broadcastState();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int px = offsetX + x * CELL_SIZE;
                int py = offsetY + y * CELL_SIZE;
                int color = (x + y) % 2 == 0 ? WHITE_CELL_COLOR : BLACK_CELL_COLOR;
                context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
            }
        }

        if (selectedX != -1) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    if (possibleMoveHighlights[y][x]) {
                        int px = offsetX + x * CELL_SIZE;
                        int py = offsetY + y * CELL_SIZE;
                        int highlightColor;
                        if (castlingHighlights[y][x]) {
                            highlightColor = 0x808000FF;
                        } else if (captureHighlights[y][x]) {
                            highlightColor = 0x80FF0000;
                        } else {
                            highlightColor = 0x8000FF00;
                        }
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, highlightColor);
                    }
                }
            }
        }

        MatrixStack matrixStack = context.getMatrices();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = board[y][x];
                if (piece != 0) {
                    pieceRenderX[y][x] = MathHelper.lerp(delta * 0.25f, pieceRenderX[y][x], x);
                    pieceRenderY[y][x] = MathHelper.lerp(delta * 0.25f, pieceRenderY[y][x], y);

                    float pixelX = pieceRenderX[y][x] * CELL_SIZE + offsetX + 3;
                    float pixelY = pieceRenderY[y][x] * CELL_SIZE + offsetY + 3;
                    int baseX = (int) pixelX;
                    int baseY = (int) pixelY;
                    float fracX = pixelX - baseX;
                    float fracY = pixelY - baseY;
                    int texSize = CELL_SIZE - 5;

                    matrixStack.push();
                    matrixStack.translate(fracX, fracY, 0);

                    switch (piece) {
                        case 1 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_ROOK, baseX, baseY, texSize, texSize);
                        case 2 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_KNIGHT, baseX, baseY, texSize, texSize);
                        case 3 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_BISHOP, baseX, baseY, texSize, texSize);
                        case 4 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_QUEEN, baseX, baseY, texSize, texSize);
                        case 5 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_KING, baseX, baseY, texSize, texSize);
                        case 6 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_PAWN, baseX, baseY, texSize, texSize);
                        case 7 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_ROOK, baseX, baseY, texSize, texSize);
                        case 8 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_KNIGHT, baseX, baseY, texSize, texSize);
                        case 9 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_BISHOP, baseX, baseY, texSize, texSize);
                        case 10 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_QUEEN, baseX, baseY, texSize, texSize);
                        case 11 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_KING, baseX, baseY, texSize, texSize);
                        case 12 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_PAWN, baseX, baseY, texSize, texSize);
                    }

                    matrixStack.pop();
                }
            }
        }

        String info;
        if (gameOver) {
            info = (winner == 3) ? "Draw" : (winner == 1 ? "White wins" : "Black wins");
        } else {
            info = "Turn: " + (currentPlayer == 1 ? "White" : "Black");
        }
        context.drawText(this.textRenderer, info, this.width / 2 - info.length() * 3, offsetY + CELL_SIZE * 8 + 5, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (gameOver) {
            if (isHost()) {
                initBoard();
                currentPlayer = 1;
                winner = 0;
                gameOver = false;
                broadcastState();
            }
            return true;
        }

        int gridX = (int) ((mouseX - offsetX) / CELL_SIZE);
        int gridY = (int) ((mouseY - offsetY) / CELL_SIZE);

        if (gridX >= 0 && gridX < 8 && gridY >= 0 && gridY < 8) {
            int localPlayerId = getLocalPlayerId();
            if (localPlayerId == 0) return true;

            if (selectedX == -1 && selectedY == -1) {
                if (localPlayerId == currentPlayer && isValidPiece(gridX, gridY, localPlayerId)) {
                    selectedX = gridX;
                    selectedY = gridY;
                    updateHighlights(localPlayerId);
                }
            } else {
                if (localPlayerId == currentPlayer) {
                    if (isHost()) {
                        processMove(selectedX, selectedY, gridX, gridY, localPlayerId);
                    } else {
                        LocalVoicePacket.send(new ChessMoveData(selectedX, selectedY, gridX, gridY, localPlayerId));
                    }
                }
                selectedX = -1;
                selectedY = -1;
            }
        }
        return true;
    }

    private void updateHighlights(int player) {
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                boolean isValid = isValidMove(selectedX, selectedY, tx, ty, player);
                possibleMoveHighlights[ty][tx] = isValid;
                captureHighlights[ty][tx] = isValid && board[ty][tx] != 0;
                int piece = board[selectedY][selectedX];
                int dx = Math.abs(tx - selectedX);
                int dy = Math.abs(ty - selectedY);
                castlingHighlights[ty][tx] = isValid && (piece == 5 || piece == 11) && dy == 0 && dx == 2;
            }
        }
    }

    private boolean isValidPiece(int x, int y, int player) {
        int piece = board[y][x];
        return (player == 1 && piece >= 1 && piece <= 6) || (player == 2 && piece >= 7 && piece <= 12);
    }

    private void processMove(int fromX, int fromY, int toX, int toY, int player) {
        if (!isValidMove(fromX, fromY, toX, toY, player)) return;

        int piece = board[fromY][fromX];
        int targetPiece = board[toY][toX];
        board[toY][toX] = piece;
        board[fromY][fromX] = 0;

        pieceRenderX[toY][toX] = fromX;
        pieceRenderY[toY][toX] = fromY;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        if ((piece == 5 || piece == 11) && dy == 0 && dx == 2) {
            int rookFromX = (toX > fromX) ? 7 : 0;
            int rookToX = (toX > fromX) ? fromX + 1 : fromX - 1;
            board[fromY][rookToX] = board[fromY][rookFromX];
            board[fromY][rookFromX] = 0;
            pieceRenderX[fromY][rookToX] = rookFromX;
            pieceRenderY[fromY][rookToX] = fromY;
            if (player == 1) {
                if (rookFromX == 0) whiteRookA1Moved = true;
                else whiteRookH1Moved = true;
            } else {
                if (rookFromX == 0) blackRookA8Moved = true;
                else blackRookH8Moved = true;
            }
        }

        if (piece == 5) whiteKingMoved = true;
        else if (piece == 11) blackKingMoved = true;
        else if (piece == 1) {
            if (fromX == 0 && fromY == 7) whiteRookA1Moved = true;
            else if (fromX == 7 && fromY == 7) whiteRookH1Moved = true;
        } else if (piece == 7) {
            if (fromX == 0 && fromY == 0) blackRookA8Moved = true;
            else if (fromX == 7 && fromY == 0) blackRookH8Moved = true;
        }

        if ((player == 1 && targetPiece == 11) || (player == 2 && targetPiece == 5)) {
            winner = player;
            gameOver = true;
        } else {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        if (findKing(3 - player) == null) {
            winner = player;
            gameOver = true;
        }

        broadcastState();
    }

    private int[] findKing(int player) {
        int king = (player == 1) ? 5 : 11;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board[y][x] == king) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private boolean isValidMove(int fromX, int fromY, int toX, int toY, int player) {
        if (!isValidPiece(fromX, fromY, player)) return false;
        if (fromX == toX && fromY == toY) return false;

        int piece = board[fromY][fromX];
        int targetPiece = board[toY][toX];
        if (player == 1 && targetPiece >= 1 && targetPiece <= 6) return false;
        if (player == 2 && targetPiece >= 7 && targetPiece <= 12) return false;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);

        return switch (piece) {
            case 1, 7 -> (dx == 0 || dy == 0) && isPathClear(fromX, fromY, toX, toY);
            case 2, 8 -> (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
            case 3, 9 -> dx == dy && isPathClear(fromX, fromY, toX, toY);
            case 4, 10 -> (dx == dy || dx == 0 || dy == 0) && isPathClear(fromX, fromY, toX, toY);
            case 5 -> (dx <= 1 && dy <= 1) || isCastlingWhite(fromX, fromY, toX, toY);
            case 11 -> (dx <= 1 && dy <= 1) || isCastlingBlack(fromX, fromY, toX, toY);
            case 6 -> (player == 1 && fromY == 6 && toY == 4 && dx == 0 && board[toY][toX] == 0) ||
                    (player == 1 && toY == fromY - 1 && dx == 0 && board[toY][toX] == 0) ||
                    (player == 1 && toY == fromY - 1 && dx == 1 && board[toY][toX] >= 7);
            case 12 -> (player == 2 && fromY == 1 && toY == 3 && dx == 0 && board[toY][toX] == 0) ||
                    (player == 2 && toY == fromY + 1 && dx == 0 && board[toY][toX] == 0) ||
                    (player == 2 && toY == fromY + 1 && dx == 1 && board[toY][toX] <= 6 && board[toY][toX] > 0);
            default -> false;
        };
    }

    private boolean isCastlingWhite(int fromX, int fromY, int toX, int toY) {
        if (whiteKingMoved || fromY != 7 || fromX != 4 || toY != 7 || Math.abs(toX - fromX) != 2) return false;
        if (toX == 6) return !whiteRookH1Moved && board[7][5] == 0 && board[7][6] == 0;
        else if (toX == 2) return !whiteRookA1Moved && board[7][1] == 0 && board[7][2] == 0 && board[7][3] == 0;
        return false;
    }

    private boolean isCastlingBlack(int fromX, int fromY, int toX, int toY) {
        if (blackKingMoved || fromY != 0 || fromX != 4 || toY != 0 || Math.abs(toX - fromX) != 2) return false;
        if (toX == 6) return !blackRookH8Moved && board[0][5] == 0 && board[0][6] == 0;
        else if (toX == 2) return !blackRookA8Moved && board[0][1] == 0 && board[0][2] == 0 && board[0][3] == 0;
        return false;
    }

    private boolean isPathClear(int fromX, int fromY, int toX, int toY) {
        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        int x = fromX + dx;
        int y = fromY + dy;
        while (x != toX || y != toY) {
            if (board[y][x] != 0) return false;
            x += dx;
            y += dy;
        }
        return true;
    }

    private void broadcastState() {
        int[] cells = new int[64];
        for (int i = 0; i < 64; i++) {
            cells[i] = board[i / 8][i % 8];
        }
        for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
            int start = chunkIndex * CELLS_PER_CHUNK;
            int end = Math.min(start + CELLS_PER_CHUNK, 64);
            int chunkSize = end - start;
            int[] cellsChunk = new int[chunkSize];
            if (chunkSize > 0) {
                System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
            }
            LocalVoicePacket.send(new ChessStateData(chunkIndex, cellsChunk, currentPlayer, winner, whiteKingMoved, blackKingMoved, whiteRookA1Moved, whiteRookH1Moved, blackRookA8Moved, blackRookH8Moved));
        }
    }

    public void onRemoteMove(ChessMoveData move) {
        if (!isHost()) return;
        if (move.playerId() != currentPlayer) return;
        processMove(move.fromX(), move.fromY(), move.toX(), move.toY(), move.playerId());
    }

    public void onRemoteState(ChessStateData state) {
        int chunkIndex = state.chunkIndex();
        int start = chunkIndex * CELLS_PER_CHUNK;
        int[] cellsChunk = state.cellsChunk();
        for (int i = 0; i < cellsChunk.length && start + i < 64; i++) {
            pendingBoard[(start + i) / 8][(start + i) % 8] = cellsChunk[i];
        }
        receivedChunkMask |= (1 << chunkIndex);
        currentPlayer = state.currentPlayer();
        winner = state.winner();
        gameOver = (winner != 0);
        whiteKingMoved = state.whiteKingMoved();
        blackKingMoved = state.blackKingMoved();
        whiteRookA1Moved = state.whiteRookA1Moved();
        whiteRookH1Moved = state.whiteRookH1Moved();
        blackRookA8Moved = state.blackRookA8Moved();
        blackRookH8Moved = state.blackRookH8Moved();

        if (receivedChunkMask == 63) {
            applyPendingBoard();
            receivedChunkMask = 0;
        }
    }

    private void applyPendingBoard() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (pendingBoard[y][x] != board[y][x] && pendingBoard[y][x] != 0) {
                    boolean found = false;
                    for (int fy = 0; fy < 8 && !found; fy++) {
                        for (int fx = 0; fx < 8; fx++) {
                            if (board[fy][fx] == pendingBoard[y][x] && pendingBoard[fy][fx] == 0) {
                                pieceRenderX[y][x] = fx;
                                pieceRenderY[y][x] = fy;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        pieceRenderX[y][x] = x;
                        pieceRenderY[y][x] = y;
                    }
                }
            }
        }
        for (int y = 0; y < 8; y++) {
            System.arraycopy(pendingBoard[y], 0, board[y], 0, 8);
        }
    }

    private int getLocalPlayerId() {
        if (mc.player == null) return 0;
        String localName = mc.player.getGameProfile().getName();
        if (localName.equals(whitePlayer)) return 1;
        if (localName.equals(blackPlayer)) return 2;
        return 0;
    }

    private boolean isHost() {
        if (mc.player == null) return false;
        return players.isEmpty() || players.getFirst().equals(mc.player.getGameProfile().getName());
    }
}

