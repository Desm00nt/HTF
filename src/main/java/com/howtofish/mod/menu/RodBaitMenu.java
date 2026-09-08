package com.howtofish.mod.menu;

import com.howtofish.mod.item.BaitKind;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.howtofish.mod.registry.ModMenuTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The rod's bait menu - opened by pressing "B" while holding the fishing rod.
 * Shows a single BAIT slot (stored directly in the rod's NBT) plus the
 * player's inventory. Accepted baits are defined by {@link BaitKind}: the
 * EMPTY BEER CAN (one cast - the Spider Crab's lure: no nibbles, one deep
 * plunge, hooking summons the boss) and the GOLDEN BAIT (15 casts, makes
 * expensive fish bite faster). Add new baits in BaitKind, not here.
 */
public class RodBaitMenu extends AbstractContainerMenu {

    /** The rod whose bait is being edited (server side; EMPTY on the client). */
    private final ItemStack rodStack;

    public RodBaitMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY);
    }

    public RodBaitMenu(int containerId, Inventory playerInventory, ItemStack rod) {
        super(ModMenuTypes.ROD_BAIT.get(), containerId);
        this.rodStack = rod == null ? ItemStack.EMPTY : rod;
        this.addSlot(new BaitSlot(new BaitContainer(this.rodStack), 0, 88, 16));
        // Main inventory (27 slots).
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 64 + row * 18));
            }
        }
        // Hotbar (9 slots).
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 118));
        }
    }

    public ItemStack getRod() {
        return rodStack;
    }

    public ItemStack getRodBait() {
        return FishingRodCustomItem.getBait(rodStack);
    }

    /** Container that keeps the bait inside the rod's NBT (server side). */
    private static class BaitContainer extends SimpleContainer {
        private final ItemStack rod;

        BaitContainer(ItemStack rod) {
            super(1);
            this.rod = rod;
            ItemStack bait = FishingRodCustomItem.getBait(rod);
            if (!bait.isEmpty()) {
                super.setItem(0, bait);
            }
        }

        @Override
        public void setChanged() {
            FishingRodCustomItem.setBait(rod, this.getItem(0));
        }
    }

    /** Only items known to {@link BaitKind} may go into the rod's bait slot. */
    private static class BaitSlot extends Slot {
        BaitSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BaitKind.of(stack) != BaitKind.NONE;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot from = this.slots.get(index);
        if (!from.hasItem()) return ItemStack.EMPTY;

        if (index == 0) {
            // Bait -> any inventory slot.
            ItemStack stack = from.getItem();
            ItemStack original = stack.copy();
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                from.set(ItemStack.EMPTY);
            } else {
                from.setChanged();
            }
            return original;
        }

        // Inventory -> bait slot (only baits, only if the bait slot is free).
        ItemStack stack = from.getItem();
        if (BaitKind.of(stack) == BaitKind.NONE) {
            return ItemStack.EMPTY;
        }
        Slot baitSlot = this.slots.get(0);
        if (baitSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack single = stack.copy();
        single.setCount(1);
        baitSlot.set(single);
        stack.shrink(1);
        if (stack.isEmpty()) {
            from.set(ItemStack.EMPTY);
        } else {
            from.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
