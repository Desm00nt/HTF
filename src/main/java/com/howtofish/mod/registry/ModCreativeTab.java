package com.howtofish.mod.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 1.19.2 still uses the "classic" CreativeModeTab API (the
 * DeferredRegister/builder based system only arrived in 1.19.3+), so the tab
 * is a plain static instance and items opt into it via
 * {@code Item.Properties#tab(CreativeModeTab)}.
 */
public class ModCreativeTab {
    public static final CreativeModeTab HOW_TO_FISH_TAB = new CreativeModeTab("howtofish") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.FISHING_ROD.get());
        }
    };
}
