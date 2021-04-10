package main;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Sam Gershkovich - 000801766
 */
public class SamBot {

    private double canvasWidth;
    private double canvasHeight;
    private GraphicsContext gc;
    private Canvas canvas;

    private int gameSize;
    private BattleShip battleShip;
    private Random random;
    private CellState[][] board;

    private int lowestProbability;
    private int highestProbability;
    private int[][] probabilityBoard;
    private boolean getFirstCell = true;
    private boolean getFirstTarget = true;

    private int smallestShipSize;
    private int biggestShipSize;
    private int totalShipPoints;
    private ArrayList<Integer> ships = new ArrayList<>();
    private ArrayList<Integer> allShips = new ArrayList<>();
    private ArrayList<ArrayList<Point>> sunkShips = new ArrayList<>();
    private ArrayList<MyShip> potentialShips = new ArrayList<>();

    private int shipsRemaining;
    private int targetsRemaining;
    private int parity;
    private boolean doParity = true;

    private Point shot;

    private int missTargetCount = 0;

    private boolean hit;
    private boolean multiShipHit = false;

    private TargetDirection targetDirection = new TargetDirection("", 0);
    private boolean directionChanged = false;
    private int shipsToSink = 0;
    private int targetHitCount = 0;
    private boolean targetedAdjacents = false;
    private boolean targetedEdges = false;

    private boolean manualUpdateProbabilities = false;

    private boolean shipSunk = false;

    private Point lastHit;
    private Point lastShot;

    private ArrayList<Point> consecutiveDirectionHits = new ArrayList<>();

    private boolean sinking = false;
    private ArrayList<Point> targets = new ArrayList<>();
    private ArrayList<Point> lowPriorityTargets = new ArrayList<>();
    private ArrayList<Point> shipPoints = new ArrayList<>();
    private ArrayList<Point> sunkPoints = new ArrayList<>();

    /**
     * Constructor keeps a copy of the BattleShip instance
     *
     * @param b previously created battleship instance - should be a new game
     */
    public SamBot(BattleShip b, double canvasWidth, double canvasHeight, GraphicsContext gc, Canvas canvas) {

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.gc = gc;
        this.canvas = canvas;

        battleShip = b;
        gameSize = b.boardSize;

        probabilityBoard = new int[gameSize][gameSize];

        int[] sizes = b.shipSizes();

        smallestShipSize = sizes[0];

        for (int i = 0; i < sizes.length; i++) {
            totalShipPoints += sizes[i];
            ships.add(sizes[i]);
            allShips.add(sizes[i]);

            if (sizes[i] > biggestShipSize) {
                biggestShipSize = sizes[i];
            }
            if (sizes[i] < smallestShipSize) {
                smallestShipSize = sizes[i];
            }
        }

        parity = smallestShipSize;
        shipsRemaining = ships.size();
        targetsRemaining = (totalShipPoints - shipPoints.size());
        //ships.remove(0);

        random = new Random();   // Needed for random shooter - not required for more systematic approaches
        board = new CellState[gameSize][gameSize];

        lastShot = new Point((gameSize - 1) / 2, (gameSize - 1) / 2);

        for (int x = 0; x < gameSize; x++) {
            for (int y = 0; y < gameSize; y++) {
                board[x][y] = CellState.Empty;
            }
        }
    }

