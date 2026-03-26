package me.liqw.locatorborder.neoforge.config;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClothConfigImplementation {
    public static void load() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () ->
                (client, parent) -> AutoConfigClient.getConfigScreen(LocatorBorderConfig.class, parent).get()
        );
    }
}
