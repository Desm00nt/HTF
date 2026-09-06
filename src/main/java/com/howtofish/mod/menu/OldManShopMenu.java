package com.howtofish.mod.menu;

import com.howtofish.mod.registry.ModItems;
import com.howtofish.mod.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Old Man's shop. Shown as a simple list of buyable goods with a Ruble
 * price each; the actual buy logic lives in {@link com.howtofish.mod.client.OldManShopScreen}
 * (client -&gt; server "buy" network packet) to keep this menu lightweight and
 * compatible across versions without a big slot/inventory rewrite.
 */
public class OldManShopMenu extends AbstractContainerMenu {

    public record ShopOffer(ItemStack display, int price, String id) {}

    public static final List<ShopOffer> OFFERS = List.of(
            new ShopOffer(new ItemStack(ModItems.FISHING_ROD.get()), 3, "fishing_rod"),
            new ShopOffer(new ItemStack(ModItems.KNIFE.get()), 4, "knife"),
            new ShopOffer(new ItemStack(ModItems.BAIT.get()), 15, "bait"),
            new ShopOffer(new ItemStack(ModItems.RADAR.get()), 10, "radar"),
            new ShopOffer(new ItemStack(ModItems.BEER.get()), 2, "beer")
    );

    private final Inventory playerInventory;

    public OldManShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.OLD_MAN_SHOP.get(), containerId);
        this.playerInventory = playerInventory;
    }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    public Inventory getPlayerInventory() {
        return playerInventory;
    }
}
