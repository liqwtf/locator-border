package me.liqw.locatorborder.config;

import me.liqw.locatorborder.LocatorBorder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = LocatorBorder.MOD_ID)
public class Configuration implements ConfigData {
    public enum DisplayNames {
        Hover, Focal, PlayerList, Always, Never;

        public String toString() {
            return this.name().replace("PlayerList", "Player List");
        }
    }

    public enum OutlineColor {
        Waypoint, Team, Black,
    }

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    public int margin = 4;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DisplayNames displayNames = DisplayNames.Hover;
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public RenderPlayerFace renderPlayerFace = new RenderPlayerFace();

    @ConfigEntry.Category("overrides")
    public List<Overrides> overrides = new ArrayList<>();

    @ConfigEntry.Category("directions")
    public boolean cardinalDirections = false;
    @ConfigEntry.Category("directions")
    public boolean intercardinal = false;

    public static class RenderPlayerFace {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public OutlineColor color = OutlineColor.Black;
    }

    public static class Overrides {
        public String name = "";
        @ConfigEntry.ColorPicker
        public int color = 0xFFFFFF;
    }

}
