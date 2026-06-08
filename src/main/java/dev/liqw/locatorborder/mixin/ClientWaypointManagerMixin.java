package dev.liqw.locatorborder.mixin;

import com.mojang.datafixers.util.Either;
import dev.liqw.locatorborder.LocatorBorder;
import dev.liqw.locatorborder.config.LocatorBorderConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

    @Inject(method = "forEachWaypoint", at = @At("HEAD"), cancellable = true)
    private void populateWaypointsMap(Entity fromEntity, Consumer<TrackedWaypoint> consumer, CallbackInfo ci) {
        if (!(fromEntity.level() instanceof ClientLevel level)) return;

        LocatorBorderConfig config = LocatorBorder.getConfig();

        if (config.forceWaypoints) {
            for (Player player : level.players()) {
                if (player == fromEntity || player.isInvisible()) continue;

                consumer.accept(
                        TrackedWaypoint.setPosition(player.getUUID(), Waypoint.Icon.NULL, player.blockPosition())
                );
            }
        }

        waypoints.values().forEach(waypoint -> {
            if (!waypoint.id().left()
                    .map(uuid -> config.forceWaypoints && level.getPlayerByUUID(uuid) == null)
                    .orElse(false)) {
                consumer.accept(waypoint);
            }
        });

        ci.cancel();
    }

    @Inject(method = "hasWaypoints", at = @At("HEAD"), cancellable = true)
    private void forceHasWaypoints(CallbackInfoReturnable<Boolean> cir) {
        if (LocatorBorder.getConfig().forceWaypoints) {
            cir.setReturnValue(true);
        }
    }
}