package me.liqw.locatorborder.util;

public class CardinalDirections {
    public static final Direction[] DIRECTIONS = new Direction[] {
            new Direction("S", 0.0f, false),
            new Direction("SE", 45.0f, true),
            new Direction("W", 90.0f, false),
            new Direction("SW", 135.0f, true),
            new Direction("N", 180.0f, false),
            new Direction("NW", 225.0f, true),
            new Direction("E", 270.0f, false),
            new Direction("NE", 315.0f, true)
    };

    public record Direction(String label, float angle, boolean isIntercardinal) {
        public int getColor() {
            if (label.equals("N")) return 0xFFFF5555;
            return isIntercardinal ? 0xFFAAAAAA : 0xFFFFFFFF;
        }
    }
}
