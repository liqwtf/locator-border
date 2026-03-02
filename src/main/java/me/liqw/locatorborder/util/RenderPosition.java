package me.liqw.locatorborder.util;

import net.minecraft.client.gui.GuiGraphics;

public class RenderPosition {
    public record Result(float x, float y, boolean isBottom) {}

    public static Result calculate(GuiGraphics graphics, float angle, float offset) {
        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;

        float edgeX = centerX - offset;
        float edgeY = centerY - offset;

        double radians = Math.toRadians(angle);
        float dirX = (float) Math.sin(radians);
        float dirY = (float) -Math.cos(radians);

        float ratioX = Math.abs(dirX / Math.max(0.0001f, edgeX / centerX));
        float ratioY = Math.abs(dirY / Math.max(0.0001f, edgeY / centerY));
        float projectionFactor = Math.max(ratioX, ratioY);

        boolean isBottom = (ratioY >= ratioX) && dirY > 0;

        float renderX = centerX + (dirX / projectionFactor) * edgeX;
        float renderY = centerY + (dirY / projectionFactor) * edgeY;

        return new Result(renderX, renderY, isBottom);
    }

    public static void draw(GuiGraphics graphics, float angle, float offset, DrawAction drawAction) {
        Result result = calculate(graphics, angle, offset);

        if (result.isBottom()) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(result.x(), result.y());
        drawAction.draw(graphics);
        graphics.pose().popMatrix();
    }

    @FunctionalInterface
    public interface DrawAction {
        void draw(GuiGraphics graphics);
    }
}