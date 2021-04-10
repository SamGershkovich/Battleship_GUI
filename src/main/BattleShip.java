//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package main;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class BattleShip {
    private final boolean DEBUGMODE;
    public static final int DEFAULTBOARDSIZE = 10;
    public static final int[] DEFAULTSHIPSIZES = new int[]{2, 3, 3, 4, 5};
    public int boardSize;
    public int[] shipSizes;

    private final CellState[][] board;

    private final ArrayList<Ship> ships;
    private final ArrayList<Point> hits;
    private final ArrayList<Point> misses;
    private final Random random;

    public static int cellSize = 50;


    public static String version() {
        return "Version 1.1 [Dec 17,2015]";
    }

    public BattleShip() {
        this(10, DEFAULTSHIPSIZES);
    }

    public BattleShip(int userSpecifiedBoardSize, int[] userShipSizes) {
        this.DEBUGMODE = false;
        this.random = new Random();
        this.boardSize = userSpecifiedBoardSize;
        if (this.boardSize < 10) {
            throw new IllegalArgumentException("Invalid board size specified - Minmimum board size is 10");
        } else {
            if (userShipSizes.length < 1) {
                this.shipSizes = DEFAULTSHIPSIZES;
            } else {
                int[] var3 = userShipSizes;
                int var4 = userShipSizes.length;
                int var5 = 0;

                while (true) {
                    if (var5 >= var4) {
                        this.shipSizes = Arrays.copyOf(userShipSizes, userShipSizes.length);
                        break;
                    }

                    int shipLength = var3[var5];
                    if (shipLength < 1 || shipLength > this.boardSize) {
                        throw new IllegalArgumentException("Ship lengths must be less than board size");
                    }

                    ++var5;
                }
            }

            this.board = new CellState[this.boardSize][this.boardSize];
            this.ships = new ArrayList();

            for (int i = 0; i < this.shipSizes.length; ++i) {
                Ship testShip = new Ship(this.shipSizes[i]);

                while (!testShip.getIsPlaced()) {
                    Point location = new Point(this.random.nextInt(this.boardSize), this.random.nextInt(this.boardSize));
                    ShipOrientation orientation = ShipOrientation.values()[this.random.nextInt(ShipOrientation.values().length)];
                    boolean placed = testShip.place(this.boardSize, location, orientation, this.ships);
                    if (placed) {
                    }
                }

                this.ships.add(testShip);
            }

            this.hits = new ArrayList();
            this.misses = new ArrayList();
        }
    }

    private boolean shipAt(Point p) {
        Iterator var2 = this.ships.iterator();

        Ship s;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            s = (Ship) var2.next();
        } while (!s.isAt(p));

        return true;
    }

    private void printBoard() {
        System.out.print("\n.  ");

        int y;
        for (y = 0; y < this.boardSize; ++y) {
            System.out.print("%2d ");
        }

        for (y = 0; y < this.boardSize; ++y) {
            System.out.printf("\n%d", y);

            for (int x = 0; x < this.boardSize; ++x) {
                System.out.printf(" %c ", this.board[x][y]);
            }
        }

    }

    public void drawBoard(GraphicsContext gc, double width, double height) {
        double startPosX = (width / 2) - ((cellSize * boardSize) / 2);
        double startPosY = (height / 2) - ((cellSize * boardSize) / 2);
        for (int y = 0; y < this.boardSize; ++y) {

            for (int x = 0; x < this.boardSize; ++x) {

                boolean fill = true;
                if (shipAt(new Point(x, y))) {
                    if (board[x][y] == CellState.Hit) {
                        gc.setFill(Color.RED);
                    } else {
                        gc.setFill(Color.TAN);
                       // fill = false;

                    }
                } else {
                    if (board[x][y] == CellState.Miss) {
                        gc.setFill(Color.WHITE);
                    } else {
                        fill = false;
                        //gc.setFill(Color.WHITE);
                    }
                }


                gc.setStroke(Color.BLACK);
                gc.setLineWidth(5);
                if (fill) {
                    gc.fillRect(startPosX + (x * cellSize), startPosY + (y * cellSize), cellSize, cellSize);
                }
                //if (shipAt(new Point(x, y))) {
                gc.strokeRect(startPosX + (x * cellSize), startPosY + (y * cellSize), cellSize, cellSize);
                //}
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(12));
                //gc.fillText(x + "," + y, startPosX + (x * cellSize) + cellSize / 2, startPosY + (y * cellSize) + cellSize / 2);
            }
        }
    }

    public boolean shoot(Point shot) {
        boolean hit = this.shipAt(shot);
        if (hit) {
            this.board[shot.x][shot.y] = CellState.Hit;
            this.hits.add(shot);
        } else {
            this.board[shot.x][shot.y] = CellState.Miss;
            this.misses.add(shot);
        }

        return this.shipAt(shot);
    }

    public int numberOfShipsSunk() {
        int num = 0;
        Iterator var2 = this.ships.iterator();

        while (var2.hasNext()) {
            Ship s = (Ship) var2.next();
            int length = s.getLength();
            Point pos = s.getLocation();
            boolean sunk = true;
            int y;
            if (s.getOrientation() == ShipOrientation.Horizontal) {
                for (y = 0; y < length; ++y) {
                    sunk &= this.board[pos.x + y][pos.y] == CellState.Hit;
                }
            } else {
                for (y = 0; y < length; ++y) {
                    sunk &= this.board[pos.x][pos.y + y] == CellState.Hit;
                }
            }

            if (sunk) {
                ++num;
            }
        }

        return num;
    }

    private int totalShipLengths() {
        int length = 0;

        Ship s;
        for (Iterator var2 = this.ships.iterator(); var2.hasNext(); length += s.getLength()) {
            s = (Ship) var2.next();
        }

        return length;
    }

    public boolean allSunk() {
        int numberOfHitCells = 0;

        for (int y = 0; y < this.boardSize; ++y) {
            for (int x = 0; x < this.boardSize; ++x) {
                if (this.board[x][y] == CellState.Hit) {
                    ++numberOfHitCells;
                }
            }
        }

        return numberOfHitCells == this.totalShipLengths();
    }

    public int totalShotsTaken() {
        return this.hits.size() + this.misses.size();
    }

    public int[] shipSizes() {
        return this.shipSizes;
    }

    public ArrayList<Point> getShips() {
        ArrayList<Point> points = new ArrayList<>();

        for (int y = 0; y < this.boardSize; ++y) {
            for (int x = 0; x < this.boardSize; ++x) {
                if (shipAt(new Point(x, y))) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    /*public static void main(String[] args) {
        System.out.println(version());
    }*/
}
