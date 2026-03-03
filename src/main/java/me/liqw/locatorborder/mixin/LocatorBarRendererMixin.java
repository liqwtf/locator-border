package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorder;
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
    private static final int OUTLINE_TICKNESS = 1;

    @Unique
    private int getIconSize(float distance) {
        if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) return 4;
        if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) return 6;
        return 8;
    }

    @Unique
    private int getWaypointColor(TrackedWaypoint waypoint, Configuration.WaypointColor color) {
        return switch (color) {
            case Waypoint -> waypoint.icon().color.orElseGet(() ->
                    waypoint.id().map(
                            uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                            string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                    ));
            case Team -> waypoint.id().left()
                    .map(this.minecraft.level::getPlayerByUUID)
                    .map(player -> 0xFF000000 | player.getTeamColor())
                    .orElseGet(() -> getWaypointColor(waypoint, Configuration.WaypointColor.Waypoint));
        };
    }

    @Unique int getOutlineColor(TrackedWaypoint waypoint, Configuration.OutlineColor color) {
        return switch (color) {
            case Waypoint -> getWaypointColor(waypoint, Configuration.WaypointColor.Waypoint);
            case Team -> getWaypointColor(waypoint, Configuration.WaypointColor.Team);
            case Black -> 0xFF000000;
        };
    }

    @Unique
    private void displayName(GuiGraphics graphics, String name, float angle, int iconSize) {
        int width = this.minecraft.font.width(name);
        int lineHeight = this.minecraft.font.lineHeight;
        int marginX = 6, marginY = 4;

        double radians = Math.toRadians(angle);
        float dirX = (float) Math.sin(radians);
        float dirY = (float) -Math.cos(radians);

        int x, y;

        if (Math.abs(dirY) > Math.abs(dirX)) {
            x = -width / 2;
            y = (dirY > 0) ? (-iconSize / 2 - lineHeight - marginY) : (iconSize / 2 + marginY);
        } else {
            x = (dirX > 0) ? (-iconSize / 2 - width - marginX) : (iconSize / 2 + marginX);
            y = -lineHeight / 2;
        }

        graphics.drawString(this.minecraft.font, name, x, y, 0xFFFFFFFF);
    }

    @Unique
    private void drawWaypoint(GuiGraphics graphics, Entity camera, TrackedWaypoint waypoint, Configuration config, float angle) {
        float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
        Optional<UUID> uuid = waypoint.id().left();
        int size = (config.renderPlayerFace.enabled && uuid.isPresent())
                ? getIconSize(distance) : DOT_SIZE;

        if (config.renderPlayerFace.enabled && uuid.isPresent()) {
            PlayerSkin skin = DefaultPlayerSkin.get(uuid.get());
            int color = getOutlineColor(waypoint, config.renderPlayerFace.outlineColor);
            int outline = size + OUTLINE_TICKNESS * 2;

            graphics.fill(-outline / 2, -size / 2, outline / 2, size / 2, color);
            graphics.fill(-size / 2, -outline / 2, size / 2, outline / 2, color);
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size);
        } else {
            Icon icon = waypoint.icon();
            WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
            int color = getWaypointColor(waypoint, config.waypointColor);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, color);
        }

        if (uuid.isPresent()) {
            boolean visible = switch (config.displayNames) {
                case PlayerList -> this.minecraft.options.keyPlayerList.isDown();
                case Focal -> Math.abs(angle) < 15.0f;
                case Always -> true;
                default -> false;
            };

            if (visible) {
                var playerInfo = this.minecraft.getConnection().getPlayerInfo(uuid.get());
                if (playerInfo != null) {
                    displayName(graphics, playerInfo.getProfile().name(), angle, size);
                }
            }
        }
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
        Configuration config = LocatorBorder.getConfig();
        Camera camera = this.minecraft.gameRenderer.getMainCamera();

        boolean isFrozen = level.tickRateManager().isEntityFrozen(cameraEntity);
        PartialTickSupplier tickSupplier = entity -> delta.getGameTimeDeltaPartialTick(!isFrozen);

        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, waypoint -> {
            if (waypoint.id().left().filter(uuid -> uuid.equals(cameraEntity.getUUID())).isPresent()) return;

            float angle = (float) waypoint.yawAngleToCamera(level, camera, tickSupplier);

            RenderPosition.draw(graphics, angle, config, (g) -> {
                drawWaypoint(g, cameraEntity, waypoint, config, angle);
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
