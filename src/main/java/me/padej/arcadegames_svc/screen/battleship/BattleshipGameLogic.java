package me.padej.arcadegames_svc.screen.battleship;

public class BattleshipGameLogic {
    private final int[][] player1Grid = new int[10][10];
    private final int[][] player2Grid = new int[10][10];
    private final int[][] player1View = new int[10][10];
    private final int[][] player2View = new int[10][10];
    private int currentPlayer = 1;
    private boolean gameOver = false;
    private int winner = 0;
    private boolean setupPhase = true;
    private int[] player1Ships = {0, 5, 4, 3, 3, 2};
    private int[] player2Ships = {0, 5, 4, 3, 3, 2};
    private int currentShipType = 1;
    private boolean currentShipHorizontal = true;
    private boolean player1Ready = false;
    private boolean player2Ready = false;

    public void initGame() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                player1Grid[y][x] = 0;
                player2Grid[y][x] = 0;
                player1View[y][x] = 0;
                player2View[y][x] = 0;
            }
        }
        currentPlayer = 1;
        gameOver = false;
        winner = 0;
        setupPhase = true;
        player1Ships = new int[]{0, 5, 4, 3, 3, 2};
        player2Ships = new int[]{0, 5, 4, 3, 3, 2};
        currentShipType = 1;
        currentShipHorizontal = true;
        player1Ready = false;
        player2Ready = false;
    }

    public boolean placeShip(int x, int y, int playerId, int shipType, boolean isHorizontal) {
        if (!setupPhase || shipType < 1 || shipType > 5) return false;
        int[][] grid = playerId == 1 ? player1Grid : player2Grid;
        int[] ships = playerId == 1 ? player1Ships : player2Ships;
        int shipSize = ships[shipType];
        if (ships[shipType] == 0) return false;

        // Check boundaries and overlaps
        if (isHorizontal) {
            if (x + shipSize > 10) return false;
            for (int i = 0; i < shipSize; i++) {
                if (!isValidCell(grid, x + i, y)) return false;
            }
        } else {
            if (y + shipSize > 10) return false;
            for (int i = 0; i < shipSize; i++) {
                if (!isValidCell(grid, x, y + i)) return false;
            }
        }

        // Place ship
        if (isHorizontal) {
            for (int i = 0; i < shipSize; i++) {
                grid[y][x + i] = shipType;
            }
        } else {
            for (int i = 0; i < shipSize; i++) {
                grid[y + i][x] = shipType;
            }
        }
        ships[shipType] = 0;
        return true;
    }

    private boolean isValidCell(int[][] grid, int x, int y) {
        if (grid[y][x] != 0) return false;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10 && grid[ny][nx] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValidShipPlacement(int x, int y, int playerId, int shipType, boolean isHorizontal) {
        if (!setupPhase || shipType < 1 || shipType > 5) return false;
        int[][] grid = playerId == 1 ? player1Grid : player2Grid;
        int[] ships = playerId == 1 ? player1Ships : player2Ships;
        int shipSize = ships[shipType];
        if (ships[shipType] == 0) return false;

        if (isHorizontal) {
            if (x + shipSize > 10) return false;
            for (int i = 0; i < shipSize; i++) {
                if (!isValidCell(grid, x + i, y)) return false;
            }
        } else {
            if (y + shipSize > 10) return false;
            for (int i = 0; i < shipSize; i++) {
                if (!isValidCell(grid, x, y + i)) return false;
            }
        }
        return true;
    }

    public boolean processMove(int x, int y, int playerId) {
        if (setupPhase || gameOver || playerId != currentPlayer) return false;
        int[][] targetGrid = playerId == 1 ? player2Grid : player1Grid;
        int[][] viewGrid = playerId == 1 ? player1View : player2View;

        if (viewGrid[y][x] != 0) return false;
        if (targetGrid[y][x] == 0) {
            viewGrid[y][x] = 6;
        } else {
            viewGrid[y][x] = 7;
        }
        currentPlayer = (currentPlayer == 1) ? 2 : 1;

        if (allShipsSunk(player1View)) {
            winner = 2;
            gameOver = true;
        } else if (allShipsSunk(player2View)) {
            winner = 1;
            gameOver = true;
        }
        return true;
    }

    private boolean allShipsSunk(int[][] viewGrid) {
        int[][] targetGrid = viewGrid == player1View ? player2Grid : player1Grid;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (targetGrid[y][x] > 0 && viewGrid[y][x] != 7) return false;
            }
        }
        return true;
    }

    public void advanceSetupPhase(int playerId) {
        if (!setupPhase) return;
        int[] ships = playerId == 1 ? player1Ships : player2Ships;
        for (int i = 1; i <= 5; i++) {
            if (ships[i] > 0) {
                currentShipType = i;
                return;
            }
        }
        if (playerId == 1) player1Ready = true;
        else player2Ready = true;
        if (player1Ready && player2Ready) {
            setupPhase = false;
            currentPlayer = 1;
        }
    }

    public void toggleShipOrientation() {
        currentShipHorizontal = !currentShipHorizontal;
    }

    public int[][] getPlayerGrid(int playerId) {
        return playerId == 1 ? player1Grid : player2Grid;
    }

    public int[][] getViewGrid(int playerId) {
        return playerId == 1 ? player1View : player2View;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public boolean isSetupPhase() {
        return setupPhase;
    }

    public void setSetupPhase(boolean setupPhase) {
        this.setupPhase = setupPhase;
    }

    public int getCurrentShipType() {
        return currentShipType;
    }

    public boolean isCurrentShipHorizontal() {
        return currentShipHorizontal;
    }

    public int[] getPlayerShips(int playerId) {
        return playerId == 1 ? player1Ships : player2Ships;
    }

    public boolean isPlayerReady(int playerId) {
        return playerId == 1 ? player1Ready : player2Ready;
    }

    public void setPlayerReady(int playerId, boolean ready) {
        if (playerId == 1) player1Ready = ready;
        else if (playerId == 2) player2Ready = ready;
    }
}
