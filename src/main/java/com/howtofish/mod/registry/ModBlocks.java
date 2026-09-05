package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.block.LighthouseLampBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HowToFishMod.MOD_ID);

    public static final RegistryObject<Block> LIGHTHOUSE_LAMP = BLOCKS.register("lighthouse_lamp",
            () -> new LighthouseLampBlock(BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.SNOW)
                    .strength(3.0f).sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion()));

    public static final RegistryObject<Block> LIGHTHOUSE_BRICKS = BLOCKS.register("lighthouse_bricks",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.TERRACOTTA_WHITE)
                    .strength(2.5f).sound(SoundType.STONE)));

    public static final RegistryObject<Item> LIGHTHOUSE_LAMP_ITEM = ModItemsBlocks.registerBlockItem("lighthouse_lamp", LIGHTHOUSE_LAMP);
    public static final RegistryObject<Item> LIGHTHOUSE_BRICKS_ITEM = ModItemsBlocks.registerBlockItem("lighthouse_bricks", LIGHTHOUSE_BRICKS);

    private static class ModItemsBlocks {
        static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {
            return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
                    new Item.Properties().tab(ModCreativeTab.HOW_TO_FISH_TAB)));
        }
    }
}
