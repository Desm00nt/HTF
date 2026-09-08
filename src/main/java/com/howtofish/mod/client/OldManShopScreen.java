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
        fill(poseStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0151D24);
        fill(poseStack, leftPos, topPos, leftPos + imageWidth, topPos + 20, 0xE0402A18);
        fill(poseStack, leftPos, topPos + 20, leftPos + imageWidth, topPos + 21, 0xFF0A0D10);
        // subtle wood frame
        fill(poseStack, leftPos, topPos, leftPos + 2, topPos + imageHeight, 0xFF2E1C10);
        fill(poseStack, leftPos + imageWidth - 2, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2E1C10);
        fill(poseStack, leftPos, topPos + imageHeight - 2, leftPos + imageWidth, topPos + imageHeight, 0xFF2E1C10);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, leftPos + imageWidth / 2, topPos + 6, 0xFFE9D9A8);

        // Goods icons (drawn over the panel, left of each buy button).
        int y = topPos + 30;
        for (OldManShopMenu.ShopOffer offer : OldManShopMenu.OFFERS) {
            fill(poseStack, leftPos + 10, y + 1, leftPos + 38, y + 19, 0x40000000);
            this.itemRenderer.renderAndDecorateItem(offer.display().copy(), leftPos + 19, y + 2);
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
