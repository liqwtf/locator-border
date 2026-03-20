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

    private final Minecraft minecraft;
    private final LocatorBorderConfig config;

    public WaypointIcon(Minecraft minecraft, LocatorBorderConfig config) {
        this.minecraft = minecraft;
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
        PlayerInfo player = uuid != null ? minecraft.getConnection().getPlayerInfo(uuid) : null;
        boolean renderPlayerFace = config.renderPlayerFace.enabled && uuid != null;

        float distance = Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));
        int baseSize = renderPlayerFace ? getPlayerFaceSize(distance) : BASE_DOT_SIZE;
        float scale = getIconScale(state.animationProgress());
        int size = (int) (baseSize * scale);

        if (renderPlayerFace) {
            PlayerSkin skin = player != null ? player.getSkin() : DefaultPlayerSkin.get(uuid);
            int outlineColor = getOutlineColor(waypoint, config.renderPlayerFace.color);
            int outlineSize = size + Math.max(1, (int) (FACE_OUTLINE_PX * scale)) * 2;

            graphics.fill(-outlineSize / 2, -size / 2, (-outlineSize / 2) + outlineSize, (-size / 2) + size, state.setAlpha(outlineColor));
            graphics.fill(-size / 2, -outlineSize / 2, (-size / 2) + size, (-outlineSize / 2) + outlineSize, state.setAlpha(outlineColor));
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size, state.setAlpha(0xFFFFFFFF));
        } else {
            WaypointStyle style = minecraft.getWaypointStyles().get(waypoint.icon().style);
            int color = getWaypointColor(waypoint, config.color);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, state.setAlpha(color));
        }

        boolean showName = player != null && config.focusWaypoint.labels.showName;
        boolean showDistance = config.focusWaypoint.labels.showDistance;

        if ((showName || showDistance) && state.animationProgress() > 0f) {
            String nameText = showName ? player.getProfile().name() : null;
            String distanceText = showDistance ? (int) distance + "m" : null;

            renderLabels(graphics, nameText, distanceText, size, state);
        }
    }

    private int getPlayerFaceSize(float distance) {
        if (config.renderPlayerFace.distanceScale) {
            if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) return 4;
            if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) return 6;
        }
        return 8;
    }

    private float getIconScale(float animationProgress) {
        return Mth.lerp(animationProgress, 1.0f, config.focusWaypoint.scale);
    }

    private Optional<Integer> getOverrideColor(TrackedWaypoint waypoint) {
        return waypoint.id().left().map(minecraft.getConnection()::getPlayerInfo).map(info -> config.overrideCache.get(info.getProfile().name().toLowerCase())).map(o -> 0xFF000000 | o.color);
    }

    private int getWaypointColor(TrackedWaypoint waypoint, LocatorBorderConfig.WaypointColor source) {
        return getOverrideColor(waypoint).orElseGet(() -> switch (source) {
            case Waypoint -> waypoint.icon().color.orElseGet(() ->
                    waypoint.id().map(
                            uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                            string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                    ));
            case Team -> waypoint.id().left()
                    .map(minecraft.getConnection()::getPlayerInfo)
                    .map(info -> minecraft.level.getScoreboard().getPlayersTeam(info.getProfile().name()))
                    .map(team -> team.getColor().getColor())
                    .map(color -> 0xFF000000 | color)
                    .orElse(0xFFFFFFFF);
        });
    }

    private int getOutlineColor(TrackedWaypoint waypoint, LocatorBorderConfig.RenderPlayerFace.OutlineColor source) {
        return getOverrideColor(waypoint).orElseGet(() -> switch (source) {
            case Waypoint -> getWaypointColor(waypoint, LocatorBorderConfig.WaypointColor.Waypoint);
            case Team -> getWaypointColor(waypoint, LocatorBorderConfig.WaypointColor.Team);
            case Black -> 0xFF000000;
        });
    }

    private void renderLabels(GuiGraphics graphics, String name, String distanceText, int iconSize, ScreenBounds.RenderState state) {
        int lineHeight = minecraft.font.lineHeight;
        int lineSpacing = 2;
        int marginX = 6, marginY = 4;

        float directionX = state.directionX();
        float directionY = state.directionY();
        float screenWidth = state.centerX() * 2f;
        float alpha = state.animationProgress();

        int totalLines = (name != null ? 1 : 0) + (distanceText != null ? 1 : 0);
        int blockHeight = totalLines * lineHeight + (totalLines - 1) * lineSpacing;

        int nameWidth = name != null ? minecraft.font.width(name) : 0;
        int distanceWidth = distanceText != null ? minecraft.font.width(distanceText) : 0;
        int maxWidth = Math.max(nameWidth, distanceWidth);

        int anchorX, anchorY;

        if (Math.abs(directionY) > Math.abs(directionX)) {
            int centeredX = -maxWidth / 2;
            float screenLeft = state.x() + centeredX;
            float screenRight = screenLeft + maxWidth;

            if (screenLeft < 0 || screenRight > screenWidth) {
                anchorX = screenLeft < 0 ? (iconSize / 2 + marginX) : (-iconSize / 2 - maxWidth - marginX);
                anchorY = -blockHeight / 2;
            } else {
                anchorX = centeredX;
                anchorY = (directionY > 0) ? (-iconSize / 2 - blockHeight - marginY) : (iconSize / 2 + marginY);
            }
        } else {
            anchorX = (directionX > 0) ? (-iconSize / 2 - maxWidth - marginX) : (iconSize / 2 + marginX);
            anchorY = -blockHeight / 2;
        }

        int yOffset = 0;

        if (name != null) {
            int x = anchorX + (maxWidth - nameWidth) / 2;
            graphics.drawString(minecraft.font, name, x, anchorY + yOffset, state.setAlpha(0xFFFFFFFF, alpha));
            yOffset += lineHeight + lineSpacing;
        }

        if (distanceText != null) {
            int x = anchorX + (maxWidth - distanceWidth) / 2;
            graphics.drawString(minecraft.font, distanceText, x, anchorY + yOffset, state.setAlpha(0xFFAAAAAA, alpha));
        }
    }
}
