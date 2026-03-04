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
import net.minecraft.client.multiplayer.PlayerInfo;
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
    private int getWaypointColor(TrackedWaypoint waypoint, Configuration.WaypointColor source) {
        return switch (source) {
            case Waypoint -> waypoint.icon().color.orElseGet(() ->
                    waypoint.id().map(
                            uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                            string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                    ));
            case Team -> waypoint.id().left()
                    .map(this.minecraft.getConnection()::getPlayerInfo)
                    .map(info -> this.minecraft.level.getScoreboard().getPlayersTeam(info.getProfile().name()))
                    .map(team -> team.getColor().getColor())
                    .map(color -> 0xFF000000 | color)
                    .orElse(0xFFFFFFFF);
        };
    }

    @Unique int getOutlineColor(TrackedWaypoint waypoint, Configuration.OutlineColor source) {
        return switch (source) {
            case Waypoint -> getWaypointColor(waypoint, Configuration.WaypointColor.Waypoint);
            case Team -> getWaypointColor(waypoint, Configuration.WaypointColor.Team);
            case Black -> 0xFF000000;
        };
    }

    @Unique
    private void displayName(GuiGraphics graphics, String name, float angle, int iconSize, float alpha) {
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

        graphics.drawString(this.minecraft.font, name, x, y, RenderPosition.setAlpha(0xFFFFFFFF, alpha));
    }

    @Unique
    private void drawWaypoint(GuiGraphics graphics, Entity camera, TrackedWaypoint waypoint, Configuration config, float angle, float alpha) {
        UUID uuid = waypoint.id().left().orElse(null);
        PlayerInfo player = uuid != null ? this.minecraft.getConnection().getPlayerInfo(uuid) : null;
        boolean renderPlayerFace = config.renderPlayerFace.enabled && uuid != null;
        float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
        int size = renderPlayerFace ? getIconSize(distance) : DOT_SIZE;

        if (renderPlayerFace) {
            PlayerSkin skin = player != null ? player.getSkin() : DefaultPlayerSkin.get(uuid);
            int color = RenderPosition.setAlpha(getOutlineColor(waypoint, config.renderPlayerFace.color), alpha);
            int outlineSize = size + OUTLINE_TICKNESS * 2;

            graphics.fill(-outlineSize / 2, -size / 2, outlineSize / 2, size / 2, color);
            graphics.fill(-size / 2, -outlineSize / 2, size / 2, outlineSize / 2, color);
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size, RenderPosition.setAlpha(0xFFFFFFFF, alpha));
        } else {
            Icon icon = waypoint.icon();
            WaypointStyle style = this.minecraft.getWaypointStyles().get(icon.style);
            int color = getWaypointColor(waypoint, config.color);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, RenderPosition.setAlpha(color, alpha));
        }

        if (player != null) {
            boolean visible = switch (config.displayNames) {
                case PlayerList -> this.minecraft.options.keyPlayerList.isDown();
                case Focal -> Math.abs(angle) < 15.0f;
                case Always -> true;
                default -> false;
            };

            if (visible) {
                displayName(graphics, player.getProfile().name(), angle, size, alpha);
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

            RenderPosition.draw(graphics, angle, config, (g, alpha) -> {
                drawWaypoint(g, cameraEntity, waypoint, config, angle, alpha);
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
