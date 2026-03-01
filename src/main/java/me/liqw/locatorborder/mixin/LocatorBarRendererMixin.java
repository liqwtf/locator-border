package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorderClient;
import me.liqw.locatorborder.config.Configuration;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint.Icon;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    private static final int DOT_SIZE = 9;
    private static final int BORDER_PADDING = 2;

    @Unique
    private int getFaceSizeForDistance(WaypointStyle style, float distance) {
        if (distance < style.nearDistance()) return 8;
        if (distance < style.farDistance()) return 4;
        return 6;
    }

    @Unique
    private void renderPlayerFace(GuiGraphics graphics, UUID uuid, int size) {
        PlayerSkin skin = DefaultPlayerSkin.get(uuid);
        int borderSize = size + BORDER_PADDING;

        graphics.fill(-borderSize / 2, -size / 2, borderSize / 2, size / 2, 0xFF000000);
        graphics.fill(-size / 2, -borderSize / 2, size / 2, borderSize / 2, 0xFF000000);

        PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size);
    }

    @Unique
    private void renderDefaultIcon(GuiGraphics graphics, Icon icon, float distance, int color) {
        WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
        Identifier sprite = style.sprite(distance);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -DOT_SIZE / 2, -DOT_SIZE / 2, DOT_SIZE, DOT_SIZE, color);
    }

    @Unique
    private int getWaypointColor(TrackedWaypoint waypoint) {
        return waypoint.icon().color.orElseGet(() -> waypoint.id().map(
                uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
        ));
    }

    /**
     * @author liqw
     * @reason render waypoints on the edge of the screen
     */
    @Overwrite
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null || this.minecraft.player == null) return;

        Level level = cameraEntity.level();
        Configuration config = LocatorBorderClient.getConfig();

        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;
        float edgeX = centerX - config.borderOffset;
        float edgeY = centerY - config.borderOffset;

        boolean isFrozen = level.tickRateManager().isEntityFrozen(cameraEntity);
        PartialTickSupplier tickSupplier = entity -> delta.getGameTimeDeltaPartialTick(!isFrozen);

        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, waypoint -> {
            if (waypoint.id().left().filter(uuid -> uuid.equals(cameraEntity.getUUID())).isPresent()) return;

            Camera camera = this.minecraft.gameRenderer.getMainCamera();

            double angleRadians = Math.toRadians(waypoint.yawAngleToCamera(level, camera, tickSupplier));
            float directionX = (float) Math.sin(angleRadians);
            float directionY = (float) -Math.cos(angleRadians);

            float ratioX = Math.abs(directionX / (edgeX / centerX));
            float ratioY = Math.abs(directionY / (edgeY / centerY));
            float projectionFactor = Math.max(ratioX, ratioY);

            boolean isHittingBottomEdge = (ratioY >= ratioX) && directionY > 0;
            if (isHittingBottomEdge) return;

            float renderX = centerX + (directionX / projectionFactor) * edgeX;
            float renderY = centerY + (directionY / projectionFactor) * edgeY;

            Icon icon = waypoint.icon();
            WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
            float distance =  Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));
            int faceSize = getFaceSizeForDistance(style, distance);

            graphics.pose().pushMatrix();
            graphics.pose().translate(renderX, renderY);

            if (config.renderPlayerFace) {
                waypoint.id().left().ifPresent(uuid -> renderPlayerFace(graphics, uuid, faceSize));
            } else {
                renderDefaultIcon(graphics, icon, distance, getWaypointColor(waypoint));
            }

            graphics.pose().popMatrix();
        });
    }

    /**
     * @author liqw
     * @reason don't render bar
     */
    @Overwrite
    public void renderBackground(GuiGraphics graphics, DeltaTracker delta) {
    }
}
