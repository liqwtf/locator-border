package me.liqw.locatorborder.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
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
    private boolean forceHasWaypointsFalse(boolean original) {
        return false;
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    private void renderLocatorBorder(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Entity camera = this.minecraft.getCameraEntity();
        if (camera == null || this.minecraft.player == null) return;

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        int margin = 12;
        float centerX = screenW / 2.0f;
        float centerY = screenH / 2.0f;

        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(camera, waypoint -> {

            if (waypoint.id().left().map(uuid -> uuid.equals(camera.getUUID())).orElse(false)) return;

            // 1. Get the angle (0 is front, negative is left, positive is right)
            double yaw = waypoint.yawAngleToCamera(camera.level(), this.minecraft.gameRenderer.getMainCamera(),
                    e -> deltaTracker.getGameTimeDeltaPartialTick(true));

            // 2. Convert to Radians. We subtract 90deg to make 0 point "Up"
            double angleRad = Math.toRadians(yaw) - (Math.PI / 2.0);
            float cos = (float) Math.cos(angleRad);
            float sin = (float) Math.sin(angleRad);

            // 3. Box Projection: Map the circular angle to the rectangular screen edges
            // We find the intersection of the ray (cos, sin) and the screen boundary
            float edgeX = (screenW / 2.0f) - margin;
            float edgeY = (screenH / 2.0f) - margin;

            // Factor determines how much we scale the unit vector to hit the edge
            float factor = Math.min(Math.abs(edgeX / cos), Math.abs(edgeY / sin));

            float renderX = centerX + (cos * factor);
            float renderY = centerY + (sin * factor);

            // 4. Handle Visuals (Icons and Colors)
            WaypointStyle style = this.minecraft.getWaypointStyles().get(waypoint.icon().style);
            float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
            Identifier sprite = style.sprite(distance);

            int color = waypoint.id().map(
                    uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
                    name -> ARGB.setBrightness(ARGB.color(255, name.hashCode()), 0.9F)
            );

            // 5. Draw the Sprite (Centered at 9x9 size)
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, (int)renderX - 4, (int)renderY - 4, 9, 9, color);
        });
    }
}