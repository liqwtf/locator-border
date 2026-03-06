package me.liqw.locatorborder.mixin;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.config.Configuration;
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

    @ModifyVariable(method = "nextContextualInfoState", at = @At("STORE"), ordinal = 0)
    private boolean forceLocatorStateOff(boolean original) {
        return false;
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    private void renderLocatorBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft.player != null && this.minecraft.player.connection.getWaypointManager().hasWaypoints()) {
            LocatorBarRenderer renderer = new LocatorBarRenderer(this.minecraft);
            renderer.render(guiGraphics, deltaTracker);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCompass(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Configuration config = LocatorBorder.getConfig();

        if (this.minecraft.options.hideGui || !config.cardinalDirections) return;

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) return;

        float yaw = cameraEntity.getYRot();

        for (CompassPoints.Point point : CompassPoints.POINTS) {
            if (point.isIntercardinal() && !config.intercardinal) continue;

            ScreenBounds.project(graphics, point.angle() - yaw, config, (g, state) -> {
                int color = state.setAlpha(point.getColor());

                g.drawCenteredString(this.minecraft.font, point.label(), 0, -this.minecraft.font.lineHeight / 2, color);
            });
        }
    }
}