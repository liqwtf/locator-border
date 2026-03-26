package me.liqw.locatorborder.fabric.mixin;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.liqw.locatorborder.util.ScreenBounds;
import me.liqw.locatorborder.util.WaypointIcon;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (!config.enabled) return;

        ci.cancel();

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null || this.minecraft.player == null) return;

        Level level = cameraEntity.level();
        Camera camera = this.minecraft.gameRenderer.getMainCamera();

        boolean isFrozen = level.tickRateManager().isEntityFrozen(cameraEntity);
        PartialTickSupplier tickSupplier = entity -> delta.getGameTimeDeltaPartialTick(!isFrozen);

        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, waypoint -> {
            if (waypoint.id().left().filter(cameraEntity.getUUID()::equals).isPresent()) return;

            ScreenBounds bounds = new ScreenBounds(this.minecraft, graphics, config, waypoint);
            WaypointIcon icon = new WaypointIcon(this.minecraft, config);
            float angle = (float) waypoint.yawAngleToCamera(level, camera, tickSupplier);
            int size = icon.getBaseSize(waypoint, cameraEntity);

            bounds.project(angle, size, size, (g, state) -> {
                icon.render(g, state, cameraEntity, waypoint);
            });
        });
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void onRenderBackground(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (LocatorBorder.getConfig().enabled) ci.cancel();
    }
}