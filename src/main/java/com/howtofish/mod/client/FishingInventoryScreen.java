package com.howtofish.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/**
 * A clean custom player inventory for the "How To Fish" experience: the hotbar
 * row on top (all NINE cells real and visible - the first three are the active
 * HUD cells and are highlighted, 3-8 are reserve that can be clicked/swapped
 * freely, they are never shuffled or hidden) plus the full 27-slot storage
 * grid below. Crafting grid and armour are deactivated. The panel is drawn
 * from scratch - no vanilla inventory texture, nothing broken or masked.
 */
public class FishingInventoryScreen extends AbstractContainerScreen<InventoryMenu> {

    private static final int PANEL_W = 194;
    private static final int PANEL_H = 118;

    public FishingInventoryScreen(Player player) {
        super(player.inventoryMenu, player.getInventory(),
                Component.translatable("container.howtofish.inventory"));
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        trimSlots();
    }

    /** Reposition & deactivate the vanilla inventory slots for our layout. */
    private void trimSlots() {
        InventoryMenu menu = this.menu;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) {
                // Crafting / result slots: deactivate in place.
                menu.slots.set(i, inert(slot));
                continue;
            }
            int index = slot.getSlotIndex();
            if (index >= 0 && index < 9) {
                // Hotbar row: ALL nine slots live, one row, nothing shuffles.
                menu.slots.set(i, new Slot(slot.container, index, 8 + index * 18, 30));
            } else if (index >= 9 && index < 36) {
                // Storage grid 9x3, repositioned.
                int idx = index - 9;
                menu.slots.set(i, new Slot(slot.container, index, 8 + (idx % 9) * 18, 62 + (idx / 9) * 18));
            } else {
                // Armour / offhand: hidden & inert.
                menu.slots.set(i, inert(slot));
            }
        }
    }

    private static Slot inert(Slot slot) {
        return new Slot(slot.container, slot.getSlotIndex(), slot.x, slot.y) {
            @Override
            public boolean isActive() {
                return false;
            }
        };
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.titleLabelX = -4000;
        this.titleLabelY = -4000;
        this.inventoryLabelX = -4000;
        this.inventoryLabelY = -4000;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel: dark sea-green wood style.
        fill(poseStack, x, y, x + imageWidth, y + imageHeight, 0xF01B2A30);
        fill(poseStack, x, y, x + imageWidth, y + 1, 0xFF3E5A66);
        fill(poseStack, x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF0C1418);
        fill(poseStack, x, y, x + 1, y + imageHeight, 0xFF2A3E47);
        fill(poseStack, x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF2A3E47);

        // The three ACTIVE (HUD-visible) hotbar cells get a golden frame; the
        // rest of the row is plain reserve.
        for (int i = 0; i < 3; i++) {
            int sx = x + 8 + i * 18;
            fill(poseStack, sx - 1, y + 29, sx + 17, y + 47, 0xFFC89B3C);
        }
    }

    /**
     * Vanilla lets the mouse wheel switch the hotbar selection even inside the
     * inventory screen - that would select hidden-behind-HUD cells 3-8. The
     * wheel in menus is useless for us, so swallow it (in-game wheel still
     * cycles cells 0-2, see ClientEvents).
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.leftPos + imageWidth / 2, this.topPos + 6, 0xFFE9D9A8);
        drawString(poseStack, this.font, Component.translatable("container.howtofish.equipment"),
                this.leftPos + 8, this.topPos + 20, 0xFFBFE8FF);
        drawString(poseStack, this.font, Component.translatable("container.howtofish.storage"),
                this.leftPos + 8, this.topPos + 52, 0xFFBFE8FF);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}