    /**
     * Create a random shot and calls the battleship shoot method
     */
    public void fireShot() {
        if (!manualUpdateProbabilities) {
            updateProbabilities(false);
        } else {
            manualUpdateProbabilities = false;
        }


        showProbability();
        //showProbabilityValue();

        if (!sinking) {//if not in sinking mode, shot at the most probable cell
            Point mostProbable = getMostProbableCell();
            int x = mostProbable.x;
            int y = mostProbable.y;
            shot = new Point(x, y);
        }

        if (sinking) {//if in sinking mode, shoot at the most probable cell from the targets list
            Point mostProbable = getMostProbableTarget();
            shot = new Point(mostProbable.x, mostProbable.y);
            targets.remove(targets.indexOf(mostProbable));
        }

        //shoot shot and determine if we sunk a ship
        int shipsSunkBefore = battleShip.numberOfShipsSunk();
        hit = battleShip.shoot(shot);
        shipSunk = battleShip.numberOfShipsSunk() > shipsSunkBefore;

        if (hit) {//we hit

            board[shot.x][shot.y] = CellState.Hit;

            shipPoints.add(shot);//add the shot to this list of known ship cells

            targetsRemaining = (totalShipPoints - shipPoints.size());

            if (potentialShips.size() > 0) {
                potentialShips.get(0).points.add(shot);
            }

            if (!sinking) {//if not in sinking mode, initiate sinking mode

                lastHit = null;
                shipsToSink++;

                targetDirects();//add all the adjacent cells to the shot to the targets list

                consecutiveDirectionHits.clear();
                consecutiveDirectionHits.add(new Point(shot.x, shot.y));

                directionChanged = false;

            } else {//we are already in sinking mode

                if (lastHit == null) {
                    lastHit = new Point(shot.x, shot.y);
                }

                if (shot.y < lastHit.y && shot.y > 0) {//if this shot is to the north of the last hit and not the edge of the board
                    if (board[shot.x][shot.y - 1] == CellState.Empty)//add the next cell north to the targets list if Empty
                        targets.add(new Point(shot.x, shot.y - 1));
                }

                if (shot.x > lastHit.x && shot.x < gameSize - 1) {//if this shot is to the east of the last hit and not the edge of the board
                    if (board[shot.x + 1][shot.y] == CellState.Empty)//add the next cell east to the targets list if Empty
                        targets.add(new Point(shot.x + 1, shot.y));
                }
                if (shot.y > lastHit.y && shot.y < gameSize - 1) {//if this shot is to the south of the last hit and not the edge of the board
                    if (board[shot.x][shot.y + 1] == CellState.Empty)//add the next cell south to the targets list if Empty
                        targets.add(new Point(shot.x, shot.y + 1));
                }
                if (shot.x < lastHit.x && shot.x > 0) {//if this shot is to the west of the last hit and not the edge of the board
                    if (board[shot.x - 1][shot.y] == CellState.Empty)//add the next cell west to the targets list if Empty
                        targets.add(new Point(shot.x - 1, shot.y));
                }


                //remove all targets in perpendicular direction
                if (lastHit.x != shot.x) {
                    if (targetDirection.direction == "Vertical") {
                        directionChanged = true;
                    } else {
                        directionChanged = false;
                    }
                    targetDirection.direction = "Horizontal";
                    targetDirection.lockedAxisValue = shot.y;

                    for (int i = 0; i < targets.size(); i++) {//remove all targets north and south of the shot
                        if (targets.get(i).y != shot.y) {
                            targets.remove(i);
                            i--;
                        }
                    }
                } else if (lastHit.y != shot.y) {
                    if (targetDirection.direction == "Horizontal") {
                        directionChanged = true;
                    } else {
                        directionChanged = false;
                    }
                    targetDirection.direction = "Vertical";
                    targetDirection.lockedAxisValue = shot.x;


                    for (int i = 0; i < targets.size(); i++) {//remove all targets east and west of the shot
                        if (targets.get(i).x != shot.x) {
                            targets.remove(i);
                            i--;
                        }
                    }
                }


                if (!directionChanged && potentialShips.size() == 0) {
                    consecutiveDirectionHits.add(new Point(shot.x, shot.y));
                } else {
                    shipsToSink++;
                }
            }

            sinking = true;
        } else {//we missed

            board[shot.x][shot.y] = CellState.Miss;

            if (!sinking) {
                shipsToSink = 0;
            }
        }

        if (shipSunk) {//we sunk a ship

            ArrayList<Point> sunkShip = new ArrayList<>();

            sunkShip = consecutiveDirectionHits;

            if (potentialShips.size() > 0) {//if we have a potential ship, use it's points instead
                sunkShip = potentialShips.get(0).points;
            }

            targets.clear();

            //The size of the sunk ship is greater than the biggest ship left in our ship array
            if (sunkShip.size() > ships.get(ships.size() - 1)) {
                updateProbabilities(true);//update probabilites with cross over on and target adjacent cells
                manualUpdateProbabilities = true;
                targetEmptyAdjacents(true);
            }

            boolean removed = false;
            ArrayList<Point> ship = new ArrayList<>();

            for (int i = 0; i < sunkShip.size(); i++) {//iterate of the points of the ship we sunk
                ship.add(sunkShip.get(i));

                if (ships.contains(sunkShip.size() - i) && sunkShip.size() - i > smallestShipSize && !removed) {//remove the correct ship if not the smallest
                    ships.remove(ships.indexOf(sunkShip.size() - i));
                    removed = true;
                }

            }

            consecutiveDirectionHits.clear();

            if (potentialShips.size() > 0) {
                potentialShips.remove(0);
            }

            sunkShips.add(ship);

            sunkPoints.add(new Point(shot.x, shot.y));

            shipsRemaining--;

            shipsToSink = Math.max(0, shipsToSink - 1);

            if (potentialShips.size() == 0) {
                shipsToSink = 0;
            }

            if (shipsToSink == 0) {
                sinking = false;
            }
        }

        if (!sinking) {
            targetDirection.direction = "";
            targetDirection.lockedAxisValue = -1;
        }

        if (potentialShips.size() > 0) {
            targetPotentialShip();
        }

        if (shipsRemaining == 1) {//1 ship left, set it to be the size of the remaining targets
            ships.clear();
            ships.add((totalShipPoints - shipPoints.size()));
        } else if (targetsRemaining < getMinTotalShipLength() && !sinking) {
            targetEmptyAdjacents(true);
        }

        if ((targets.size() == 0 && sinking)) {//we have no targets left, but are still in sinking mode (did not sink a ship)
            if (consecutiveDirectionHits.size() > 0) {//if we have consecutive hits
                targetDirectionEdge();//check the edges of the our hits

                if (targets.size() == 0) {//if still no targets
                    for (int i = 0; i < consecutiveDirectionHits.size(); i++) {//add a potential ship for every consecutive hit
                        potentialShips.add(new MyShip(new TargetDirection(targetDirection.direction, targetDirection.lockedAxisValue), consecutiveDirectionHits.get(i)));
                    }
                    shipsToSink = consecutiveDirectionHits.size();

                    targetPotentialShip();//target the first potential ship
                }

            } else {//no consecutive hits
                targetEmptyAdjacents(false);//target the adjacent cells of all hits
            }
        }

        for (int i = 0; i < targets.size(); i++) {//remove bad targets
            if (board[targets.get(i).x][targets.get(i).y] != CellState.Empty) {
                targets.remove(i);
                i--;
            }
        }

        if (potentialShips.size() > 0) {
            consecutiveDirectionHits.clear();
        }

        if (targets.size() == 0) {//set sinking mode if we have targets or not
            sinking = false;
        } else {
            sinking = true;
        }

        if (ships.size() > 0 && ships.get(0) == 1) {//if we have a ship of size 1, change it to be smallest ship size
            ships.set(0, smallestShipSize);
        }

        if (hit) {
            lastHit = new Point(shot.x, shot.y);
        }

        if (potentialShips.size() > 0) {
            Point potShot = potentialShips.get(0).points.get(potentialShips.get(0).points.size() - 1);
            lastHit = new Point(potShot.x, potShot.y);
        }

        if (!hit && !sinking) {
            lastHit = null;
        }

        battleShip.drawBoard(gc, canvasWidth, canvasHeight);
        showStats();

    }

