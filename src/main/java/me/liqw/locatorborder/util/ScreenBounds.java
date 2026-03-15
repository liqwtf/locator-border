package me.liqw.locatorborder.util;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class ScreenBounds {
    private static final float HOTBAR_WIDTH = 182.0f + 24.0f;
    private static final float FADE_BUFFER = 48.0f;

    public static RenderState compute(GuiGraphics graphics, float angle, float margin) {
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

        float alpha = y > centerY ? Mth.clamp((Math.abs(x - centerX) - HOTBAR_WIDTH / 2.0f) / FADE_BUFFER, 0.0f, 1.0f) : 1.0f;

        return new RenderState(x, y, directionX, directionY, centerX, centerY, alpha);
    }

    public static void project(GuiGraphics graphics, float angle, LocatorBorderConfig config, DrawCallback callback) {
        RenderState state = compute(graphics, angle, config.margin);

        if (state.alpha <= 0.0f) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(state.x(), state.y());
        callback.draw(graphics, state);
        graphics.pose().popMatrix();
    }

    @FunctionalInterface
    public interface DrawCallback {
        void draw(GuiGraphics graphics, RenderState state);
    }

    public record RenderState(float x, float y, float directionX, float directionY, float centerX, float centerY, float alpha) {
        public int setAlpha(int color) {
            return (color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * this.alpha) << 24);
        }

        public boolean isHovered(double mouseX, double mouseY, float radius) {
            float dx = (float) mouseX - x;
            float dy = (float) mouseY - y;
            return dx * dx + dy * dy <= radius * radius;
        }
    }
}