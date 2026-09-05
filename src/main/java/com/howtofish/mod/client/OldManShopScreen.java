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
 * Simple custom-drawn shop screen listing the Old Man's goods
 * (Fishing Rod 3, Knife 4, Radar 10, Beer 2 Rubles) with buy buttons.
 * Written against the 1.19.2 PoseStack-based Screen/AbstractContainerScreen API.
 */
public class OldManShopScreen extends AbstractContainerScreen<OldManShopMenu> {

    public OldManShopScreen(OldManShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 32 + OldManShopMenu.OFFERS.size() * 22;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.titleLabelY = -4000; // hide default title/inventory labels, we draw our own
        this.inventoryLabelY = -4000;

        int y = topPos + 28;
        for (OldManShopMenu.ShopOffer offer : OldManShopMenu.OFFERS) {
            Component label = Component.literal(offer.display().getHoverName().getString() + " - " + offer.price() + "\u20BD");
            this.addRenderableWidget(new Button(leftPos + 10, y, imageWidth - 20, 18, label,
                    b -> ModNetwork.CHANNEL.sendToServer(new BuyItemPacket(offer.id()))));
            y += 22;
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        fill(poseStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0202020);
        fill(poseStack, leftPos, topPos, leftPos + imageWidth, topPos + 20, 0xE0402A18);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, leftPos + imageWidth / 2, topPos + 6, 0xFFE9D9A8);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}