    /**
     * Update probability board
     *
     * @param allowHitCrossOver Whether or not to allow a check on cell that is a Hit
     */
    public void updateProbabilities(boolean allowHitCrossOver) {
        if (ships.size() > 0) {
            probabilityBoard = new int[gameSize][gameSize];

            for (int y = 0; y < gameSize; y++) {//iterate over the board
                for (int x = 0; x < gameSize; x++) {

                    if (board[x][y] == CellState.Miss //if the current cell is a miss, skip
                            || sunkPoints.contains(new Point(x, y)) //if the current cell is a confirmed sunk hit, skip

                            //skip if the current cell is a Hit and hitCrossOver is off (but only if there is 1 ship left with more than 1 un-hit point)
                            || (ships.size() == 1 && board[x][y] == CellState.Hit && (totalShipPoints - shipPoints.size()) > 1) && !allowHitCrossOver) {
                        continue;
                    }
                    for (int ship = 0; ship < ships.size(); ship++) {//iterate through all ships remaining

                        boolean fitsHorizontal = true;//whether the ship fits horizontally
                        boolean fitsVertical = true;//whether the ship fits vertically

                        for (int i = 1; i < ships.get(ship); i++) {//iterate across the ships points
                            if (((x + i > gameSize - 1 //the offset x position is off the board, doesnt fit
                                    || board[x + i][y] == CellState.Miss) //the offset x position is a miss, doesnt fit

                                    //doesn't fit if the current cell is a Hit and hitCrossOver is off (but only if there is more than 1 un-hit ship point left)
                                    || (ships.size() == 1 && board[x + i][y] == CellState.Hit && (totalShipPoints - shipPoints.size()) > 1) && !allowHitCrossOver)
                                    && fitsHorizontal) {
                                fitsHorizontal = false;
                            }
                            if (((y + i > gameSize - 1 //the offset y position is off the board, doesnt fit
                                    || board[x][y + i] == CellState.Miss)  //the offset y position is a miss, doesnt fit

                                    //doesn't fit if the current cell is a Hit and hitCrossOver is off (but only if there is more than 1 un-hit ship point left)
                                    || (ships.size() == 1 && board[x][y + i] == CellState.Hit && (totalShipPoints - shipPoints.size()) > 1) && !allowHitCrossOver)
                                    && fitsVertical) {
                                fitsVertical = false;
                            }
                        }
                        if (!fitsHorizontal && !fitsVertical) {//skip if doesnt fit either direction
                            continue;
                        }

                        for (int i = 0; i < ships.get(ship); i++) {//loop through ship points
                            if (fitsHorizontal) {//if the ship fit horizontal, add to probability
                                if (board[x + i][y] == CellState.Empty) {
                                    probabilityBoard[x + i][y]++;
                                    if (probabilityBoard[x + i][y] > highestProbability) {
                                        highestProbability = probabilityBoard[x + i][y];
                                    }
                                }
                            }
                            if (fitsVertical) {//if the ship fit vertical, add to probability
                                if (board[x][y + i] == CellState.Empty) {
                                    probabilityBoard[x][y + i]++;
                                    if ( probabilityBoard[x][y + i] > highestProbability) {
                                        highestProbability =  probabilityBoard[x][y + i];
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        getLowestProbability();
    }

    /**
     * Add all adjacent Empty cells to the hit to targets
     */
    public void targetDirects() {
        if (shot.y > 0) {
            if (board[shot.x][shot.y - 1] == CellState.Empty)
                targets.add(new Point(shot.x, shot.y - 1));
        }
        if (shot.x < gameSize - 1) {
            if (board[shot.x + 1][shot.y] == CellState.Empty)
                targets.add(new Point(shot.x + 1, shot.y));
        }
        if (shot.y < gameSize - 1) {
            if (board[shot.x][shot.y + 1] == CellState.Empty)
                targets.add(new Point(shot.x, shot.y + 1));
        }
        if (shot.x > 0) {
            if (board[shot.x - 1][shot.y] == CellState.Empty)
                targets.add(new Point(shot.x - 1, shot.y));
        }
    }

    /**
     * Add all Empty cells adjacent to all the previous hits
     *
     * @param consecutive Whether or not to only add cells adjacent to the current consecutive hits
     */
    public void targetEmptyAdjacents(boolean consecutive) {

        if (ships.size() > 0 && ships.get(0) == 1) {//if there is only 1 single un-hit cell left, dont filter for only consecutive shots
            consecutive = false;
        }

        if (consecutive) {//add all empty cells within the board adjacent to the consecutive hits
            for (int i = 0; i < consecutiveDirectionHits.size(); i++) {
                Point shot = consecutiveDirectionHits.get(i);

                if (shot.y > 0) {
                    if (board[shot.x][shot.y - 1] == CellState.Empty)
                        targets.add(new Point(shot.x, shot.y - 1));
                }
                if (shot.x < gameSize - 1) {
                    if (board[shot.x + 1][shot.y] == CellState.Empty)
                        targets.add(new Point(shot.x + 1, shot.y));
                }
                if (shot.y < gameSize - 1) {
                    if (board[shot.x][shot.y + 1] == CellState.Empty)
                        targets.add(new Point(shot.x, shot.y + 1));
                }
                if (shot.x > 0) {
                    if (board[shot.x - 1][shot.y] == CellState.Empty)
                        targets.add(new Point(shot.x - 1, shot.y));
                }
            }
        } else {//add all empty cells within the board adjacent to all previous hits
            for (int i = 0; i < shipPoints.size(); i++) {
                Point shot = shipPoints.get(i);

                if (shot.y > 0) {
                    if (board[shot.x][shot.y - 1] == CellState.Empty)
                        targets.add(new Point(shot.x, shot.y - 1));
                }
                if (shot.x < gameSize - 1) {
                    if (board[shot.x + 1][shot.y] == CellState.Empty)
                        targets.add(new Point(shot.x + 1, shot.y));
                }
                if (shot.y < gameSize - 1) {
                    if (board[shot.x][shot.y + 1] == CellState.Empty)
                        targets.add(new Point(shot.x, shot.y + 1));
                }
                if (shot.x > 0) {
                    if (board[shot.x - 1][shot.y] == CellState.Empty)
                        targets.add(new Point(shot.x - 1, shot.y));
                }
            }
        }
        if (targets.size() == 0 && consecutive) {
            targetEmptyAdjacents(false);
        } else {
            sinking = false;
        }
    }

    /**
     * Add the edges of the current ship we are sinking
     * Example:      Add 1 to targets
     *               ..........
     * . = empty     ..oxxm1...
     * x = hit       .....m....
     * o = miss      .....m....
     * m = old hit   ..........
     */
    public void targetDirectionEdge() {
        if (targetDirection.direction == "") {//there is no target direction
            Point hit = consecutiveDirectionHits.get(0);

            //find and empty cell adjacent to the hit, and set the direction
            if (hit.x > 0) {
                if (board[hit.x - 1][hit.y] != CellState.Miss) {
                    targetDirection.direction = "Horizontal";
                    targetDirection.lockedAxisValue = hit.y;
                }
            } else if (hit.x < gameSize - 1) {
                if (board[hit.x + 1][hit.y] != CellState.Miss) {
                    targetDirection.direction = "Horizontal";
                    targetDirection.lockedAxisValue = hit.y;
                }
            } else if (hit.y > 0) {
                if (board[hit.x][hit.y - 1] != CellState.Miss) {
                    targetDirection.direction = "Vertical";
                    targetDirection.lockedAxisValue = hit.x;
                }
            } else if (hit.y < gameSize - 1) {
                if (board[hit.x][hit.y + 1] != CellState.Miss) {
                    targetDirection.direction = "Vertical";
                    targetDirection.lockedAxisValue = hit.x;
                }
            }
        }

        switch (targetDirection.direction) {
            case "Horizontal":

                //initialize farthest and closest x values
                int farthestX = consecutiveDirectionHits.get(0).x;
                int closestX = consecutiveDirectionHits.get(0).x;

                int y = consecutiveDirectionHits.get(0).y;//y value is constant

                //find the shot with the highest x, and the shot with the lowest x
                for (int i = 0; i < consecutiveDirectionHits.size(); i++) {
                    if (consecutiveDirectionHits.get(i).x > farthestX) {
                        farthestX = consecutiveDirectionHits.get(i).x;
                    } else if (consecutiveDirectionHits.get(i).x < closestX) {
                        closestX = consecutiveDirectionHits.get(i).x;
                    }
                }

                boolean added = false;

                //if the shot after the shot with the farthest x is on the board and empty, add it to targets
                if (farthestX < gameSize - 1 && board[farthestX + 1][y] == CellState.Empty) {
                    targets.add(new Point(farthestX + 1, y));
                    added = true;
                }
                //if the shot before the shot with the closest x is on the board and empty, add it to targets
                if (closestX > 0 && board[closestX - 1][y] == CellState.Empty) {
                    targets.add(new Point(closestX - 1, y));
                    added = true;
                }

                if (!added) {
                    int offset = 1;

                    //if the shot after the shot with the farthest x is on the board and not a miss, keep checking the next cell over until the cell is not a hit
                    if (farthestX + offset <= gameSize - 1 && board[farthestX + offset][y] != CellState.Miss) {

                        //increment offset until the cell is not a hit
                        while (farthestX + offset <= gameSize - 1 && board[farthestX + offset][y] == CellState.Hit) {
                            offset++;
                        }

                        //if the offset cell is on the board and Empty, add it to targets
                        if (farthestX + offset <= gameSize - 1 && board[farthestX + offset][y] == CellState.Empty) {
                            targets.add(new Point(farthestX + offset, y));
                        }
                    }

                    //if the shot before the shot with the closest x is on the board and not a miss, keep checking the previous cell over until the cell is not a hit
                    if (closestX - offset >= 0 && board[closestX - offset][y] != CellState.Miss) {

                        //increment offset until the cell is not a hit
                        while (closestX - offset >= 0 && board[closestX - offset][y] == CellState.Hit) {
                            offset++;
                        }

                        //if the offset cell is on the board and Empty, add it to targets
                        if (closestX - offset >= 0 && board[closestX - offset][y] == CellState.Empty) {
                            targets.add(new Point(closestX - offset, y));
                        }
                    }
                }
                break;
            case "Vertical":

                //initialize farthest and closest y values
                int farthestY = consecutiveDirectionHits.get(0).y;
                int closestY = consecutiveDirectionHits.get(0).y;

                int x = consecutiveDirectionHits.get(0).x;//x value is constant

                //find the shot with the y x, and the shot with the lowest y
                for (int i = 0; i < consecutiveDirectionHits.size(); i++) {
                    if (consecutiveDirectionHits.get(i).y > farthestY) {
                        farthestY = consecutiveDirectionHits.get(i).y;
                    } else if (consecutiveDirectionHits.get(i).y < closestY) {
                        closestY = consecutiveDirectionHits.get(i).y;
                    }
                }

                added = false;

                //if the shot after the shot with the farthest y is on the board and empty, add it to targets
                if (farthestY < gameSize - 1 && board[x][farthestY + 1] == CellState.Empty) {
                    targets.add(new Point(x, farthestY + 1));
                    added = true;

                }

                //if the shot before the shot with the closest y is on the board and empty, add it to targets
                if (closestY > 0 && board[x][closestY - 1] == CellState.Empty) {
                    targets.add(new Point(x, closestY - 1));
                    added = true;
                }

                if (!added) {
                    int offset = 1;

                    //if the shot after the shot with the farthest y is on the board and not a miss, keep checking the next cell over until the cell is not a hit
                    if (farthestY + offset <= gameSize - 1 && board[x][farthestY + offset] != CellState.Miss) {

                        //increment offset until the cell is not a hit
                        while (farthestY + offset <= gameSize - 1 && board[x][farthestY + offset] == CellState.Hit) {
                            offset++;
                        }

                        //if the offset cell is on the board and Empty, add it to targets
                        if (farthestY + offset <= gameSize - 1 && board[x][farthestY + offset] == CellState.Empty) {
                            targets.add(new Point(x, farthestY + offset));
                        }
                    }

                    //if the shot before the shot with the closest y is on the board and not a miss, keep checking the previous cell over until the cell is not a hit
                    if (closestY - offset >= 0 && board[x][closestY - offset] != CellState.Miss) {

                        //increment offset until the cell is not a hit
                        while (closestY - offset >= 0 && board[x][closestY - offset] == CellState.Hit) {
                            offset++;
                        }

                        //if the offset cell is on the board and Empty, add it to targets
                        if (closestY - offset >= 0 && board[x][closestY - offset] == CellState.Empty) {
                            targets.add(new Point(x, closestY - offset));
                        }
                    }
                }
                break;
        }
    }

    /**
     * Add the correct directional targets for the first potential ship cell we hit
     * Example:        Add 1 and 2 to targets
     *                       ..........
     * . = empty             ...1......
     * x = hit but no sink   ..oxxo....
     * o = miss              ...2......
     *                       ..........
     */
    public void targetPotentialShip() {


        if (potentialShips.get(0).points.size() == 1) {//only do this once for the potential ship

            //update the target direction
            targetDirection = new TargetDirection(potentialShips.get(0).shipDirection.direction, potentialShips.get(0).shipDirection.lockedAxisValue);
            targets.clear();

            Point origin = potentialShips.get(0).origin;

            switch (potentialShips.get(0).shipDirection.direction) {//based on the direction of the potential ship:
                case "Horizontal":

                    int constantPos = potentialShips.get(0).origin.y;

                    //Add the the cells east and west of the origin of the potential ship if those cells are empty and within the board

                    if (origin.x < gameSize - 1) {
                        if (board[origin.x + 1][constantPos] == CellState.Empty) {
                            targets.add(new Point(origin.x + 1, constantPos));
                        }
                    }
                    if (origin.x > 0) {
                        if (board[origin.x - 1][constantPos] == CellState.Empty) {
                            targets.add(new Point(origin.x - 1, constantPos));
                        }
                    }
                    break;
                case "Vertical":

                    constantPos = potentialShips.get(0).origin.x;

                    //Add the the cells north and south of the origin of the potential ship if those cells are empty and within the board
                    if (origin.y < gameSize - 1) {
                        if (board[constantPos][origin.y + 1] == CellState.Empty) {
                            targets.add(new Point(constantPos, origin.y + 1));
                        }
                    }
                    if (origin.y > 0) {
                        if (board[constantPos][origin.y - 1] == CellState.Empty) {
                            targets.add(new Point(constantPos, origin.y - 1));
                        }
                    }
                    break;
            }
        }
    }

    /**
     * @return The most probable Point from all the Empty cells on the board
     */
    public Point getMostProbableCell() {


        Point cell = new Point(gameSize / 2, gameSize / 2);
        int probability = probabilityBoard[gameSize / 2][gameSize / 2];

        for (int y = 0; y < gameSize; y++) {//loop through probability board, and find the cell with the highest probability
            for (int x = 0; x < gameSize; x++) {

                if (getFirstCell) {//if there are multiple highest probabilities, this will select the first one
                    if (probabilityBoard[x][y] > probability) {
                        probability = probabilityBoard[x][y];
                        cell = new Point(x, y);
                    }
                } else {//if there are multiple highest probabilities, this will select the last one (difference is >= vs. just >)
                    if (probabilityBoard[x][y] >= probability) {
                        probability = probabilityBoard[x][y];
                        cell = new Point(x, y);
                    }
                }
            }
        }

        getFirstCell = !getFirstCell;

        //if probability is zero, there are no possible position for the ships on the board
        //first, check if the number of ships we think remain matches how many actually remain
        if (probability == 0 && ships.size() < shipsRemaining) {
            ships.add(0, smallestShipSize);//add the smallest ship to list, update probabilities and try again
            updateProbabilities(false);
            return getMostProbableCell();
        }
        //second, check if check if there is only 1 ship left
        else if (probability == 0 && ships.size() == 1) {
            updateProbabilities(true);//update probabilities with hit cross over on, and try again
            return getMostProbableCell();
        }
        //something else has gone wrong
        else if (probability == 0) {//clear the ships list then add back the smallest ship, then update probabilities and try again
            int temp = ships.get(0);
            ships.clear();
            ships.add(temp);
            updateProbabilities(false);
            return getMostProbableCell();
        }

        return cell;
    }

    /**
     * @return The most probable Point from all the targets
     */
    public Point getMostProbableTarget() {

        Point cell = new Point(targets.get(0).x, targets.get(0).y);
        int probability = probabilityBoard[cell.x][cell.y];

        for (int i = 0; i < targets.size(); i++) {//loop through targets list, and find the cell with the highest probability

            if (getFirstTarget) {//if there are multiple highest probabilities, this will select the first one
                if (probabilityBoard[targets.get(i).x][targets.get(i).y] > probability) {
                    probability = probabilityBoard[targets.get(i).x][targets.get(i).y];
                    cell = new Point(targets.get(i).x, targets.get(i).y);
                }
            } else {//if there are multiple highest probabilities, this will select the last one (difference is >= vs. just >)
                if (probabilityBoard[targets.get(i).x][targets.get(i).y] >= probability) {
                    probability = probabilityBoard[targets.get(i).x][targets.get(i).y];
                    cell = new Point(targets.get(i).x, targets.get(i).y);
                }
            }
        }

        getFirstTarget = !getFirstTarget;

        return cell;
    }

    /**
     * @return The smallest possible combined length of ships based on the number of ships left
     */
    public int getMinTotalShipLength() {
        int length = 0;

        for (int i = 0; i < shipsRemaining; i++) {
            length += allShips.get(i);
        }
        return length;
    }

    /**GUI FUNCTIONS*/

    /**Display variables in GUI*/
    public void showStats() {
        showTargets();
        if (shot != null) {
            showSelect(shot);
        }

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(18));

        if (shot != null) {
            gc.fillText("Shot:" + shot.x + "," + shot.y, 25, 400);
        }
        if (lastHit != null) {
            gc.fillText("Last Hit:" + lastHit.x + "," + lastHit.y, 25, 425);
        }

        if (targetedEdges) {
            gc.fillText("Targeted Edges", 25, 475);
        }
        gc.fillText("Hit: " + hit, 25, 525);
        gc.fillText("Sunk: " + shipSunk, 25, 550);
        gc.fillText("Sinking: " + sinking, 25, 575);
        gc.fillText("Direction: " + targetDirection, 25, 600);
        gc.fillText("Direction Changed: " + directionChanged, 25, 625);
        gc.fillText("Hits: " + targetHitCount, 25, 650);
        gc.fillText("Consec Hits: " + consecutiveDirectionHits.size(), 25, 675);
        gc.fillText("Targets: " + targets.size(), 25, 700);
        gc.fillText("To Sink: " + shipsToSink, 25, 725);
        gc.fillText("Ships Remaining: " + shipsRemaining, 25, 750);
        gc.fillText("Targets Remaining: " + (totalShipPoints - shipPoints.size()), 25, 775);
        gc.fillText("Parity: " + parity, 25, 800);

        String shipsText = "[";
        for (int i = 0; i < consecutiveDirectionHits.size(); i++) {
            shipsText += "(" + consecutiveDirectionHits.get(i).x + "," + consecutiveDirectionHits.get(i).y + ")";
            if (i < consecutiveDirectionHits.size() - 1) {
                shipsText += ", ";
            }
        }
        gc.fillText("Consec Shots: " + shipsText + "]", 25, 900);

        shipsText = "[";
        for (int i = 0; i < potentialShips.size(); i++) {
            shipsText += potentialShips.get(i).toString();
            if (i < potentialShips.size() - 1) {
                shipsText += ", ";
            }
        }
        gc.fillText("Potential ships: " + shipsText + "]", 25, 925);

        shipsText = "[";
        for (int i = 0; i < sunkShips.size(); i++) {
            shipsText += sunkShips.get(i).size();
            if (i < sunkShips.size() - 1) {
                shipsText += ", ";
            }
        }
        gc.fillText("Sunk ships: " + shipsText + "]", 25, 950);


        shipsText = "[";
        for (int i = 0; i < ships.size(); i++) {
            shipsText += ships.get(i);
            if (i < ships.size() - 1) {
                shipsText += ", ";
            }
        }
        gc.fillText("Ships: " + shipsText + "]", 25, 975);
    }

    /**
     *
     * @return The variables printed to the console
     */
    public String getDataOutput() {
        String output = "";

        output += "Shot:" + shot.x + "," + shot.y + "\n";


        if (lastHit != null) {
            output += "Last Hit:" + lastHit.x + "," + lastHit.y + "\n";
        }

        output += "Hit: " + hit + "\n";
        output += "Sunk: " + shipSunk + "\n";
        output += "Sinking: " + sinking + "\n";
        output += "Direction: " + targetDirection + "\n";

        output += "Direction Changed: " + directionChanged + "\n";
        output += "Hits: " + targetHitCount + "\n";
        output += "Consec Hits: " + consecutiveDirectionHits.size() + "\n";
        output += "Targets: " + targets.size() + "\n";
        output += "To Sink: " + shipsToSink + "\n";

        output += "Targeted Adjacents: " + targetedAdjacents + "\n";
        output += "Targeted Edges: " + targetedEdges + "\n";

        output += "Ships Remaining: " + shipsRemaining + "\n";
        output += "Targets Remaining: " + (totalShipPoints - shipPoints.size()) + "\n";
        output += "Parity: " + parity + "\n";
        output += "To Sink: " + shipsToSink + "\n";

        String shipsText = "[";
        for (int i = 0; i < ships.size(); i++) {
            shipsText += ships.get(i);
            if (i < ships.size() - 1) {
                shipsText += ", ";
            }
        }

        output += "Ships: " + shipsText + "]" + "\n";

        return output;
    }

    /**
     * Outline the cell we shot at
     */
    public void showSelect(Point shot) {
        double startPosX = (canvasWidth / 2) - ((BattleShip.cellSize * gameSize) / 2);
        double startPosY = (canvasHeight / 2) - ((BattleShip.cellSize * gameSize) / 2);
        gc.setStroke(Color.RED);
        gc.strokeRect(startPosX + (shot.x * BattleShip.cellSize), startPosY + (shot.y * BattleShip.cellSize), BattleShip.cellSize, BattleShip.cellSize);
    }

    /**
     * Outline the cells we are targeting
     */
    public void showTargets() {
        double startPosX = (canvasWidth / 2) - ((BattleShip.cellSize * gameSize) / 2);
        double startPosY = (canvasHeight / 2) - ((BattleShip.cellSize * gameSize) / 2);

        gc.setStroke(Color.YELLOW);
        for (int i = 0; i < lowPriorityTargets.size(); i++) {
            gc.strokeRect(startPosX + (lowPriorityTargets.get(i).x * BattleShip.cellSize), startPosY + (lowPriorityTargets.get(i).y * BattleShip.cellSize), BattleShip.cellSize, BattleShip.cellSize);
        }

        gc.setStroke(Color.GREEN);
        for (int i = 0; i < targets.size(); i++) {
            gc.strokeRect(startPosX + (targets.get(i).x * BattleShip.cellSize), startPosY + (targets.get(i).y * BattleShip.cellSize), BattleShip.cellSize, BattleShip.cellSize);
        }
    }

    /**
     * Fill in the cells with darker greys based on their probability
     */
    public void showProbability() {
        double startPosX = (canvasWidth / 2) - ((BattleShip.cellSize * gameSize) / 2);
        double startPosY = (canvasHeight / 2) - ((BattleShip.cellSize * gameSize) / 2);

        //Map the range of probabilities to a range specific range to give us nice shade values
        int lowestShade = 64;
        int highestShade = 200;
        if (highestProbability - lowestProbability == 0) {
            highestProbability++;
        }
        int slope = (highestShade - lowestShade) / (highestProbability - lowestProbability);

        for (int y = 0; y < gameSize; y++) {
            for (int x = 0; x < gameSize; x++) {
                if (probabilityBoard[x][y] == 0) {
                    continue;
                }
                int shade = lowestShade + slope * (probabilityBoard[x][y] - lowestProbability);//get the translated probability value

                gc.setFill(Color.rgb(255 - shade, 255 - shade, 255 - shade));
                gc.fillRect(startPosX + (x * BattleShip.cellSize), startPosY + (y * BattleShip.cellSize), BattleShip.cellSize, BattleShip.cellSize);
            }
        }
    }

    /**
     * Display the porbability value in each cell
     */
    public void showProbabilityValue() {
        double startPosX = (canvasWidth / 2) - ((BattleShip.cellSize * gameSize) / 2);
        double startPosY = (canvasHeight / 2) - ((BattleShip.cellSize * gameSize) / 2);
        gc.setFont(Font.font(12));
        for (int y = 0; y < gameSize; y++) {
            for (int x = 0; x < gameSize; x++) {
                gc.setFill(Color.BLACK);
                gc.fillText("" + probabilityBoard[x][y], startPosX + (x * BattleShip.cellSize) + BattleShip.cellSize / 2, startPosY + (y * BattleShip.cellSize) + BattleShip.cellSize / 2);
            }
        }


    }

    /**
     * Get the loweest probability - used for determing shade of grey to use
     */
    public void getLowestProbability() {
        lowestProbability = highestProbability;
        for (int y = 0; y < gameSize; y++) {
            for (int x = 0; x < gameSize; x++) {
                if (probabilityBoard[x][y] > 0 && probabilityBoard[x][y] < lowestProbability) {
                    lowestProbability = probabilityBoard[x][y];
                }
            }
        }
    }
}
