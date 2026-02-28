package me.liqw.locatorborder.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.Waypoint.Icon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    private static final int DOT_SIZE = 9;
    private static final int MARGIN = 4;
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * @author liqw
     * @reason rewrite render
     */
    @Overwrite
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = this.minecraft;
        Entity cameraEntity = minecraft.getCameraEntity();

        if (cameraEntity != null) {
            Level level = cameraEntity.level();
            TickRateManager tickRateManager = level.tickRateManager();
            PartialTickSupplier partialTickSupplier = entity -> deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));

            float screenWidth = (float) guiGraphics.guiWidth();
            float screenHeight = (float) guiGraphics.guiHeight();
            float centerX = screenWidth / 2.0f;
            float centerY = screenHeight / 2.0f;

            float edgeX = centerX - MARGIN;
            float edgeY = centerY - MARGIN;

            minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, trackedWaypoint -> {
                if (!trackedWaypoint.id().left().map(uUID -> uUID.equals(cameraEntity.getUUID())).orElse(false)) {
                    double d = trackedWaypoint.yawAngleToCamera(level, minecraft.gameRenderer.getMainCamera(), partialTickSupplier);
                    double angleRad = Math.toRadians(d);

                    float relX = (float) Math.sin(angleRad);
                    float relY = (float) -Math.cos(angleRad);

                    float ratioX = Math.abs(relX / (edgeX / centerX));
                    float ratioY = Math.abs(relY / (edgeY / centerY));
                    float factor = Math.max(ratioX, ratioY);

                    float renderX = centerX + (relX / factor) * edgeX;
                    float renderY = centerY + (relY / factor) * edgeY;

                    boolean isOnBottomRail = (ratioY >= ratioX) && relY > 0;

                    if (!isOnBottomRail) {
                        Icon icon = trackedWaypoint.icon();
                        WaypointStyle waypointStyle = minecraft.getWaypointStyles().get(icon.style);
                        float distance = Mth.sqrt((float) trackedWaypoint.distanceSquared(cameraEntity));
                        Identifier sprite = waypointStyle.sprite(distance);

                        int color = icon.color.orElseGet(() -> trackedWaypoint.id().map(
                                uUID -> ARGB.setBrightness(ARGB.color(255, uUID.hashCode()), 0.9F),
                                string -> ARGB.setBrightness(ARGB.color(255, string.hashCode()), 0.9F)
                        ));

                        guiGraphics.pose().pushMatrix();
                        guiGraphics.pose().translate(renderX, renderY);
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -DOT_SIZE / 2, -DOT_SIZE / 2, DOT_SIZE, DOT_SIZE, color);
                        guiGraphics.pose().popMatrix();
                    }
                }
            });
        }
    }

    /**
     * @author liqw
     * @reason don't render the background
     */
    @Overwrite
    public void renderBackground(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    }
}
