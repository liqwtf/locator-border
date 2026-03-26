package me.liqw.locatorborder.fabric.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.liqw.locatorborder.config.LocatorBorderConfig;
import me.shedaniel.autoconfig.AutoConfigClient;

public class ClothConfigImplementation implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(LocatorBorderConfig.class, parent).get();
    }
}