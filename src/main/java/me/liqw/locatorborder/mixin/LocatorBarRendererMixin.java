package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.config.Configuration;
import me.liqw.locatorborder.util.WaypointState;
import me.liqw.locatorborder.util.WaypointRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        ci.cancel();

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

            WaypointState.project(graphics, angle, config, (g, state) -> {
                WaypointRenderer renderer = new WaypointRenderer(g, this.minecraft, config, state);
                renderer.draw(cameraEntity, waypoint, angle);
            });
        });
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void onRenderBackground(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        ci.cancel();
    }
}
