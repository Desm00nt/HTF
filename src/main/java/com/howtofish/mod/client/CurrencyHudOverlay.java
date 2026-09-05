package com.howtofish.mod.client;

import com.howtofish.mod.economy.PlayerCurrency;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the player's Ruble balance in the top-left corner, right next to the
 * vanilla hotbar/health area, as requested ("деньги указаны в левом углу
 * рядом со здоровьем"). Uses the 1.19.2 PoseStack-based overlay API.
 */
@Mod.EventBusSubscriber(modid = com.howtofish.mod.HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CurrencyHudOverlay extends GuiComponent {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PoseStack poseStack = event.getPoseStack();

        int rubles = PlayerCurrency.get(mc.player);
        int x = 4;
        int y = mc.getWindow().getGuiScaledHeight() - 68;
        fill(poseStack, x, y, x + 70, y + 14, 0x90000000);
        mc.font.draw(poseStack, "\u20BD " + rubles, x + 5, y + 3, 0xFFF6D77A);
    }
}
