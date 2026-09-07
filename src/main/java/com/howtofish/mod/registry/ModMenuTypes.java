package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.menu.OldManShopMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, HowToFishMod.MOD_ID);

    public static final RegistryObject<MenuType<OldManShopMenu>> OLD_MAN_SHOP = MENUS.register("old_man_shop",
            () -> IForgeMenuType.create((windowId, inv, data) -> new OldManShopMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.howtofish.mod.menu.RodBaitMenu>> ROD_BAIT = MENUS.register("rod_bait",
            () -> IForgeMenuType.create((windowId, inv, data) -> new com.howtofish.mod.menu.RodBaitMenu(windowId, inv)));
}
