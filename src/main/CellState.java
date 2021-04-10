//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package main;

public enum CellState {
    Empty,
    Hit,
    Miss;

    private CellState() {
    }

    public String toString() {
        switch(this) {
            case Empty:
                return ".";
            case Hit:
                return "X";
            case Miss:
                return "o";
            default:
                return "?";
        }
    }
}
