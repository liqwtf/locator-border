package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.liqw.locatorborder.util.CompassPoints;
import me.liqw.locatorborder.util.ScreenBounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;
    private LocatorBarRenderer renderer;

    @ModifyVariable(method = "nextContextualInfoState", at = @At("STORE"), ordinal = 0)
    private boolean forceLocatorStateOff(boolean original) {
        if (LocatorBorder.getConfig().enabled) return false;
        return original;
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    private void renderLocatorBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (config.enabled && this.minecraft.player != null && this.minecraft.player.connection.getWaypointManager().hasWaypoints()) {
            if (this.renderer == null) {
                this.renderer = new LocatorBarRenderer(this.minecraft);
            }

            this.renderer.render(guiGraphics, deltaTracker);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCompass(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (!config.enabled || this.minecraft.options.hideGui || !config.compass.enabled) return;

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) return;

        float yaw = cameraEntity.getYRot();

        for (CompassPoints.Point point : CompassPoints.POINTS) {
            if (point.isIntercardinal() && !config.compass.intercardinal) continue;

            ScreenBounds bounds = new ScreenBounds(this.minecraft, graphics, config);

            bounds.project(graphics, point.angle() - yaw, config, (g, state) -> {
                g.drawCenteredString(this.minecraft.font, point.label(), 0, -this.minecraft.font.lineHeight / 2, state.setAlpha(point.getColor()));
            });
        }
    }
}