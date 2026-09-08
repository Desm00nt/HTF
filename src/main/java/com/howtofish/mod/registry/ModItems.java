package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.FishType;
import com.howtofish.mod.item.BeerItem;
import com.howtofish.mod.item.FishMeatItem;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.howtofish.mod.item.KnifeItem;
import com.howtofish.mod.item.RadarItem;
import com.howtofish.mod.item.RubleCoinItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HowToFishMod.MOD_ID);

    private static Item.Properties base() {
        return new Item.Properties().tab(ModCreativeTab.HOW_TO_FISH_TAB);
    }

    public static final RegistryObject<Item> FISHING_ROD = ITEMS.register("fishing_rod",
            () -> new FishingRodCustomItem(base().stacksTo(1).durability(128)));

    public static final RegistryObject<Item> KNIFE = ITEMS.register("knife",
            () -> new KnifeItem(base().stacksTo(1)));

    public static final RegistryObject<Item> RADAR = ITEMS.register("radar",
            () -> new RadarItem(base().stacksTo(1)));

    public static final RegistryObject<Item> BAIT = ITEMS.register("bait",
            () -> new com.howtofish.mod.item.BaitItem(base().stacksTo(1)));

    /** Beer - the Spider Crab's lure (kept stackable: bait for repeated expeditions). */
    public static final RegistryObject<Item> BEER = ITEMS.register("beer",
            () -> new BeerItem(base().stacksTo(8)));

    public static final RegistryObject<Item> RUBLE_COIN = ITEMS.register("ruble_coin",
            () -> new RubleCoinItem(base().stacksTo(64)));

    // Raw fish meat items (one per FishType) obtained by killing a released fish.
    public static final RegistryObject<Item> FISH_MEAT_ANCHOVY = registerFishMeat(FishType.ANCHOVY);
    public static final RegistryObject<Item> FISH_MEAT_HERRING = registerFishMeat(FishType.HERRING);
    public static final RegistryObject<Item> FISH_MEAT_CRAB = registerFishMeat(FishType.CRAB);
    public static final RegistryObject<Item> FISH_MEAT_SHRIMP = registerFishMeat(FishType.SHRIMP);
    public static final RegistryObject<Item> FISH_MEAT_LOBSTER = registerFishMeat(FishType.LOBSTER);
    public static final RegistryObject<Item> FISH_MEAT_PUFFERFISH = registerFishMeat(FishType.PUFFERFISH);
    public static final RegistryObject<Item> SPIDER_CRAB_SHELL = ITEMS.register("spider_crab_shell",
            () -> new Item(base()));

    private static RegistryObject<Item> registerFishMeat(FishType type) {
        return ITEMS.register(type.getMeatId(), () -> new FishMeatItem(base(), type));
    }
}
