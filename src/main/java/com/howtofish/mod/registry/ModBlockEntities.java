package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.block.LighthouseLampBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HowToFishMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<LighthouseLampBlockEntity>> LIGHTHOUSE_LAMP =
            BLOCK_ENTITIES.register("lighthouse_lamp", () -> BlockEntityType.Builder.of(
                    LighthouseLampBlockEntity::new, ModBlocks.LIGHTHOUSE_LAMP.get()).build(null));
}
