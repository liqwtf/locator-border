package dev.liqw.locatorborder;

//~ !skip_replace

import dev.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;

//? fabric
//import net.fabricmc.api.ClientModInitializer;

//? neoforge {
//~ if <=1.21.10 'AutoConfigClient' -> 'AutoConfig'
import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//? }

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? neoforge
@Mod("locator_border")
public class LocatorBorder /*? fabric { */ /*implements ClientModInitializer *//*? } */ {
    public static final String MOD_ID = "locator-border";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private void initialize() {
        ConfigHolder<LocatorBorderConfig> holder = AutoConfig.register(LocatorBorderConfig.class, GsonConfigSerializer::new);

        // temp fix, validatePostLoad isn't called when saving
        holder.registerSaveListener(((configHolder, config) -> {
            config.validatePostLoad();
            return InteractionResult.SUCCESS;
        }));
    }

    //? fabric {
    /*@Override
    public void onInitializeClient() {
        initialize();
    }
    *///? }

    //? neoforge {
    public LocatorBorder() {
        initialize();

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () ->
                //~ if <=1.21.10 'AutoConfigClient' -> 'AutoConfig'
                (client, parent) -> AutoConfigClient.getConfigScreen(LocatorBorderConfig.class, parent).get()
        );
    }
    //? }

    public static LocatorBorderConfig getConfig() {
        return AutoConfig.getConfigHolder(LocatorBorderConfig.class).getConfig();
    }
}