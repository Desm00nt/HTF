package com.howtofish.mod.client;

import com.howtofish.mod.menu.RodBaitMenu;
import com.howtofish.mod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The rod's bait menu screen: one bait socket with a live status line and a
 * two-line bait catalogue with ITEM ICONS (Empty Can - the Spider Crab's
 * lure; Golden Bait - 15 casts). Everything speaks the mod's single GUI
 * dialect ({@link GuiStyle}, same as the radar bar, hotbar and inventory).
 */
public class RodBaitScreen extends AbstractContainerScreen<RodBaitMenu> {

    private static final int PANEL_W = 194;
    private static final int PANEL_H = 164;   // + a 22px footer for the bait catalogue

    public RodBaitScreen(RodBaitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
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
        GuiStyle.panel(poseStack, x, y, imageWidth, imageHeight, true);

        // Bait socket: golden active cell + teal corner brackets ("magic").
        int baitX = x + 88;
        int baitY = y + 16;
        boolean baitHover = isHover(baitX, baitY, mouseX, mouseY);
        GuiStyle.slotCell(poseStack, baitX, baitY, true, baitHover);
        int s = 4;
        fill(poseStack, baitX - 3, baitY - 3, baitX - 3 + s, baitY - 2, GuiStyle.TEAL);
        fill(poseStack, baitX - 3, baitY - 3, baitX - 2, baitY - 3 + s, GuiStyle.TEAL);
        fill(poseStack, baitX + 19 - s, baitY - 3, baitX + 21, baitY - 2, GuiStyle.TEAL);
        fill(poseStack, baitX + 20, baitY - 3, baitX + 21, baitY - 3 + s, GuiStyle.TEAL);
        fill(poseStack, baitX - 3, baitY + 18, baitX - 3 + s, baitY + 19, GuiStyle.TEAL);
        fill(poseStack, baitX - 3, baitY + 19 - s, baitX - 2, baitY + 19, GuiStyle.TEAL);
        fill(poseStack, baitX + 19 - s, baitY + 18, baitX + 21, baitY + 19, GuiStyle.TEAL);
        fill(poseStack, baitX + 20, baitY + 19 - s, baitX + 21, baitY + 19, GuiStyle.TEAL);

        // Player inventory rows, framed like the inventory screen.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int cx = x + 8 + col * 18;
                int cy = y + 64 + row * 18;
                GuiStyle.slotCell(poseStack, cx, cy, false, isHover(cx, cy, mouseX, mouseY));
            }
        }
        for (int col = 0; col < 9; col++) {
            int cx = x + 8 + col * 18;
            GuiStyle.slotCell(poseStack, cx, y + 118, col < 3, isHover(cx, y + 118, mouseX, mouseY));
        }
        GuiStyle.separator(poseStack, x + 8, y + 58, imageWidth - 16);
        // Bait catalogue footer: icon + short description per row.
        GuiStyle.separator(poseStack, x + 8, y + 140, imageWidth - 16);
        catalogueRow(poseStack, y + 144, new ItemStack(ModItems.EMPTY_CAN.get()),
                Component.translatable("menu.howtofish.rod_bait_can"), 0xFFE8D9A8);
        catalogueRow(poseStack, y + 154, new ItemStack(ModItems.BAIT.get()),
                Component.translatable("menu.howtofish.rod_bait_golden"), 0xFFFFD700);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        GuiStyle.title(poseStack, this.font, this.title, this.leftPos + imageWidth / 2, this.topPos + 6);

        ItemStack bait = this.menu.getRodBait();
        Component hint = bait.isEmpty()
                ? Component.translatable("menu.howtofish.rod_bait_empty")
                : Component.translatable("menu.howtofish.rod_bait_set", bait.getHoverName());
        GuiStyle.caption(poseStack, this.font, hint, this.leftPos + imageWidth / 2, this.topPos + 30);
    }

    /** icon + centered label on one line of the catalogue. */
    private void catalogueRow(PoseStack pose, int rowY, ItemStack icon, Component label, int color) {
        int ix = this.leftPos + 24;
        pose.pushPose();
        pose.translate(0, 0, 20);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(icon, ix, rowY);
        pose.popPose();
        drawString(poseStack, this.font, label, ix + 14, rowY + 3, color);
    }

    private boolean isHover(int x, int y, int mx, int my) {
        return mx >= x && mx < x + 18 && my >= y && my < y + 18;
    }
}
