package com.howtofish.mod.client;

import com.howtofish.mod.menu.RodBaitMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The rod's bait menu screen: one bait slot up top with hints about the two
 * baits (Beer - one catch, Golden Bait - 15 catches) and the player's
 * inventory below. Custom drawn panel, no vanilla inventory texture.
 */
public class RodBaitScreen extends AbstractContainerScreen<RodBaitMenu> {

    private static final int PANEL_W = 194;
    private static final int PANEL_H = 144;

    public RodBaitScreen(RodBaitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        // Shift the inner slot grid to match our panel (slots are at the menu's
        // default positions offset by +9/+? -> we shift the whole layout).
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

        // Bait slot highlight box.
        int baitX = x + 88;
        int baitY = y + 16;
        fill(poseStack, baitX - 2, baitY - 2, baitX + 18, baitY + 18, 0xFFC89B3C);
        fill(poseStack, baitX - 1, baitY - 1, baitX + 17, baitY + 17, 0xFF8A6D28);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.leftPos + imageWidth / 2, this.topPos + 6, 0xFFE9D9A8);

        // Hints under the bait slot.
        ItemStack bait = this.menu.getRodBait();
        Component hint = bait.isEmpty()
                ? Component.translatable("menu.howtofish.rod_bait_empty")
                : Component.translatable("menu.howtofish.rod_bait_set", bait.getHoverName());
        drawCenteredString(poseStack, this.font, hint, this.leftPos + imageWidth / 2, this.topPos + 38, 0xFFBFE8FF);

        Component can = Component.translatable("menu.howtofish.rod_bait_can");
        drawCenteredString(poseStack, this.font, can, this.leftPos + imageWidth / 2, this.topPos + 48, 0xFFE8D9A8);
        Component golden = Component.translatable("menu.howtofish.rod_bait_golden");
        drawCenteredString(poseStack, this.font, golden, this.leftPos + imageWidth / 2, this.topPos + 58, 0xFFFFD700);

        renderTooltip(poseStack, mouseX, mouseY);
    }
}
