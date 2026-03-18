package me.liqw.locatorborder.util;

import com.mojang.datafixers.util.Either;
import me.liqw.locatorborder.config.LocatorBorderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.UUID;
import java.util.WeakHashMap;

public class ScreenBounds {
    private static final float HOTBAR_WIDTH = 182.0f + 24.0f;
    private static final float FADE_BUFFER = 48.0f;
    private static final float FOCAL_ANGLE_THRESHOLD = 15.0f;

    private static final WeakHashMap<Either<UUID, String>, Float> animationStates = new WeakHashMap<>();

    public Minecraft minecraft;
    public GuiGraphics graphics;
    public LocatorBorderConfig config;
    public TrackedWaypoint waypoint;

    public ScreenBounds(Minecraft minecraft, GuiGraphics graphics, LocatorBorderConfig config, TrackedWaypoint waypoint) {
        this.minecraft = minecraft;
        this.graphics = graphics;
        this.config = config;
        this.waypoint = waypoint;
    }

    private Point getProjection(float centerX, float centerY, float directionX, float directionY, int margin) {
        float boundsX = centerX - margin;
        float boundsY = centerY - margin;

        float scaleX = Math.abs(directionX / Math.max(0.0001f, boundsX / centerX));
        float scaleY = Math.abs(directionY / Math.max(0.0001f, boundsY / centerY));
        float hitScale = Math.max(scaleX, scaleY);

        return new Point(
                centerX + (directionX / hitScale) * boundsX,
                centerY + (directionY / hitScale) * boundsY
        );
    }

    private boolean isMouseHovered(Point p1, Point p2, float width, float height) {
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        return mouseX >= (Math.min(p1.x, p2.x) - width / 2f) && mouseX <= (Math.max(p1.x, p2.x) + width / 2f) &&
                mouseY >= (Math.min(p1.y, p2.y) - height / 2f) && mouseY <= (Math.max(p1.y, p2.y) + height / 2f);
    }

    public RenderState compute(float angle, float width, float height) {
        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;

        double radians = Math.toRadians(angle);
        float directionX = (float) Math.sin(radians);
        float directionY = (float) -Math.cos(radians);

        Point outer = getProjection(centerX, centerY, directionX, directionY, config.margin);
        Point inner = getProjection(centerX, centerY, directionX, directionY, config.margin + config.focusWaypoint.inset);

        boolean focused = config.focusWaypoint.enabled && switch (config.focusWaypoint.trigger) {
            case Hover -> isMouseHovered(outer, inner, width, height);
            case Focal -> Math.abs(angle) < FOCAL_ANGLE_THRESHOLD;
            case PlayerList -> minecraft.options.keyPlayerList.isDown();
        };

        float currentProgress = animationStates.getOrDefault(this.waypoint.id(), 0.0f);
        float targetProgress = focused ? 1.0f : 0.0f;

        if (config.animations) {
            if (Math.abs(currentProgress - targetProgress) > 0.001f) {
                currentProgress = Mth.lerp(0.2f, currentProgress, targetProgress);
                animationStates.put(this.waypoint.id(), currentProgress);
            } else {
                currentProgress = targetProgress;
            }
        } else {
            currentProgress = targetProgress;
            animationStates.put(this.waypoint.id(), currentProgress);
        }

        float animatedInset = config.focusWaypoint.inset * currentProgress;

        Point position = getProjection(centerX, centerY, directionX, directionY, config.margin + (int)animatedInset);

        float alpha = position.y > centerY ? (config.animations ? Mth.clamp((Math.abs(position.x - centerX) - HOTBAR_WIDTH / 2.0f) / FADE_BUFFER, 0.0f, 1.0f) : (Math.abs(position.x - centerX) > HOTBAR_WIDTH / 2.0f ? 1.0f : 0.0f)) : 1.0f;

        return new RenderState(this.minecraft, position.x, position.y, directionX, directionY, centerX, centerY, alpha, currentProgress, focused);
    }

    public void project(float angle, float width, float height, DrawCallback callback) {
        RenderState state = compute(angle, width, height);

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

    private record Point(float x, float y) {}

    public record RenderState(Minecraft minecraft, float x, float y, float directionX, float directionY, float centerX, float centerY, float alpha, float animationProgress, boolean focused) {
        public int setAlpha(int color) {
            return (color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * this.alpha) << 24);
        }

        public boolean isFocused() {
            return this.focused;
        }
    }
}