package dev.liqw.locatorborder;

//~ !skip_replace

import dev.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocatorBorder implements ClientModInitializer {
    public static final String MOD_ID = /*$ mod_id*/ "locator-border";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
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