package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.BobberEntity;
import com.howtofish.mod.entity.BossFishEntity;
import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.entity.OldManEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HowToFishMod.MOD_ID);

    public static final RegistryObject<EntityType<CustomFishEntity>> CUSTOM_FISH = ENTITY_TYPES.register("custom_fish",
            () -> EntityType.Builder.of(CustomFishEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.4f).clientTrackingRange(8).build(HowToFishMod.MOD_ID + ":custom_fish"));

    public static final RegistryObject<EntityType<BossFishEntity>> BOSS_FISH = ENTITY_TYPES.register("boss_fish",
            () -> EntityType.Builder.of(BossFishEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 1.0f).clientTrackingRange(10).build(HowToFishMod.MOD_ID + ":boss_fish"));

    public static final RegistryObject<EntityType<OldManEntity>> OLD_MAN = ENTITY_TYPES.register("old_man",
            () -> EntityType.Builder.of(OldManEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build(HowToFishMod.MOD_ID + ":old_man"));

    public static final RegistryObject<EntityType<BobberEntity>> BOBBER = ENTITY_TYPES.register("bobber",
            () -> EntityType.Builder.<BobberEntity>of(BobberEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(5)
                    .build(HowToFishMod.MOD_ID + ":bobber"));
}
