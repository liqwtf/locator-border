package me.liqw.locatorborder.config;

import me.liqw.locatorborder.LocatorBorder;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.*;

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

    @ConfigEntry.Gui.Excluded
    public transient Map<String, PlayerSpecificConfig.Override> overrideCache = new HashMap<>();
    @ConfigEntry.Category("overrides")
    public List<PlayerSpecificConfig> overrides = new ArrayList<>(List.of(
            new PlayerSpecificConfig("liqw", new PlayerSpecificConfig.Override(0x6395EE))
    ));

    @ConfigEntry.Category("extras")
    @ConfigEntry.Gui.CollapsibleObject
    public CardinalDirections compass = new CardinalDirections();

    @Override
    public void validatePostLoad() {
        overrideCache.clear();
        if (overrides == null) return;

        overrides.removeIf(entry -> entry.name == null || entry.name.isBlank());

        for (PlayerSpecificConfig entry : overrides) {
            overrideCache.put(entry.name.toLowerCase(), entry.override);
        }
    }

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

    public static class PlayerSpecificConfig {
        public String name;
        @ConfigEntry.Gui.TransitiveObject
        public Override override = new Override();

        public static class Override {
            @ConfigEntry.ColorPicker
            public int color = 0xFFFFFF;
            @ConfigEntry.BoundedDiscrete(min = 50, max = 400)
            public int iconScale = 100;

            public Override() {}
            public Override(int color) {
                this.color = color;
            }
        }

        public PlayerSpecificConfig() {}
        public PlayerSpecificConfig(String name, Override override) {
            this.name = name;
            this.override = override;
        }
    }
}