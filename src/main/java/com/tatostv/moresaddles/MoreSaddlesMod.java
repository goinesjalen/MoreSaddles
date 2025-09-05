package com.tatostv.moresaddles;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(MoreSaddlesMod.MODID)
public class MoreSaddlesMod {
    public static final String MODID = "moresaddles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoreSaddlesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("More Saddles mod loaded successfully!");
    }
}