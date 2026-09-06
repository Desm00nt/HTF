package com.howtofish.mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/**
 * A trimmed-down player inventory for the "How To Fish" experience: only the
 * three hotbar slots on the left plus the dedicated BAIT slot (the offhand
 * slot) are visible. All other vanilla slots (main inventory, crafting grid,
 * armour) are deactivated (isActive=false, so they can't be clicked or
 * hovered) and their background squares are masked out with flat panel
 * colour, leaving a clean minimal screen.
 */
public class FishingInventoryScreen extends InventoryScreen {

    public FishingInventoryScreen(Player player) {
        super(player);
        hideForbiddenSlots();
    }

    /** Deactivate every slot except hotbar 0-2 and the offhand (bait) slot. */
    private void hideForbiddenSlots() {
        InventoryMenu menu = this.menu;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (isAllowed(slot)) continue;
            // Swap in an inert copy of the same slot (same container & index)
            // that is never active - unrendered, unclickable, unhoverable.
            menu.slots.set(i, new Slot(slot.container, slot.getSlotIndex(), slot.x, slot.y) {
                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }
    }

    private static boolean isAllowed(Slot slot) {
        if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory inv)) {
            return false; // crafting containers etc.
        }
        int index = slot.getSlotIndex();
        // Hotbar 0-2 or the offhand slot (index 40).
        return (index >= 0 && index <= 2) || index == net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int x, int y) {
        super.renderBg(poseStack, partialTick, x, y);
        // Mask the vanilla background squares that belong to hidden slots by
        // painting flat panel colour over them. Vanilla inventory.png panel
        // interior is RGB(198,198,198), so this is seamless.
        int lx = this.leftPos;
        int ly = this.topPos;

        // Armour column (left edge).
        mask(poseStack, lx + 6, ly + 6, lx + 26, ly + 80);
        // Crafting grid.
        mask(poseStack, lx + 96, ly + 16, lx + 146, ly + 61);
        // Crafting result.
        mask(poseStack, lx + 150, ly + 16, lx + 174, ly + 50);
        // Main inventory grid.
        mask(poseStack, lx + 6, ly + 82, lx + 163, ly + 141);
        // Hotbar slots 3-8 (keep 0-2 and the bait slot visible).
        mask(poseStack, lx + 61, ly + 140, lx + 170, ly + 160);
    }

    private void mask(PoseStack poseStack, int x0, int y0, int x1, int y1) {
        fill(poseStack, x0, y0, x1, y1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int x, int y) {
        // Only our own label above the bait slot.
        RenderSystem.disableDepthTest();
        Component bait = Component.translatable("container.howtofish.bait_slot");
        this.font.draw(poseStack, bait, this.imageWidth - 92 - this.font.width(bait), 50.0f, 0x404040);
        RenderSystem.enableDepthTest();
    }
}
