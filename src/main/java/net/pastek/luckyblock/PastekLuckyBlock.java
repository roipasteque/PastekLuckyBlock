package net.pastek.luckyblock;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.pastek.luckyblock.prefab.configuration.LBConfigurationHandler;
import net.pastek.luckyblock.registers.UnifiedLuckyBlockRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(PastekLuckyBlock.MOD_ID)
public class PastekLuckyBlock {
    public static final String MOD_ID = "pastekluckyblock";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public PastekLuckyBlock(IEventBus EventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, LBConfigurationHandler.COMMON_SPEC);
        EventBus.addListener(this::commonSetup);
        UnifiedLuckyBlockRegister.register(EventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }


    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(PastekLuckyBlock.MOD_ID, path);
    }
}