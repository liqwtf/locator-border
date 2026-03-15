package me.liqw.locatorborder;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class LocatorBorder implements ClientModInitializer {
	public static final String MOD_ID = "locator-border";
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