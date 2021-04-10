package main;

public class TargetDirection {
    public String direction;
    public int lockedAxisValue;

    public TargetDirection(String dir, int val) {
        direction = dir;
        lockedAxisValue = val;
    }

    public String toString() {
        switch (direction) {
            case "Horizontal":
                return direction + " @ " + lockedAxisValue + " Y";
            case "Vertical":
                return direction + " @ " + lockedAxisValue + " X";
            default:
                return "";
        }
    }
}
