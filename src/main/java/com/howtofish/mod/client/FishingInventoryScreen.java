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
        // One language with the radar bar & the HUD hotbar (see GuiStyle).
        GuiStyle.panel(poseStack, this.leftPos, this.topPos, imageWidth, imageHeight, true);
        int hotX = this.leftPos + 8;
        int sel = this.minecraft.player != null ? this.minecraft.player.getInventory().selected : -1;
        // Every hotbar cell is a real cell: 0-2 active (gold), 3-8 reserve.
        for (int i = 0; i < 9; i++) {
            int cx = hotX + i * 18;
            int cy = this.topPos + 30;
            boolean hovered = isHovering(cx, cy, 18, 18, mouseX, mouseY);
            GuiStyle.slotCell(poseStack, cx, cy, i < 3, hovered || i == sel);
            // number tags so the player learns which key does what
            this.font.drawShadow(poseStack, String.valueOf(i + 1), cx + 1, this.topPos + 21,
                    i < 3 ? GuiStyle.GOLD : GuiStyle.TEXT_DIM);
        }
        // Storage grid 9x3.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int cx = this.leftPos + 8 + col * 18;
                int cy = this.topPos + 62 + row * 18;
                GuiStyle.slotCell(poseStack, cx, cy, false, isHovering(cx, cy, 18, 18, mouseX, mouseY));
            }
        }
        GuiStyle.separator(poseStack, this.leftPos + 8, this.topPos + 55, imageWidth - 16);
    }

    private static boolean isHovering(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        GuiStyle.title(poseStack, this.font, this.title,
                this.leftPos + imageWidth / 2, this.topPos + 6);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}
