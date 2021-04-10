package main;

import java.awt.*;
import java.util.ArrayList;

public class MyShip {
    public TargetDirection shipDirection = new TargetDirection("", -1);
    public ArrayList<Point> points = new ArrayList<>();
    public Point origin;

    public MyShip(TargetDirection direction, Point origin) {
        switch (direction.direction) {
            case "Horizontal":
                shipDirection.direction = "Vertical";
                shipDirection.lockedAxisValue = origin.x;
                break;
            case "Vertical":
                shipDirection.direction = "Horizontal";
                shipDirection.lockedAxisValue = origin.y;
                break;
        }
        this.origin = origin;
        points.add(origin);
    }

    @Override
    public String toString() {
        return "{" + shipDirection + ", " + points.size() + '}';
    }
}

