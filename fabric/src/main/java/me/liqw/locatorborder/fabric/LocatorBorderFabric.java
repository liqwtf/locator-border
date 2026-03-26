package me.liqw.locatorborder.fabric;

import me.liqw.locatorborder.LocatorBorder;
import net.fabricmc.api.ClientModInitializer;

public final class LocatorBorderFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LocatorBorder.initialize();
    }
}
