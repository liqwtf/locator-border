package me.liqw.locatorborder.util;

import me.liqw.locatorborder.config.Configuration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class WaypointState {
    private static final float HALF_HOTBAR_WIDTH = 182.0f / 2.0f + 12.0f;
    private static final float TRANSFORM_BUFFER = 48.0f;

    public static RenderState calculate(GuiGraphics graphics, float angle, float margin) {
        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;

        float boundsX = centerX - margin;
        float boundsY = centerY - margin;

        double radians = Math.toRadians(angle);
        float directionX = (float) Math.sin(radians);
        float directionY = (float) -Math.cos(radians);

        float scaleX = Math.abs(directionX / Math.max(0.0001f, boundsX / centerX));
        float scaleY = Math.abs(directionY / Math.max(0.0001f, boundsY / centerY));
        float hitScale = Math.max(scaleX, scaleY);

        float x = centerX + (directionX / hitScale) * boundsX;
        float y = centerY + (directionY / hitScale) * boundsY;

        float alpha = y > centerY ? Mth.clamp((Math.abs(x - centerX) - HALF_HOTBAR_WIDTH) / TRANSFORM_BUFFER, 0.0f, 1.0f) : 1.0f;

        return new RenderState(x, y, directionX, directionY, centerX, centerY, alpha);
    }

    public static void project(GuiGraphics graphics, float angle, Configuration config, DrawAction drawAction) {
        RenderState state = calculate(graphics, angle, config.margin);

        if (state.alpha <= 0.0f) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(state.x(), state.y());
        drawAction.draw(graphics, state);
        graphics.pose().popMatrix();
    }



    @FunctionalInterface
    public interface DrawAction {
        void draw(GuiGraphics graphics, RenderState state);
    }

    public record RenderState(float x, float y, float directionX, float directionY, float centerX, float centerY, float alpha) {
        public int setAlpha(int color) {
            return (color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * this.alpha) << 24);
        }
    }
}