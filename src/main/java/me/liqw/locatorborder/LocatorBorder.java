package me.liqw.locatorborder;

import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

public class LocatorBorder implements ClientModInitializer {
	public static final String MOD_ID = "locator-border";

	@Override
	public void onInitializeClient() {
		AutoConfig.register(LocatorBorderConfig.class, GsonConfigSerializer::new);
	}

	public static LocatorBorderConfig getConfig() {
		return AutoConfig.getConfigHolder(LocatorBorderConfig.class).getConfig();
	}
}