package me.liqw.locatorborder.util;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.config.Configuration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class RenderPosition {
    private static final float HALF_HOTBAR_WIDTH = 182.0f / 2.0f + 10.0f;
    private static final float TRANSFORM_BUFFER = 30.0f;

    public static Position calculate(GuiGraphics graphics, float angle, float offset) {
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

        float renderX = centerX + (dirX / projectionFactor) * edgeX;
        float renderY = centerY + (dirY / projectionFactor) * edgeY;

        return new Position(renderX, renderY);
    }

    public static void draw(GuiGraphics graphics, float angle, Configuration config, DrawAction drawAction) {
        Position position = calculate(graphics, angle, config.screenMargin);
        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;

        float scale = Mth.clamp((Math.abs(position.x() - centerX) - HALF_HOTBAR_WIDTH) / TRANSFORM_BUFFER, 0.0f, 1.0f);
        if (scale <= 0.0f && position.y() > centerY) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(position.x(), position.y());
        graphics.pose().scale(position.y() > centerY ? scale : 1.0f);
        drawAction.draw(graphics);
        graphics.pose().popMatrix();
    }

    @FunctionalInterface
    public interface DrawAction {
        void draw(GuiGraphics graphics);
    }

    public record Position(float x, float y) {}
}