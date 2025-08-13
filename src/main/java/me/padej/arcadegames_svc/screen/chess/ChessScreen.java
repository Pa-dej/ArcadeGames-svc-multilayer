package me.padej.arcadegames_svc.screen.chess;

import me.padej.arcadegames_svc.ArcadeGames;
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
import org.joml.Vector2i;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessScreen extends ArcadeGame {
    private static final int WHITE_CELL_COLOR = 0xFF_c3d7d8;
    private static final int BLACK_CELL_COLOR = 0xFF_4e5e80;
    private static final Vector2i BACKGROUND_SCALE = new Vector2i(350, 271);
    private static final Vector2i OUTLINE_SCALE = new Vector2i(368, 289);
    private static final int CELL_SIZE = 30;
    private static final int CELLS_PER_CHUNK = 11;
    private static final int CAPTURED_PIECE_SIZE = 20;
    private static final int PROMOTION_BUTTON_SIZE = 30;
    private static final int PROMOTION_BUTTON_SPACING = 5;

    private final ChessGameLogic gameLogic;
    private final ChessHighlighter highlighter;
    private final float[][] pieceRenderX = new float[8][8];
    private final float[][] pieceRenderY = new float[8][8];
    private final int[][] pendingBoard = new int[8][8];
    private int receivedChunkMask = 0;
    private final List<String> players;
    private String whitePlayer = null;
    private String blackPlayer = null;
    private final BlockPos lobbyPos;
    private int offsetX, offsetY;
    private ChessMoveData pendingPromotionMove = null;
    private boolean showPromotionMenu = false;
    private int promotionX, promotionY;

    public ChessScreen(BlockPos lobbyPos) {
        super("Chess", 9602);
        this.lobbyPos = lobbyPos;
        this.players = new ArrayList<>(LobbyManager.getPlayers(lobbyPos));
        this.gameLogic = new ChessGameLogic();
        this.highlighter = new ChessHighlighter(gameLogic);
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
                pieceRenderX[y][x] = x;
                pieceRenderY[y][x] = y;
            }
        }
        gameLogic.initBoard();
        highlighter.clearHighlights();
        showPromotionMenu = false;
        pendingPromotionMove = null;
    }

    @Override
    protected void init() {
        offsetX = (this.width - CELL_SIZE * 8) / 2;
        offsetY = (this.height - CELL_SIZE * 8) / 2;
        players.clear();
        players.addAll(LobbyManager.getPlayers(lobbyPos));
        assignPlayers();
        if (isHost()) {
            if (!loadGameState()) {
                initBoard();
            }
            broadcastState();
        }
        highlighter.clearHighlights();
    }

    private Map<Integer, Integer> calculateCapturedPieces() {
        Map<Integer, Integer> pieceCounts = new HashMap<>();
        pieceCounts.put(1, 2); // White rooks
        pieceCounts.put(2, 2); // White knights
        pieceCounts.put(3, 2); // White bishops
        pieceCounts.put(4, 1); // White queen
        pieceCounts.put(5, 1); // White king
        pieceCounts.put(6, 8); // White pawns
        pieceCounts.put(7, 2); // Black rooks
        pieceCounts.put(8, 2); // Black knights
        pieceCounts.put(9, 2); // Black bishops
        pieceCounts.put(10, 1); // Black queen
        pieceCounts.put(11, 1); // Black king
        pieceCounts.put(12, 8); // Black pawns

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = gameLogic.getBoard()[y][x];
                if (piece != 0) {
                    pieceCounts.merge(piece, -1, Integer::sum);
                }
            }
        }

        int extraWhiteQueens = pieceCounts.getOrDefault(4, 0) - 1;
        int extraBlackQueens = pieceCounts.getOrDefault(10, 0) - 1;
        if (extraWhiteQueens > 0) {
            pieceCounts.merge(6, -extraWhiteQueens, Integer::sum);
            pieceCounts.put(4, 1);
        }
        if (extraBlackQueens > 0) {
            pieceCounts.merge(12, -extraBlackQueens, Integer::sum);
            pieceCounts.put(10, 1);
        }

        return pieceCounts;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean isFlipped = getLocalPlayerId() == 2;

        // Render background and outline
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
                ChessTextures.OUTLINE,
                halfW - OUTLINE_SCALE.x / 2,
                halfH - OUTLINE_SCALE.y / 2,
                OUTLINE_SCALE.x,
                OUTLINE_SCALE.y
        );

        // Render board
        for (int ry = 0; ry < 8; ry++) {
            int ly = isFlipped ? 7 - ry : ry;
            for (int rx = 0; rx < 8; rx++) {
                int px = offsetX + rx * CELL_SIZE;
                int py = offsetY + ry * CELL_SIZE;
                int color = (rx + ly) % 2 == 0 ? WHITE_CELL_COLOR : BLACK_CELL_COLOR;
                context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);
            }
        }

        // Render highlights
        if (gameLogic.getSelectedX() != -1) {
            for (int ry = 0; ry < 8; ry++) {
                int ly = isFlipped ? 7 - ry : ry;
                for (int rx = 0; rx < 8; rx++) {
                    int highlightColor = highlighter.getHighlightColor(rx, ly);
                    if (highlightColor != 0) {
                        int px = offsetX + rx * CELL_SIZE;
                        int py = offsetY + ry * CELL_SIZE;
                        context.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, highlightColor);
                    }
                }
            }
        }

        // Render pieces with animation
        MatrixStack matrixStack = context.getMatrices();
        for (int ly = 0; ly < 8; ly++) {
            for (int lx = 0; lx < 8; lx++) {
                int piece = gameLogic.getBoard()[ly][lx];
                if (piece != 0) {
                    pieceRenderX[ly][lx] = MathHelper.lerp(delta * 0.25f, pieceRenderX[ly][lx], lx);
                    pieceRenderY[ly][lx] = MathHelper.lerp(delta * 0.25f, pieceRenderY[ly][lx], ly);

                    float animLX = pieceRenderX[ly][lx];
                    float animLY = pieceRenderY[ly][lx];
                    float animRY = isFlipped ? 7 - animLY : animLY;
                    float pixelX = animLX * CELL_SIZE + offsetX + 3;
                    float pixelY = animRY * CELL_SIZE + offsetY + 3;
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

        // Render captured pieces
        Map<Integer, Integer> capturedCounts = calculateCapturedPieces();

        List<Integer> leftPieces = isFlipped ? List.of(6, 2, 3, 1, 4) : List.of(12, 8, 9, 7, 10);
        List<Integer> rightPieces = isFlipped ? List.of(12, 8, 9, 7, 10) : List.of(6, 2, 3, 1, 4);

        int leftIndex = 0;
        for (int piece : leftPieces) {
            int count = Math.min(capturedCounts.getOrDefault(piece, 0), 15);
            for (int i = 0; i < count; i++) {
                int col = leftIndex % 2;
                int row = leftIndex / 2;
                int px = offsetX - (CAPTURED_PIECE_SIZE + 10) - col * (CAPTURED_PIECE_SIZE + 5);
                int py = offsetY + row * (CAPTURED_PIECE_SIZE + 5);
                matrixStack.push();
                switch (piece) {
                    case 1 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_ROOK, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 2 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_KNIGHT, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 3 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_BISHOP, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 4 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_QUEEN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 6 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_PAWN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 7 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_ROOK, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 8 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_KNIGHT, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 9 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_BISHOP, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 10 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_QUEEN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 12 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_PAWN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                }
                matrixStack.pop();
                leftIndex++;
            }
        }

        int rightIndex = 0;
        for (int piece : rightPieces) {
            int count = Math.min(capturedCounts.getOrDefault(piece, 0), 15);
            for (int i = 0; i < count; i++) {
                int col = rightIndex % 2;
                int row = rightIndex / 2;
                int px = offsetX + 8 * CELL_SIZE + 10 + col * (CAPTURED_PIECE_SIZE + 5);
                int py = offsetY + row * (CAPTURED_PIECE_SIZE + 5);
                matrixStack.push();
                switch (piece) {
                    case 1 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_ROOK, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 2 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_KNIGHT, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 3 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_BISHOP, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 4 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_QUEEN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 6 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_PAWN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 7 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_ROOK, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 8 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_KNIGHT, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 9 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_BISHOP, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 10 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_QUEEN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                    case 12 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_PAWN, px, py, CAPTURED_PIECE_SIZE, CAPTURED_PIECE_SIZE);
                }
                matrixStack.pop();
                rightIndex++;
            }
        }

        // Render promotion selection menu (horizontal)
        if (showPromotionMenu) {
            int menuX = offsetX + promotionX * CELL_SIZE;
            int menuY = offsetY + (isFlipped ? 7 - promotionY : promotionY) * CELL_SIZE - PROMOTION_BUTTON_SIZE - PROMOTION_BUTTON_SPACING;
            int[] promotionPieces = getLocalPlayerId() == 1 ? new int[]{4, 1, 2, 3} : new int[]{10, 7, 8, 9}; // Queen, Rook, Knight, Bishop
            for (int i = 0; i < 4; i++) {
                int buttonX = menuX + i * (PROMOTION_BUTTON_SIZE + PROMOTION_BUTTON_SPACING);
                context.fill(buttonX, menuY, buttonX + PROMOTION_BUTTON_SIZE, menuY + PROMOTION_BUTTON_SIZE, 0xFF_AAAAAA);
                matrixStack.push();
                switch (promotionPieces[i]) {
                    case 1 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_ROOK, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 2 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_KNIGHT, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 3 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_BISHOP, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 4 -> DrawUtil.simpleDrawTexture(context, ChessTextures.WHITE_QUEEN, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 7 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_ROOK, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 8 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_KNIGHT, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 9 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_BISHOP, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                    case 10 -> DrawUtil.simpleDrawTexture(context, ChessTextures.BLACK_QUEEN, buttonX + 2, menuY + 2, PROMOTION_BUTTON_SIZE - 4, PROMOTION_BUTTON_SIZE - 4);
                }
                matrixStack.pop();
            }
        }

        // Board coordinates
        String letters = "abcdefgh";
        for (int i = 0; i < 8; i++) {
            String letterStr = String.valueOf(letters.charAt(i));
            int pxLetter = offsetX + i * CELL_SIZE + 4;
            int pyLetter = offsetY + 8 * CELL_SIZE + 2;
            int lyBottom = isFlipped ? 0 : 7;
            int cellColorForLetter = (i + lyBottom) % 2 == 0 ? WHITE_CELL_COLOR : BLACK_CELL_COLOR;
            int letterColor = cellColorForLetter == WHITE_CELL_COLOR ? BLACK_CELL_COLOR : WHITE_CELL_COLOR;
            context.drawText(this.textRenderer, letterStr, pxLetter + 20, pyLetter - 10, letterColor, false);

            int pxNumber = offsetX - 10;
            int pyNumber = offsetY + i * CELL_SIZE + 8;
            String numStr = isFlipped ? Integer.toString(i + 1) : Integer.toString(8 - i);
            int lyNumber = isFlipped ? 7 - i : i;
            int lxNumber = 0;
            int cellColorForNumber = (lxNumber + lyNumber) % 2 == 0 ? WHITE_CELL_COLOR : BLACK_CELL_COLOR;
            int numberColor = cellColorForNumber == WHITE_CELL_COLOR ? BLACK_CELL_COLOR : WHITE_CELL_COLOR;
            context.drawText(this.textRenderer, numStr, pxNumber + 11, pyNumber - 7, numberColor, false);
        }

        // Game status
        String info;
        if (gameLogic.isGameOver()) {
            info = (gameLogic.getWinner() == 3) ? "Draw" : (gameLogic.getWinner() == 1 ? "White wins" : "Black wins");
        } else if (showPromotionMenu) {
            info = "Select promotion piece";
        } else {
            info = "Turn: " + (gameLogic.getCurrentPlayer() == 1 ? "White" : "Black");
        }
        context.drawText(this.textRenderer, info, halfW - info.length() * 3, offsetY + CELL_SIZE * 8 + 5, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (gameLogic.isGameOver()) {
            if (isHost()) {
                initBoard();
                gameLogic.resetGame();
                broadcastState();
            }
            return true;
        }

        if (showPromotionMenu) {
            int menuX = offsetX + promotionX * CELL_SIZE;
            int menuY = offsetY + (getLocalPlayerId() == 2 ? 7 - promotionY : promotionY) * CELL_SIZE - PROMOTION_BUTTON_SIZE - PROMOTION_BUTTON_SPACING;
            int[] promotionPieces = getLocalPlayerId() == 1 ? new int[]{4, 1, 2, 3} : new int[]{10, 7, 8, 9};
            for (int i = 0; i < 4; i++) {
                int buttonX = menuX + i * (PROMOTION_BUTTON_SIZE + PROMOTION_BUTTON_SPACING);
                if (mouseX >= buttonX && mouseX < buttonX + PROMOTION_BUTTON_SIZE &&
                        mouseY >= menuY && mouseY < menuY + PROMOTION_BUTTON_SIZE) {
                    int selectedPiece = promotionPieces[i];
                    if (isHost()) {
                        processMove(pendingPromotionMove.toX(), pendingPromotionMove.toY(), pendingPromotionMove.playerId(), selectedPiece);
                    } else {
                        LocalVoicePacket.send(new ChessMoveData(
                                pendingPromotionMove.fromX(),
                                pendingPromotionMove.fromY(),
                                pendingPromotionMove.toX(),
                                pendingPromotionMove.toY(),
                                pendingPromotionMove.playerId(),
                                selectedPiece
                        ));
                    }
                    showPromotionMenu = false;
                    pendingPromotionMove = null;
                    gameLogic.clearSelection();
                    highlighter.clearHighlights();
                    return true;
                }
            }
            return true;
        }

        int renderGridX = (int) ((mouseX - offsetX) / CELL_SIZE);
        int renderGridY = (int) ((mouseY - offsetY) / CELL_SIZE);

        boolean isFlipped = getLocalPlayerId() == 2;
        int gridY = isFlipped ? 7 - renderGridY : renderGridY;

        if (renderGridX >= 0 && renderGridX < 8 && gridY >= 0 && gridY < 8) {
            int localPlayerId = getLocalPlayerId();
            if (localPlayerId == 0) return true;

            if (gameLogic.getSelectedX() == -1 && gameLogic.getSelectedY() == -1) {
                if (localPlayerId == gameLogic.getCurrentPlayer() && gameLogic.isValidPiece(renderGridX, gridY, localPlayerId)) {
                    gameLogic.setSelected(renderGridX, gridY);
                    highlighter.updateHighlights(localPlayerId);
                }
            } else {
                if (localPlayerId == gameLogic.getCurrentPlayer()) {
                    ChessMoveData move = new ChessMoveData(gameLogic.getSelectedX(), gameLogic.getSelectedY(), renderGridX, gridY, localPlayerId, 0);
                    if (gameLogic.isValidMove(move.fromX(), move.fromY(), move.toX(), move.toY(), move.playerId())) {
                        if (gameLogic.isPawnPromotion(move.fromX(), move.fromY(), move.toX(), move.toY(), move.playerId())) {
                            pendingPromotionMove = move;
                            showPromotionMenu = true;
                            promotionX = move.toX();
                            promotionY = move.toY();
                        } else {
                            if (isHost()) {
                                processMove(move.toX(), move.toY(), move.playerId(), 0);
                            } else {
                                LocalVoicePacket.send(move);
                            }
                            gameLogic.clearSelection();
                            highlighter.clearHighlights();
                        }
                    } else {
                        gameLogic.clearSelection();
                        highlighter.clearHighlights();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (isHost() && !gameLogic.isGameOver()) {
            saveGameState();
        }
        super.close();
    }

    private void processMove(int toX, int toY, int player, int promotionPiece) {
        int fromX = gameLogic.getSelectedX();
        int fromY = gameLogic.getSelectedY();
        if (gameLogic.processMove(fromX, fromY, toX, toY, player, promotionPiece)) {
            pieceRenderX[toY][toX] = fromX;
            pieceRenderY[toY][toX] = fromY;

            int piece = gameLogic.getBoard()[toY][toX];
            int dx = Math.abs(toX - fromX);
            int dy = Math.abs(toY - fromY);
            if ((piece == 5 || piece == 11) && dy == 0 && dx == 2) {
                int rookFromX = (toX > fromX) ? 7 : 0;
                int rookToX = (toX > fromX) ? fromX + 1 : fromX - 1;
                pieceRenderX[fromY][rookToX] = rookFromX;
                pieceRenderY[fromY][rookToX] = fromY;
            }
            broadcastState();
            if (gameLogic.isGameOver()) {
                deleteGameState();
            } else {
                saveGameState();
            }
        }
    }

    private void broadcastState() {
        int[] cells = new int[64];
        for (int i = 0; i < 64; i++) {
            cells[i] = gameLogic.getBoard()[i / 8][i % 8];
        }
        for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
            int start = chunkIndex * CELLS_PER_CHUNK;
            int end = Math.min(start + CELLS_PER_CHUNK, 64);
            int chunkSize = end - start;
            int[] cellsChunk = new int[chunkSize];
            if (chunkSize > 0) {
                System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
            }
            LocalVoicePacket.send(new ChessStateData(chunkIndex, cellsChunk, gameLogic.getCurrentPlayer(), gameLogic.getWinner(),
                    gameLogic.isWhiteKingMoved(), gameLogic.isBlackKingMoved(), gameLogic.isWhiteRookA1Moved(),
                    gameLogic.isWhiteRookH1Moved(), gameLogic.isBlackRookA8Moved(), gameLogic.isBlackRookH8Moved(),
                    gameLogic.isGameOver(), gameLogic.getEnPassantTargetX(), gameLogic.getEnPassantTargetY()));
        }
    }

    private void saveGameState() {
        try {
            Path dir = Paths.get("config/arcadegames_svc/chess");
            Files.createDirectories(dir);
            Path file = dir.resolve("chess.dat");
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
                int[] cells = new int[64];
                for (int i = 0; i < 64; i++) {
                    cells[i] = gameLogic.getBoard()[i / 8][i % 8];
                }
                for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
                    int start = chunkIndex * CELLS_PER_CHUNK;
                    int end = Math.min(start + CELLS_PER_CHUNK, 64);
                    int chunkSize = end - start;
                    int[] cellsChunk = new int[chunkSize];
                    if (chunkSize > 0) {
                        System.arraycopy(cells, start, cellsChunk, 0, chunkSize);
                    }
                    ChessStateData state = new ChessStateData(chunkIndex, cellsChunk, gameLogic.getCurrentPlayer(),
                            gameLogic.getWinner(), gameLogic.isWhiteKingMoved(), gameLogic.isBlackKingMoved(),
                            gameLogic.isWhiteRookA1Moved(), gameLogic.isWhiteRookH1Moved(),
                            gameLogic.isBlackRookA8Moved(), gameLogic.isBlackRookH8Moved(), gameLogic.isGameOver(),
                            gameLogic.getEnPassantTargetX(), gameLogic.getEnPassantTargetY());
                    out.writeLong(state.pack());
                }
            }
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("saveGameState: {}", String.valueOf(e));
        }
    }

    private void deleteGameState() {
        try {
            Path file = Paths.get("config/arcadegames_svc/chess/chess.dat");
            Files.deleteIfExists(file);
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("deleteGameState: {}", String.valueOf(e));
        }
    }

    private boolean loadGameState() {
        Path file = Paths.get("config/arcadegames_svc/chess/chess.dat");
        if (!Files.exists(file)) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            for (int chunkIndex = 0; chunkIndex < 6; chunkIndex++) {
                long packed = in.readLong();
                ChessStateData state = ChessStateData.unpack(packed);
                if (state.chunkIndex() != chunkIndex) {
                    return false;
                }
                onRemoteState(state);
            }
            return true;
        } catch (IOException e) {
            ArcadeGames.LOGGER.error("loadGameState: {}", String.valueOf(e));
            return false;
        }
    }

    public void onRemoteMove(ChessMoveData move) {
        if (!isHost()) return;
        if (move.playerId() != gameLogic.getCurrentPlayer()) return;
        if (gameLogic.isValidMove(move.fromX(), move.fromY(), move.toX(), move.toY(), move.playerId())) {
            gameLogic.setSelected(move.fromX(), move.fromY());
            processMove(move.toX(), move.toY(), move.playerId(), move.promotionPiece());
            highlighter.clearHighlights();
        }
    }

    public void onRemoteState(ChessStateData state) {
        int chunkIndex = state.chunkIndex();
        int start = chunkIndex * CELLS_PER_CHUNK;
        int[] cellsChunk = state.cellsChunk();
        for (int i = 0; i < cellsChunk.length && start + i < 64; i++) {
            pendingBoard[(start + i) / 8][(start + i) % 8] = cellsChunk[i];
        }
        receivedChunkMask |= (1 << chunkIndex);
        gameLogic.setCurrentPlayer(state.currentPlayer());
        gameLogic.setWinner(state.winner());
        gameLogic.setGameOver(state.gameOver());
        gameLogic.setWhiteKingMoved(state.whiteKingMoved());
        gameLogic.setBlackKingMoved(state.blackKingMoved());
        gameLogic.setWhiteRookA1Moved(state.whiteRookA1Moved());
        gameLogic.setWhiteRookH1Moved(state.whiteRookH1Moved());
        gameLogic.setBlackRookA8Moved(state.blackRookA8Moved());
        gameLogic.setBlackRookH8Moved(state.blackRookH8Moved());
        gameLogic.setEnPassantTargetX(state.enPassantTargetX());
        gameLogic.setEnPassantTargetY(state.enPassantTargetY());

        if (receivedChunkMask == 63) {
            applyPendingBoard();
            receivedChunkMask = 0;
            if (gameLogic.getSelectedX() != -1 && gameLogic.getSelectedY() != -1) {
                highlighter.updateHighlights(getLocalPlayerId());
            } else {
                highlighter.clearHighlights();
            }
        }
    }

    private void applyPendingBoard() {
        int[][] currentBoard = gameLogic.getBoard();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (pendingBoard[y][x] != currentBoard[y][x] && pendingBoard[y][x] != 0) {
                    boolean found = false;
                    for (int fy = 0; fy < 8 && !found; fy++) {
                        for (int fx = 0; fx < 8; fx++) {
                            if (currentBoard[fy][fx] == pendingBoard[y][x] && (pendingBoard[fy][fx] == 0 || (fx == x && fy == y))) {
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
            System.arraycopy(pendingBoard[y], 0, currentBoard[y], 0, 8);
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