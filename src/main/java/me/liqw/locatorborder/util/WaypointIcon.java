package me.liqw.locatorborder.util;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.Optional;
import java.util.UUID;

public class WaypointIcon {
    private static final int BASE_DOT_SIZE = 9;
    private static final int FACE_OUTLINE_PX = 1;

    private static final LocatorBorderConfig.PlayerSpecificConfig.Override DEFAULT_OVERRIDE = new LocatorBorderConfig.PlayerSpecificConfig.Override();

    private final Minecraft client;
    private final LocatorBorderConfig config;

    public WaypointIcon(Minecraft client, LocatorBorderConfig config) {
        this.client = client;
        this.config = config;
    }

    public int getBaseSize(TrackedWaypoint waypoint, Entity cameraEntity) {
        boolean renderPlayerFace = config.renderPlayerFace.enabled && waypoint.id().left().isPresent();

        if (renderPlayerFace) {
            float distance = Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));
            return getPlayerFaceSize(distance);
        }

        return BASE_DOT_SIZE;
    }

    public void render(GuiGraphics graphics, ScreenBounds.RenderState state, Entity cameraEntity, TrackedWaypoint waypoint) {
        UUID uuid = waypoint.id().left().orElse(null);
        PlayerInfo player = uuid != null ? client.getConnection().getPlayerInfo(uuid) : null;
        boolean renderPlayerFace = config.renderPlayerFace.enabled && uuid != null;

        float distance = Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));
        int baseSize = renderPlayerFace ? getPlayerFaceSize(distance) : BASE_DOT_SIZE;
        float scale = getIconScale(player, state.animationProgress());
        int size = (int) (baseSize * scale);

        if (renderPlayerFace) {
            PlayerSkin skin = player != null ? player.getSkin() : DefaultPlayerSkin.get(uuid);
            int outlineColor = getOutlineColor(waypoint, config.renderPlayerFace.color);
            int outlineSize = size + Math.max(1, (int) (FACE_OUTLINE_PX * scale)) * 2;

            graphics.fill(-outlineSize / 2, -size / 2, (-outlineSize / 2) + outlineSize, (-size / 2) + size, state.setAlpha(outlineColor));
            graphics.fill(-size / 2, -outlineSize / 2, (-size / 2) + size, (-outlineSize / 2) + outlineSize, state.setAlpha(outlineColor));
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size, state.setAlpha(0xFFFFFFFF));
        } else {
            WaypointStyle style = client.getWaypointStyles().get(waypoint.icon().style);
            int color = getWaypointColor(waypoint, config.color);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, state.setAlpha(color));
        }

        if (player != null && config.focusWaypoint.displayName && state.animationProgress() > 0f) {
            displayName(graphics, player.getProfile().name(), size, state);
        }
    }

    private int getPlayerFaceSize(float distance) {
        if (config.renderPlayerFace.distanceScale) {
            if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) return 4;
            if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) return 6;
        }
        return 8;
    }

    private float getIconScale(PlayerInfo player, float animationProgress) {
        float baseScale = (player != null) ? config.overrideCache.getOrDefault(player.getProfile().name().toLowerCase(), DEFAULT_OVERRIDE).scale : 1.0f;

        return Mth.lerp(animationProgress, baseScale, baseScale * config.focusWaypoint.scale);
    }

    private Optional<Integer> getOverrideColor(TrackedWaypoint waypoint) {
        return waypoint.id().left().map(client.getConnection()::getPlayerInfo).map(info ->
                config.overrideCache.get(info.getProfile().name().toLowerCase())
        ).map(o -> 0xFF000000 | o.color);
    }

    private int getWaypointColor(TrackedWaypoint waypoint, LocatorBorderConfig.WaypointColor source) {
        return getOverrideColor(waypoint).orElseGet(() -> switch (source) {
            case Waypoint -> waypoint.icon().color.orElseGet(() ->
                    waypoint.id().map(
                            uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                            string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                    ));
            case Team -> waypoint.id().left()
                    .map(client.getConnection()::getPlayerInfo)
                    .map(info -> client.level.getScoreboard().getPlayersTeam(info.getProfile().name()))
                    .map(team -> team.getColor().getColor())
                    .map(color -> 0xFF000000 | color)
                    .orElse(0xFFFFFFFF);
        });
    }

    private int getOutlineColor(TrackedWaypoint waypoint, LocatorBorderConfig.OutlineColor source) {
        return getOverrideColor(waypoint).orElseGet(() -> switch (source) {
            case Waypoint -> getWaypointColor(waypoint, LocatorBorderConfig.WaypointColor.Waypoint);
            case Team -> getWaypointColor(waypoint, LocatorBorderConfig.WaypointColor.Team);
            case Black -> 0xFF000000;
        });
    }

    private void displayName(GuiGraphics graphics, String name, int iconSize, ScreenBounds.RenderState state) {
        int textWidth = client.font.width(name);
        int lineHeight = client.font.lineHeight;
        int marginX = 6, marginY = 4;

        float directionX = state.directionX();
        float directionY = state.directionY();

        float screenWidth = state.centerX() * 2f;

        int x, y;

        if (Math.abs(directionY) > Math.abs(directionX)) {
            int centeredX = -textWidth / 2;
            float screenLeft = state.x() + centeredX;
            float screenRight = screenLeft + textWidth;

            if (screenLeft < 0 || screenRight > screenWidth) {
                x = screenLeft < 0 ? (iconSize / 2 + marginX) : (-iconSize / 2 - textWidth - marginX);
                y = -lineHeight / 2;
            } else {
                x = centeredX;
                y = (directionY > 0) ? (-iconSize / 2 - lineHeight - marginY) : (iconSize / 2 + marginY);
            }
        } else {
            x = (directionX > 0) ? (-iconSize / 2 - textWidth - marginX) : (iconSize / 2 + marginX);
            y = -lineHeight / 2;
        }

        graphics.drawString(client.font, name, x, y, state.setAlpha(0xFFFFFFFF, state.animationProgress()));
    }
}
