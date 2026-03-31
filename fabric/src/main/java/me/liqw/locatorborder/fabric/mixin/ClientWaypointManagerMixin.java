package me.liqw.locatorborder.fabric.mixin;

import com.mojang.datafixers.util.Either;
import me.liqw.locatorborder.LocatorBorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(ClientWaypointManager.class)
public abstract class ClientWaypointManagerMixin {
    @Shadow @Final private Map<Either<UUID, String>, TrackedWaypoint> waypoints;
    @Shadow public abstract void trackWaypoint(TrackedWaypoint waypoint);

    @Inject(method = "forEachWaypoint", at = @At("HEAD"))
    private void populateWaypointsMap(Entity fromEntity, Consumer<TrackedWaypoint> consumer, CallbackInfo ci) {
        if (!LocatorBorder.getConfig().forceWaypoints || !(fromEntity.level() instanceof ClientLevel level)) return;

        for (AbstractClientPlayer player : level.players()) {
            if (player == Minecraft.getInstance().player) continue;

            UUID uuid = player.getUUID();
            Either<UUID, String> id = Either.left(uuid);
            TrackedWaypoint existing = waypoints.get(id);

            TrackedWaypoint updatedData = TrackedWaypoint.setPosition(uuid, Waypoint.Icon.NULL, player.blockPosition());

            if (existing == null) {
                this.trackWaypoint(updatedData);
            } else {
                existing.update(updatedData);
            }
        }

        waypoints.values().removeIf(waypoint ->
                waypoint.id().left().filter(uuid -> level.getPlayerByUUID(uuid) == null).isPresent()
        );
    }

    @Inject(method = "hasWaypoints", at = @At("HEAD"), cancellable = true)
    private void forceHasWaypoints(CallbackInfoReturnable<Boolean> cir) {
        if (LocatorBorder.getConfig().forceWaypoints) {
            cir.setReturnValue(true);
        }
    }
}
