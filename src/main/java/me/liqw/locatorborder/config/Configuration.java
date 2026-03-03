package me.liqw.locatorborder.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "locator_border")
public class Configuration implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    public int screenMargin = 4;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointColor waypointColor = WaypointColor.Waypoint;
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public RenderPlayerFace renderPlayerFace = new RenderPlayerFace();
    @ConfigEntry.Category("directions")
    public boolean cardinalDirections = false;
    @ConfigEntry.Category("directions")
    public boolean intercardinal = false;

    public enum WaypointColor {
        Waypoint, Team,
    }

    public enum OutlineColor {
        Waypoint, Team, Black,
    }

    public static class RenderPlayerFace {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
        @ConfigEntry.Gui.Tooltip
        public boolean distanceScale = true;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public OutlineColor outlineColor = OutlineColor.Black;
    }
}
