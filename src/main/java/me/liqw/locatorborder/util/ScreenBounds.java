package me.liqw.locatorborder.util;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.HashMap;
import java.util.Map;

public class ScreenBounds {
    private static final float HOTBAR_WIDTH = 182f + 24f;
    private static final float HOTBAR_FADE_BUFFER = 48f;
    private static final float FOCAL_ANGLE_THRESHOLD = 15f;
    private static final float ANIMATION_DURATION_MS = 250f;

    private static final Map<String, Float> animationStates = new HashMap<>();

    private final Minecraft minecraft;
    private final GuiGraphics graphics;
    private final LocatorBorderConfig config;
    private final TrackedWaypoint waypoint;

    public ScreenBounds(Minecraft minecraft, GuiGraphics graphics, LocatorBorderConfig config, TrackedWaypoint waypoint) {
        this.minecraft = minecraft;
        this.graphics = graphics;
        this.config = config;
        this.waypoint = waypoint;
    }

    private static float smoothstep(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private float centerX() {
        return graphics.guiWidth() / 2.0f;
    }

    private float centerY() {
        return graphics.guiHeight() / 2.0f;
    }

    private Point project(float directionX, float directionY, int margin) {
        float cx = centerX();
        float cy = centerY();
        float boundsX = cx - margin;
        float boundsY = cy - margin;
        float scaleX = Math.abs(directionX / Math.max(0.0001f, boundsX / cx));
        float scaleY = Math.abs(directionY / Math.max(0.0001f, boundsY / cy));
        float hitScale = Math.max(scaleX, scaleY);

        return new Point(cx + (directionX / hitScale) * boundsX, cy + (directionY / hitScale) * boundsY);
    }

    private boolean isHovering(Point outer, float width, float height) {
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        return mouseX >= (outer.x - width / 2f) && mouseX <= (outer.x + width / 2f) && mouseY >= (outer.y - height / 2f) && mouseY <= (outer.y + height / 2f);
    }

    private float computeAlpha(float posX, float posY) {
        if (posY <= centerY()) return 1f;
        float dist = Math.abs(posX - centerX()) - HOTBAR_WIDTH / 2f;
        if (!config.animations) return dist > 0f ? 1f : 0f;

        return Mth.clamp(dist / HOTBAR_FADE_BUFFER, 0f, 1f);
    }

    public RenderState compute(float angle, float width, float height, float deltaTick) {
        double radians = Math.toRadians(angle);
        float directionX = (float) Math.sin(radians);
        float directionY = (float) -Math.cos(radians);

        Point outer = project(directionX, directionY, config.margin);

        boolean focused = config.focusWaypoint.enabled && switch (config.focusWaypoint.trigger) {
            case Hover -> isHovering(outer, width, height);
            case Focal -> Math.abs(angle) < FOCAL_ANGLE_THRESHOLD;
            case PlayerList -> minecraft.options.keyPlayerList.isDown();
        };

        String key = this.waypoint.id().toString();
        float currentProgress = animationStates.getOrDefault(key, 0.0f);
        float targetProgress = focused ? 1.0f : 0.0f;

        if (config.animations) {
            float step = (deltaTick * 50f) / ANIMATION_DURATION_MS;

            if (focused) {
                currentProgress = Math.min(currentProgress + step, 1.0f);
            } else {
                currentProgress = Math.max(currentProgress - step, 0.0f);
            }
        } else {
            currentProgress = targetProgress;
        }

        animationStates.put(key, currentProgress);

        float easedProgress = smoothstep(currentProgress);
        float animatedInset = config.focusWaypoint.inset * easedProgress;
        Point position = project(directionX, directionY, config.margin + (int) animatedInset);
        float alpha = computeAlpha(position.x, position.y);

        return new RenderState(position.x, position.y, directionX, directionY, centerX(), centerY(), alpha, easedProgress, currentProgress, focused);
    }

    public void project(float angle, float width, float height, float deltaTick, DrawCallback callback) {
        RenderState state = compute(angle, width, height, deltaTick);
        if (state.alpha <= 0.0f) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(state.x(), state.y());
        callback.draw(graphics, state);
        graphics.pose().popMatrix();
    }

    public void project(float angle, float width, float height, DrawCallback callback) {
        project(angle, width, height, 1.0f, callback);
    }

    @FunctionalInterface
    public interface DrawCallback {
        void draw(GuiGraphics graphics, RenderState state);
    }

    private record Point(float x, float y) {}

    public record RenderState(float x, float y, float directionX, float directionY, float centerX, float centerY,
                              float alpha, float animationProgress, float rawAnimationProgress, boolean focused) {
        public int setAlpha(int color) {
            return (color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * alpha) << 24);
        }

        public int setAlpha(int color, float extraAlpha) {
            float combined = alpha * Mth.clamp(extraAlpha, 0f, 1f);
            return (color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * combined) << 24);
        }
    }
}