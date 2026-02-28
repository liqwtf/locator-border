package me.liqw.locatorborder.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
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
    private LocatorBarRenderer customLocatorRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initCustomRenderer(Minecraft minecraft, CallbackInfo ci) {
        this.customLocatorRenderer = new LocatorBarRenderer(minecraft);
    }

    @ModifyVariable(method = "nextContextualInfoState", at = @At("STORE"), ordinal = 0)
    private boolean forceLocatorStateOff(boolean original) {
        return false;
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    private void manuallyRenderLocator(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft.player != null &&
                this.minecraft.player.connection.getWaypointManager().hasWaypoints()) {

            if (this.customLocatorRenderer != null) {
                this.customLocatorRenderer.render(guiGraphics, deltaTracker);
            }
        }
    }
}