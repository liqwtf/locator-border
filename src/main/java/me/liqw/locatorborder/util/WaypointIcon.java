package me.liqw.locatorborder.util;

import com.mojang.blaze3d.platform.Window;
import me.liqw.locatorborder.config.Configuration;
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
import net.minecraft.world.waypoints.Waypoint;

import java.util.UUID;

public class WaypointIcon {
    private static final int BASE_DOT_SIZE = 9;
    private static final int FACE_OUTLINE_PX = 1;
    private static final float FOCAL_ANGLE_THRESHOLD = 15.0f;

    private final GuiGraphics graphics;
    private final Minecraft client;
    private final Configuration config;
    private final ScreenBounds.RenderState state;

    public WaypointIcon(GuiGraphics graphics, Minecraft client, Configuration config, ScreenBounds.RenderState state) {
        this.graphics = graphics;
        this.client = client;
        this.config = config;
        this.state = state;
    }

    public void render(Entity cameraEntity, TrackedWaypoint waypoint, float angle) {
        UUID uuid = waypoint.id().left().orElse(null);
        PlayerInfo player = uuid != null ? client.getConnection().getPlayerInfo(uuid) : null;
        boolean renderPlayerFace = config.renderPlayerFace.enabled && uuid != null;
        float distance = Mth.sqrt((float) waypoint.distanceSquared(cameraEntity));
        int size = renderPlayerFace ? getIconSize(distance) : BASE_DOT_SIZE;

        if (renderPlayerFace) {
            PlayerSkin skin = player != null ? player.getSkin() : DefaultPlayerSkin.get(uuid);
            int color = getOutlineColor(waypoint, config.renderPlayerFace.color);
            int outlineSize = size + FACE_OUTLINE_PX * 2;

            graphics.fill(-outlineSize / 2, -size / 2, outlineSize / 2, size / 2, state.setAlpha(color));
            graphics.fill(-size / 2, -outlineSize / 2, size / 2, outlineSize / 2, state.setAlpha(color));
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size, state.setAlpha(0xFFFFFFFF));
        } else {
            Waypoint.Icon icon = waypoint.icon();
            WaypointStyle style = client.getWaypointStyles().get(icon.style);
            int color = getWaypointColor(waypoint);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, state.setAlpha(color));
        }

        if (player != null) {
            Window window = client.getWindow();
            int mouseX = Mth.floor(client.mouseHandler.getScaledXPos(window));
            int mouseY = Mth.floor(client.mouseHandler.getScaledYPos(window));

            boolean visible = switch (config.displayNames) {
                case Hover -> state.isHovered(mouseX, mouseY, renderPlayerFace ? getIconSize(distance) : BASE_DOT_SIZE);
                case Focal -> Math.abs(angle) < FOCAL_ANGLE_THRESHOLD;
                case PlayerList -> client.options.keyPlayerList.isDown();
                case Always -> true;
                default -> false;
            };

            if (visible) {
                displayName(graphics, client, player.getProfile().name(), size, state);
            }
        }
    }

    private int getIconSize(float distance) {
        if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) return 4;
        if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) return 6;
        return 8;
    }

    private int getOverrideWaypointColor(TrackedWaypoint waypoint) {
        String name = waypoint.id().left()
                .map(client.getConnection()::getPlayerInfo)
                .map(info -> info.getProfile().name())
                .orElse(null);

        if (name != null) {
            for (Configuration.Overrides override : config.overrides) {
                if (override.name.equalsIgnoreCase(name)) {
                    return override.color;
                }
            }
        }

        return -1;
    }

    private int getWaypointColor(TrackedWaypoint waypoint) {
        int overrideColor = getOverrideWaypointColor(waypoint);
        if (overrideColor != -1) {
            return overrideColor;
        }

        return waypoint.icon().color.orElseGet(() ->
                waypoint.id().map(
                        uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                        string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                ));
    }

    private int getOutlineColor(TrackedWaypoint waypoint, Configuration.OutlineColor source) {
        int overrideColor = getOverrideWaypointColor(waypoint);
        if (overrideColor != -1) {
            return overrideColor;
        }

        return switch (source) {
            case Waypoint -> getWaypointColor(waypoint);
            case Team -> waypoint.id().left()
                    .map(client.getConnection()::getPlayerInfo)
                    .map(info -> client.level.getScoreboard().getPlayersTeam(info.getProfile().name()))
                    .map(team -> team.getColor().getColor())
                    .map(color -> 0xFF000000 | color)
                    .orElse(0xFFFFFFFF);
            case Black -> 0xFF000000;
        };
    }

    private void displayName(GuiGraphics graphics, Minecraft client, String name, int iconSize, ScreenBounds.RenderState state) {
        int width = client.font.width(name);
        int lineHeight = client.font.lineHeight;
        int marginX = 6, marginY = 4;

        float directionX = state.directionX();
        float directionY = state.directionY();

        int x, y;

        if (Math.abs(directionY) > Math.abs(directionX)) {
            int centeredX = -width / 2;
            float screenLeft  = state.x() + centeredX;
            float screenRight = state.x() + centeredX + width;

            if (screenLeft < 0 || screenRight > graphics.guiWidth()) {
                x = screenLeft < 0 ? (iconSize / 2 + marginX) : (-iconSize / 2 - width - marginX);
                y = -lineHeight / 2;
            } else {
                x = centeredX;
                y = (directionY > 0) ? (-iconSize / 2 - lineHeight - marginY) : (iconSize / 2 + marginY);
            }
        } else {
            x = (directionX > 0) ? (-iconSize / 2 - width - marginX) : (iconSize / 2 + marginX);
            y = -lineHeight / 2;
        }

        graphics.drawString(client.font, name, x, y, state.setAlpha(0xFFFFFFFF));
    }
}
