package com.howtofish.mod.world;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.BossFishEntity;
import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.entity.OldManEntity;
import com.howtofish.mod.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects worlds created with the "Fishing" world type (identified by the
 * custom {@code howtofish:fishing_dimension_type} assigned to their Overworld)
 * and builds the lighthouse island the first time that level loads.
 */
public class ModWorldGen {

    public static final ResourceLocation FISHING_DIMENSION_TYPE = new ResourceLocation(HowToFishMod.MOD_ID, "fishing_dimension_type");
    private static final Set<String> INITIALISED_LEVELS = new HashSet<>();

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ModWorldGen());
    }

    public static boolean isFishingWorld(ServerLevel level) {
        return level.dimensionTypeRegistration().unwrapKey()
                .map(key -> key.location().equals(FISHING_DIMENSION_TYPE))
                .orElse(false);
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!isFishingWorld(level)) return;

        String key = level.dimension().location().toString() + "@" + System.identityHashCode(level.getServer());
        if (INITIALISED_LEVELS.contains(key)) return;

        // Only build once: check the lighthouse lamp block at its actual position.
        if (level.getBlockState(IslandBuilder.getLighthouseLampPos()).is(com.howtofish.mod.registry.ModBlocks.LIGHTHOUSE_LAMP.get())) {
            INITIALISED_LEVELS.add(key);
            return;
        }

        level.getServer().execute(() -> {
            IslandBuilder.buildSpawnIsland(level);
            IslandBuilder.buildSecondIsland(level);
            level.setDefaultSpawnPos(IslandBuilder.SPAWN_ISLAND_ORIGIN.offset(0, 2, 3), 0.0f);
            INITIALISED_LEVELS.add(key);
            HowToFishMod.LOGGER.info("How To Fish: generated the lighthouse island at {}", IslandBuilder.SPAWN_ISLAND_ORIGIN);
        });
    }

    @Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Attributes {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(ModEntities.CUSTOM_FISH.get(), CustomFishEntity.createAttributes().build());
            event.put(ModEntities.BOSS_FISH.get(), BossFishEntity.createAttributes().build());
            event.put(ModEntities.OLD_MAN.get(), OldManEntity.createAttributes().build());
        }
    }
}
