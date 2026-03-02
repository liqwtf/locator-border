package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorderClient;
import me.liqw.locatorborder.config.Configuration;
import me.liqw.locatorborder.util.RenderPosition;
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

import java.util.Optional;
import java.util.UUID;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    private static final int DOT_SIZE = 9;
    private static final int BORDER_PADDING = 2;

    @Unique
    private int getFaceSizeForDistance(WaypointStyle style, float distance) {
        if (distance >= style.farDistance()) return 4;
        if (distance >= style.nearDistance()) return 6;
        return 8;
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
    private void renderWaypoint(GuiGraphics graphics, Icon icon, float distance, int color) {
        WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
        Identifier sprite = style.sprite(distance);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -DOT_SIZE / 2, -DOT_SIZE / 2, DOT_SIZE, DOT_SIZE, color);
    }

    @Unique
    private int getWaypointColor(TrackedWaypoint waypoint) {
        return waypoint.icon().color.orElseGet(() ->
                waypoint.id().map(
                    uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                    string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                )
        );
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
        Camera camera = this.minecraft.gameRenderer.getMainCamera();

        boolean isFrozen = level.tickRateManager().isEntityFrozen(cameraEntity);
        PartialTickSupplier tickSupplier = entity -> delta.getGameTimeDeltaPartialTick(!isFrozen);

        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, waypoint -> {
            if (waypoint.id().left().filter(uuid -> uuid.equals(cameraEntity.getUUID())).isPresent()) return;

            float angle = (float) waypoint.yawAngleToCamera(level, camera, tickSupplier);

            RenderPosition.draw(graphics, angle, config.borderOffset, (g) -> {
                Icon icon = waypoint.icon();
                WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
                float distance = Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));

                Optional<UUID> uuid = waypoint.id().left();

                if (config.renderPlayerFace && uuid.isPresent()) {
                    renderPlayerFace(g, uuid.get(), getFaceSizeForDistance(style, distance));
                } else {
                    renderWaypoint(g, icon, distance, getWaypointColor(waypoint));
                }
            });
        });
    }

    /**
     * @author liqw
     * @reason don't render bar
     */
    @Overwrite
    public void renderBackground(GuiGraphics graphics, DeltaTracker delta) {}
}
