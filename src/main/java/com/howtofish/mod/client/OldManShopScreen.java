package com.howtofish.mod.client;

import com.howtofish.mod.menu.OldManShopMenu;
import com.howtofish.mod.network.BuyItemPacket;
import com.howtofish.mod.network.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple custom-drawn shop screen listing the Old Man's goods with icons and
 * buy buttons; the balance the player carries is shown at the bottom. Buying
 * goes through a client -&gt; server packet (see {@link BuyItemPacket}).
 */
public class OldManShopScreen extends AbstractContainerScreen<OldManShopMenu> {

    private static final int ROW_H = 22;

    public OldManShopScreen(OldManShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 40 + OldManShopMenu.OFFERS.size() * ROW_H + 16;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.titleLabelY = -4000; // hide default title/inventory labels, we draw our own
        this.inventoryLabelY = -4000;

        int y = topPos + 30;
        for (OldManShopMenu.ShopOffer offer : OldManShopMenu.OFFERS) {
            Component label = Component.translatable("message.howtofish.buy_button",
                    offer.display().getHoverName(), offer.price());
            this.addRenderableWidget(new Button(leftPos + 44, y, imageWidth - 56, 18, label,
                    b -> ModNetwork.CHANNEL.sendToServer(new BuyItemPacket(offer.id()))));
            y += ROW_H;
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // Same dialect as the rest of the mod's GUI (see GuiStyle).
        GuiStyle.panel(poseStack, leftPos, topPos, imageWidth, imageHeight, true);
        for (int i = 0; i < OldManShopMenu.OFFERS.size(); i++) {
            int ry = topPos + 30 + i * ROW_H;
            // row plate + hover highlight
            fill(poseStack, leftPos + 8, ry, leftPos + imageWidth - 8, ry + ROW_H - 2, 0x50081219);
            boolean hover = mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                    && mouseY >= ry && mouseY < ry + ROW_H - 2;
            if (hover) fill(poseStack, leftPos + 8, ry, leftPos + imageWidth - 8, ry + ROW_H - 2, 0x2838D0A0);
            GuiStyle.slotCell(poseStack, leftPos + 10, ry + 1, false, hover);
        }
        GuiStyle.separator(poseStack, leftPos + 8, topPos + imageHeight - 18, imageWidth - 16);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        GuiStyle.title(poseStack, this.font, this.title, leftPos + imageWidth / 2, topPos + 6);

        // Goods icons now live inside the framed slot cells from renderBg.
        int y = topPos + 30;
        for (OldManShopMenu.ShopOffer offer : OldManShopMenu.OFFERS) {
            this.itemRenderer.renderAndDecorateItem(offer.display().copy(), leftPos + 11, y + 2);
            y += ROW_H;
        }

        // Balance footer.
        Component balance = Component.translatable("menu.howtofish.balance",
                CurrencyHudOverlay.getClientBalance());
        drawCenteredString(poseStack, this.font, balance,
                leftPos + imageWidth / 2, topPos + imageHeight - 12, 0xFFFFD77A);

        renderTooltip(poseStack, mouseX, mouseY);
    }
}
