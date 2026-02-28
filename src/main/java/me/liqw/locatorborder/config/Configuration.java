package me.liqw.locatorborder.config;

import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.gui.screens.Screen;

@Config(name = "locator_border")
public class Configuration implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    @ConfigEntry.Gui.Tooltip
    public static int margin = 4;

    public static Screen generateScreen(Screen parent) {
        return AutoConfigClient.getConfigScreen(Configuration.class, parent).get();
    }
}
