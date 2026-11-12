package com.cosmic.betterlevelingaddon;

import com.cosmic.betterlevelingaddon.capability.CosmicSkillCapability;
import com.cosmic.betterlevelingaddon.client.ClientModEvents;
import com.cosmic.betterlevelingaddon.command.CosmicSkillCommand;
import com.cosmic.betterlevelingaddon.event.CommonForgeEvents;
import com.cosmic.betterlevelingaddon.network.PacketHandler;
import com.cosmic.betterlevelingaddon.skill.SkillTreeRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BetterLevelingCosmicAddon.MOD_ID)
public class BetterLevelingCosmicAddon {
    public static final String MOD_ID = "blcosmicaddon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public BetterLevelingCosmicAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Инициализируем реестр огромного космического дерева навыков
        SkillTreeRegistry.init();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CommonForgeEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CosmicSkillCapability.register();
            PacketHandler.init();
            LOGGER.info("[Better Leveling: Cosmic Skill Tree] Успешно инициализировано {} узлов дерева навыков!",
                    SkillTreeRegistry.getAllNodes().size());
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientModEvents::initClient);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CosmicSkillCommand.register(event.getDispatcher());
    }
}
