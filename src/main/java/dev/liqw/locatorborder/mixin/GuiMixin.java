package dev.liqw.locatorborder.mixin;

import dev.liqw.locatorborder.LocatorBorder;
import dev.liqw.locatorborder.config.LocatorBorderConfig;
import dev.liqw.locatorborder.util.CardinalDirections;
import dev.liqw.locatorborder.util.ScreenBounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private LocatorBarRenderer renderer;

    //? if fabric {
    //~ if <26 '"canShowLocatorInfo"' -> '"bl"'
    @ModifyVariable(method = "nextContextualInfoState", at = @At("STORE"), name = "canShowLocatorInfo")
    //? } else {
    /*//~ if <26 '"canShowLocatorInfo"' -> '"flag"'
    @ModifyVariable(method = "nextContextualInfoState", at = @At("STORE"), name = "canShowLocatorInfo")
    *///? }
    public boolean forceLocatorStateOff(boolean original) {
        if (LocatorBorder.getConfig().enabled) return false;
        return original;
    }

    //? fabric {
    //~ if <26 'extractHotbarAndDecorations' -> 'renderHotbarAndDecorations'
    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    //? } else
    //@Inject(method = "renderContextualInfoBar", at = @At("HEAD"), cancellable = true)
    public void renderLocatorBorder(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (config.enabled && this.minecraft.player != null && this.minecraft.player.connection.getWaypointManager().hasWaypoints()) {
            if (this.renderer == null) {
                this.renderer = new LocatorBarRenderer(this.minecraft);
            }

            //~ if <26 'renderer.extractRenderState' -> 'renderer.render'
            this.renderer.extractRenderState(graphics, delta);
        }
    }

    //~ if <26 'extractRenderState' -> 'render'
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void renderCardinalDirections(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (!config.enabled || this.minecraft.options.hideGui || !config.compass.enabled) return;

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) return;

        float yaw = cameraEntity.getYRot();

        for (CardinalDirections.Direction point : CardinalDirections.DIRECTIONS) {
            if (point.isIntercardinal() && !config.compass.showIntercardinal) continue;

            ScreenBounds bounds = new ScreenBounds(this.minecraft, graphics, config);
            Font font = this.minecraft.font;

            bounds.project(point.angle() - yaw, (g, state) -> {
                //~ if <26 'centeredText' -> 'drawCenteredString'
                g.centeredText(font, point.label(), 0, -font.lineHeight / 2, state.setAlpha(point.getColor()));
            });
        }
    }
}