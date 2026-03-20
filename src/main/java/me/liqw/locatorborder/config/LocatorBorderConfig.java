package me.liqw.locatorborder.config;

import me.liqw.locatorborder.LocatorBorder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.*;
import java.util.stream.Collectors;

@Config(name = LocatorBorder.MOD_ID)
public class LocatorBorderConfig implements ConfigData {
    @ConfigEntry.Gui.Excluded
    public transient Map<String, PlayerSpecificConfig.Override> overrideCache = new HashMap<>();

    public enum WaypointColor {
        Waypoint, Team,
    }

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public int margin = 4;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointColor color = WaypointColor.Waypoint;

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public RenderPlayerFace renderPlayerFace = new RenderPlayerFace();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public FocusWaypoint focusWaypoint = new FocusWaypoint();

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Category("overrides")
    public List<PlayerSpecificConfig> overrides = new ArrayList<>();

    @ConfigEntry.Category("miscellaneous")
    @ConfigEntry.Gui.CollapsibleObject
    public CardinalDirections compass = new CardinalDirections();

    @ConfigEntry.Category("miscellaneous")
    public boolean animations = true;

    @Override
    public void validatePostLoad() {
        if (overrides == null) overrides = new ArrayList<>();

        overrides.removeIf(entry -> entry.name == null || entry.name.isBlank());
        overrideCache = overrides.stream()
                .collect(Collectors.toMap(e -> e.name.toLowerCase(), e -> e.override));
    }

    public static class RenderPlayerFace {
        public enum OutlineColor {
            Waypoint, Team, Black,
        }

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;

        @ConfigEntry.Gui.Tooltip
        public boolean distanceScale = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public OutlineColor color = OutlineColor.Black;
    }

    public static class FocusWaypoint {
        public enum Trigger {
            Hover, Focal, PlayerList, None;

            public String toString() {
                return this.name().replaceAll("([a-z])([A-Z])", "$1 $2");
            }
        }

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public Trigger trigger = Trigger.Hover;

        @ConfigEntry.Gui.Tooltip
        public float scale = 1.2f;

        @ConfigEntry.Gui.Tooltip
        public int inset = 2;

        @ConfigEntry.Gui.CollapsibleObject
        public FocusLabels labels = new FocusLabels();

        public static class FocusLabels {
            @ConfigEntry.Gui.Tooltip
            public boolean showName = true;

            @ConfigEntry.Gui.Tooltip
            public boolean showDistance = false;
        }
    }

    public static class PlayerSpecificConfig {
        public String name;

        @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
        public Override override = new Override();

        public static class Override {
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.ColorPicker
            public int color = 0xFFFFFF;

            @ConfigEntry.Gui.Tooltip
            public boolean alwaysFocused = false;
        }
    }

    public static class CardinalDirections {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;

        @ConfigEntry.Gui.Tooltip
        public boolean showIntercardinal = false;
    }
}