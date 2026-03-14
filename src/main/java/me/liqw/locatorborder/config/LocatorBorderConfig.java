package me.liqw.locatorborder.config;

import me.liqw.locatorborder.LocatorBorder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.List;

@Config(name = LocatorBorder.MOD_ID)
public class LocatorBorderConfig implements ConfigData {
    public enum DisplayNames {
        Hover, Focal, PlayerList, Always, Never;

        public String toString() {
            return this.name().replace("PlayerList", "Player List");
        }
    }

    public enum WaypointColor {
        Waypoint, Team,
    }

    public enum OutlineColor {
        Waypoint, Team, Black,
    }

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 16)
    public int margin = 4;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointColor color = WaypointColor.Waypoint;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DisplayNames displayNames = DisplayNames.Hover;
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public RenderPlayerFace renderPlayerFace = new RenderPlayerFace();

    @ConfigEntry.Category("overrides")
    public List<Override> overrides = List.of(new Override("liqw", 0x6395EE));

    @ConfigEntry.Category("extras")
    @ConfigEntry.Gui.CollapsibleObject
    public CardinalDirections compass = new CardinalDirections();

    public static class RenderPlayerFace {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
        @ConfigEntry.Gui.Tooltip
        public boolean distanceScale = true;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public OutlineColor color = OutlineColor.Black;
    }

    public static class CardinalDirections {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
        @ConfigEntry.Gui.Tooltip
        public boolean intercardinal = false;
    }

    public static class Override {
        public Override() {}

        public Override(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public String name;
        @ConfigEntry.ColorPicker
        public int color = 0xFFFFFF;
    }
}