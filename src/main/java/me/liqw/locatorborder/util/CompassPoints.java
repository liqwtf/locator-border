package me.liqw.locatorborder.util;

public class CompassPoints {
    public static final Point[] POINTS = new Point[] {
            new Point("S", 0.0f, false),
            new Point("SE", 45.0f, true),
            new Point("W", 90.0f, false),
            new Point("SW", 135.0f, true),
            new Point("N", 180.0f, false),
            new Point("NW", 225.0f, true),
            new Point("E", 270.0f, false),
            new Point("NE", 315.0f, true)
    };

    public record Point(String label, float angle, boolean isIntercardinal) {
        public int getColor() {
            if (label.equals("N")) return 0xFFFF5555;
            return isIntercardinal ? 0xFFAAAAAA : 0xFFFFFFFF;
        }
    }
}
