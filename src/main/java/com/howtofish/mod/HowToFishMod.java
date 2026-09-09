package com.howtofish.mod;

import com.howtofish.mod.economy.CurrencyEvents;
import com.howtofish.mod.event.FishingEvents;
import com.howtofish.mod.event.ModEvents;
import com.howtofish.mod.registry.ModBlockEntities;
import com.howtofish.mod.registry.ModBlocks;
import com.howtofish.mod.registry.ModEntities;
import com.howtofish.mod.registry.ModItems;
import com.howtofish.mod.registry.ModMenuTypes;
import com.howtofish.mod.registry.ModSounds;
import com.howtofish.mod.world.ModWorldGen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * How To Fish
 * ------------
 * Adds a "Fishing" world preset (selectable in the "World Type" section of the
 * Create World screen -&gt; "More World Options") inspired by the Steam game
 * "How to Fish": an endless ocean world that spawns the player on a small
 * lighthouse island with an old keeper NPC, a boat and a dock.
 *
 * Core loop: cast the custom fishing rod -&gt; a live fish is released next to
 * you -&gt; finish it off with the knife -&gt; feed the meat to the old keeper for
 * Rubles -&gt; buy gear from him (rod, knife, radar, beer) -&gt; feed him a boss
 * trophy for the coordinates of the next island.
 */
@Mod(HowToFishMod.MOD_ID)
public class HowToFishMod {

    public static final String MOD_ID = "howtofish";
    public static final String MOD_VERSION = "1.0.5";   // logged on startup + jar name
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public HowToFishMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ModEvents());
        MinecraftForge.EVENT_BUS.register(new FishingEvents());
        MinecraftForge.EVENT_BUS.register(new CurrencyEvents());

        ModWorldGen.register();

        LOGGER.info("How To Fish v{} loaded - cast your line!", MOD_VERSION);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(com.howtofish.mod.network.ModNetwork::register);
    }
}
