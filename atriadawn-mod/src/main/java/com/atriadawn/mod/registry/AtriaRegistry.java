package com.atriadawn.mod.registry;

import com.atriadawn.mod.AtriaDawnMod;
import com.atriadawn.mod.entity.AtriaCompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Регистрация сущности компаньона. Спавн — только по команде
 * {@code /atria summon}, никаких естественных появлений.
 */
public final class AtriaRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AtriaDawnMod.MOD_ID);

    public static final RegistryObject<EntityType<AtriaCompanionEntity>> ATRIA_COMPANION =
            ENTITY_TYPES.register("atria_companion", () -> EntityType.Builder
                    .of(AtriaCompanionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(12)
                    .build("atria_companion"));

    private AtriaRegistry() {
    }

    @Mod.EventBusSubscriber(modid = AtriaDawnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(ATRIA_COMPANION.get(), AtriaCompanionEntity.createAttributes().build());
        }
    }
}
