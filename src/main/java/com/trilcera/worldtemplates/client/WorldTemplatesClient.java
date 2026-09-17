package com.trilcera.worldtemplates.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber(modid = "worldtemplates", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WorldTemplatesClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new WorldTemplateManager());
        LOGGER.info("Registered WorldTemplateManager reload listener");
    }
}
