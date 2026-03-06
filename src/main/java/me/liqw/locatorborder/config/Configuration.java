package me.liqw.locatorborder.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "locator_border")
public class Configuration implements ConfigData {
    public enum WaypointColor {
        Waypoint, Team,
    }

    public enum OutlineColor {
        Waypoint, Team, Black,
    }

    public enum DisplayNames {
        Never, Hover, Focal, PlayerList, Always;

        public String toString() {
            return this.name().replace("PlayerList", "Player List");
        }
    }

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    public int margin = 4;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointColor color = WaypointColor.Waypoint;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DisplayNames displayNames = DisplayNames.Never;
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public RenderPlayerFace renderPlayerFace = new RenderPlayerFace();

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
}
