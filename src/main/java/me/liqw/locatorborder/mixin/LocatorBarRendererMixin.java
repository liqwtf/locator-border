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
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.Waypoint.Icon;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    private final static int DOT_SIZE = 9;
    private static int FACE_SIZE = 8;

    /**
     * @author liqw
     * @reason render waypoints on the edge of the screen
     */
    @Overwrite
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = this.minecraft;
        Entity cameraEntity = minecraft.getCameraEntity();

        if (cameraEntity != null) {
            Level level = cameraEntity.level();
            TickRateManager tickRateManager = level.tickRateManager();
            PartialTickSupplier partialTickSupplier = entity ->
                    delta.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
            Configuration config = LocatorBorderClient.getConfig();

            float screenWidth = (float) graphics.guiWidth();
            float screenHeight = (float) graphics.guiHeight();
            float centerX = screenWidth / 2.0f;
            float centerY = screenHeight / 2.0f;

            float edgeX = centerX - config.borderOffset;
            float edgeY = centerY - config.borderOffset;

            if (minecraft.player != null) {
                minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, trackedWaypoint -> {
                    if (!trackedWaypoint.id().left().map(uuid -> uuid.equals(cameraEntity.getUUID())).orElse(false)) {
                        Camera camera = minecraft.gameRenderer.getMainCamera();
                        double d = trackedWaypoint.yawAngleToCamera(level, camera, partialTickSupplier);
                        double angleRad = Math.toRadians(d);

                        float relX = (float) Math.sin(angleRad);
                        float relY = (float) -Math.cos(angleRad);

                        float ratioX = Math.abs(relX / (edgeX / centerX));
                        float ratioY = Math.abs(relY / (edgeY / centerY));
                        float factor = Math.max(ratioX, ratioY);

                        float renderX = centerX + (relX / factor) * edgeX;
                        float renderY = centerY + (relY / factor) * edgeY;

                        if (!((ratioY >= ratioX) && relY > 0)) {
                            Icon icon = trackedWaypoint.icon();
                            WaypointStyle style = minecraft.getWaypointStyles().get(icon.style);
                            float distance = Mth.sqrt((float) trackedWaypoint.distanceSquared(cameraEntity));

                            if (distance >= WaypointStyle.DEFAULT_FAR_DISTANCE) {
                                FACE_SIZE = 4;
                            } else if (distance >= WaypointStyle.DEFAULT_NEAR_DISTANCE) {
                                FACE_SIZE = 6;
                            } else {
                                FACE_SIZE = 8;
                            }

                            if (config.renderPlayerFace) {
                                 trackedWaypoint.id().left().ifPresent(uuid -> {
                                    graphics.pose().pushMatrix();
                                    graphics.pose().translate(renderX, renderY);
                                    renderPlayerFace(graphics, uuid);
                                    graphics.pose().popMatrix();
                                });

                                return;
                            }

                            Identifier sprite = style.sprite(distance);

                            int color = icon.color.orElseGet(() -> trackedWaypoint.id().map(
                                    uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                                    string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                            ));

                            graphics.pose().pushMatrix();
                            graphics.pose().translate(renderX, renderY);
                            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -DOT_SIZE / 2, -DOT_SIZE / 2, DOT_SIZE, DOT_SIZE, color);
                            graphics.pose().popMatrix();
                        }
                    }
                });
            }
        }
    }

    /**
     * @author liqw
     * @reason don't render bar
     */
    @Overwrite
    public void renderBackground(GuiGraphics graphics, DeltaTracker delta) {
    }

    @Unique
    private void renderPlayerFace(GuiGraphics graphics, UUID uuid) {
        PlayerSkin skin = DefaultPlayerSkin.get(uuid);
        int radius = (FACE_SIZE + 2) / 2;
        int color = 0xFF000000;

        graphics.fill(-radius, -FACE_SIZE/2, radius, FACE_SIZE/2, color);
        graphics.fill(-FACE_SIZE/2, -radius, FACE_SIZE/2, radius, color);

        PlayerFaceRenderer.draw(graphics, skin, -FACE_SIZE / 2, -FACE_SIZE / 2, FACE_SIZE);
    }
}
