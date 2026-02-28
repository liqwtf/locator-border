package me.liqw.locatorborder.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "locator_border")
public class Configuration implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    @ConfigEntry.Gui.Tooltip
    public int margin = 4;
}
