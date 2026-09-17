package com.trilcera.worldtemplates;

import com.trilcera.worldtemplates.client.gui.TrilceraConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(WorldTemplates.MODID)
public class WorldTemplates {
    public static final String MODID = "worldtemplates";
    private static final Logger LOGGER = LogUtils.getLogger();

    public WorldTemplates() {
        Config.register();
        LOGGER.info("World Templates Reforged initialized!");
        LOGGER.info("Button text: {}", Config.INSTANCE.buttonText.get());

        // Register in-game config screen (Mod List -> select mod -> Config button)
        // FMLClientSetupEvent is not needed; ConfigScreenHandler works directly
        try {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (minecraft, parent) -> new TrilceraConfigScreen(parent)
                )
            );
            LOGGER.info("Registered Trilcera config screen");
        } catch (Exception e) {
            LOGGER.warn("Could not register config screen: {}", e.getMessage());
        }
    }
}
