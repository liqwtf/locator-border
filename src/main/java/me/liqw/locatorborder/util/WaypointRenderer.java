package me.liqw.locatorborder.util;

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

public class WaypointRenderer {
    private static final int DOT_SIZE = 9;
    private static final int OUTLINE = 1;

    private static int getIconSize(float distance) {
        if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) return 4;
        if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) return 6;
        return 8;
    }


    private static int getWaypointColor(Minecraft client, TrackedWaypoint waypoint, Configuration.WaypointColor source) {
        return switch (source) {
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
        };
    }

    private static int getOutlineColor(Minecraft client, TrackedWaypoint waypoint, Configuration.OutlineColor source) {
        return switch (source) {
            case Waypoint -> getWaypointColor(client, waypoint, Configuration.WaypointColor.Waypoint);
            case Team -> getWaypointColor(client, waypoint, Configuration.WaypointColor.Team);
            case Black -> 0xFF000000;
        };
    }

    private static void displayName(GuiGraphics graphics, Minecraft client, String name, int iconSize, WaypointState.RenderState state) {
        int width = client.font.width(name);
        int lineHeight = client.font.lineHeight;
        int marginX = 6, marginY = 4;

        float directionX = state.directionX();
        float directionY = state.directionY();

        int x, y;

        if (Math.abs(directionY) > Math.abs(directionX)) {
            x = -width / 2;
            y = (directionY > 0) ? (-iconSize / 2 - lineHeight - marginY) : (iconSize / 2 + marginY);
        } else {
            x = (directionX > 0) ? (-iconSize / 2 - width - marginX) : (iconSize / 2 + marginX);
            y = -lineHeight / 2;
        }

        graphics.drawString(client.font, name, x, y, state.setAlpha(0xFFFFFFFF));
    }

    public static void draw(GuiGraphics graphics, Minecraft client, Entity camera, TrackedWaypoint waypoint, Configuration config, float angle, WaypointState.RenderState state) {
        UUID uuid = waypoint.id().left().orElse(null);
        PlayerInfo player = uuid != null ? client.getConnection().getPlayerInfo(uuid) : null;
        boolean renderPlayerFace = config.renderPlayerFace.enabled && uuid != null;
        float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
        int size = renderPlayerFace ? getIconSize(distance) : DOT_SIZE;

        if (renderPlayerFace) {
            PlayerSkin skin = player != null ? player.getSkin() : DefaultPlayerSkin.get(uuid);
            int color = getOutlineColor(client, waypoint, config.renderPlayerFace.color);
            int outlineSize = size + OUTLINE * 2;

            graphics.fill(-outlineSize / 2, -size / 2, outlineSize / 2, size / 2, state.setAlpha(color));
            graphics.fill(-size / 2, -outlineSize / 2, size / 2, outlineSize / 2, state.setAlpha(color));
            PlayerFaceRenderer.draw(graphics, skin, -size / 2, -size / 2, size, state.setAlpha(0xFFFFFFFF));
        } else {
            Waypoint.Icon icon = waypoint.icon();
            WaypointStyle style = client.getWaypointStyles().get(icon.style);
            int color = getWaypointColor(client, waypoint, config.color);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), -size / 2, -size / 2, size, size, state.setAlpha(color));
        }

        if (player != null) {
            boolean visible = switch (config.displayNames) {
                case PlayerList -> client.options.keyPlayerList.isDown();
                case Focal -> Math.abs(angle) < 15.0f;
                case Always -> true;
                default -> false;
            };

            if (visible) {
                displayName(graphics, client, player.getProfile().name(), size, state);
            }
        }
    }
}
