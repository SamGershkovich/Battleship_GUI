//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package main;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

public final class Ship {
    private boolean isPlaced = false;
    private Point location;
    private ShipOrientation orientation;
    private int length;

    public Ship(int length) {
        if (length <= 1) {
            throw new IllegalArgumentException("Invalid length specified: must be >= 1 ");
        } else {
            this.length = length;
        }
    }

    public boolean getIsPlaced() {
        return this.isPlaced;
    }

    public Point getLocation() {
        return this.location;
    }

    public ShipOrientation getOrientation() {
        return this.orientation;
    }

    public int getLength() {
        return this.length;
    }

    public boolean place(int boardSize, Point location, ShipOrientation orientation, ArrayList<Ship> ships) {
        this.location = location;
        this.orientation = orientation;
        this.isPlaced = false;
        if (!this.isValid(boardSize)) {
            return false;
        } else {
            if (ships != null) {
                Iterator var5 = ships.iterator();

                label48:
                while(true) {
                    Ship s;
                    int x;
                    int y;
                    do {
                        if (!var5.hasNext()) {
                            break label48;
                        }

                        s = (Ship)var5.next();
                        if (s.orientation == ShipOrientation.Horizontal) {
                            x = s.getLocation().y;

                            for(y = s.getLocation().x; y < s.getLocation().x + s.getLength(); ++y) {
                                if (this.isAt(new Point(y, x))) {
                                    return false;
                                }
                            }
                        }
                    } while(s.orientation != ShipOrientation.Vertical);

                    x = s.getLocation().x;

                    for(y = s.getLocation().y; y < s.getLocation().y + s.getLength(); ++y) {
                        if (this.isAt(new Point(x, y))) {
                            return false;
                        }
                    }
                }
            }

            this.isPlaced = true;
            return true;
        }
    }

    public boolean isAt(Point p) {
        if (this.getOrientation() == ShipOrientation.Horizontal) {
            return this.location.y == p.y && this.location.x <= p.x && this.location.x + this.length > p.x;
        } else {
            return this.location.x == p.x && this.location.y <= p.y && this.location.y + this.length > p.y;
        }
    }

    public boolean isValid(int boardSize) {
        if (this.location.x >= 0 && this.location.y >= 0) {
            if (this.orientation == ShipOrientation.Horizontal) {
                if (this.location.y >= boardSize || this.location.x + this.length > boardSize) {
                    return false;
                }
            } else if (this.location.x >= boardSize || this.location.y + this.length > boardSize) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }
}
