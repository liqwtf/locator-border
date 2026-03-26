package me.liqw.locatorborder;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocatorBorder {
    public static final String MOD_ID = "locator_border";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize() {
        ConfigHolder<LocatorBorderConfig> holder = AutoConfig.register(LocatorBorderConfig.class, GsonConfigSerializer::new);

        // temp fix, validatePostLoad isn't called when saving
        holder.registerSaveListener(((configHolder, config) -> {
            config.validatePostLoad();
            return InteractionResult.SUCCESS;
        }));
    }

    public static LocatorBorderConfig getConfig() {
        return AutoConfig.getConfigHolder(LocatorBorderConfig.class).getConfig();
    }
}
