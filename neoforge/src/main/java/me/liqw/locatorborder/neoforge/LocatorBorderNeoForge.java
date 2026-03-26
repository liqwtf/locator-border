package me.liqw.locatorborder.neoforge;

import me.liqw.locatorborder.LocatorBorder;
import me.liqw.locatorborder.neoforge.config.ClothConfigImplementation;
import net.neoforged.fml.common.Mod;

@Mod(LocatorBorder.MOD_ID)
public final class LocatorBorderNeoForge {
    public LocatorBorderNeoForge() {
        LocatorBorder.initialize();
        ClothConfigImplementation.load();
    }
}
