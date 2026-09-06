package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.BobberEntity;
import com.howtofish.mod.entity.CustomFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fishing HUD, drawn in the right corner of the screen:
 * - "Подсекай!" flashing alert during a BITE,
 * - a pull indicator with a progress bar while a fish is being reeled in.
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class FishingHudOverlay extends GuiComponent {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;

        BobberEntity bobber = findBobber(mc);
        if (bobber == null) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float time = (player.tickCount + mc.getFrameTime()) * 0.05f;
        int state = bobber.getState();

        int boxW = 96;
        int boxH = state == BobberEntity.STATE_HOOKED ? 34 : 26;
        int x = w - boxW - 6;
        int y = h / 2 - boxH / 2;

        fill(pose, x, y, x + boxW, y + boxH, 0xA0000000);
        fill(pose, x, y, x + boxW, y + 1, 0xFF5A7A8C);

        if (state == BobberEntity.STATE_BITE) {
            // Flashing "HIT IT!" alert.
            boolean bright = ((int) (time * 8.0f)) % 2 == 0;
            Component text = Component.translatable("hud.howtofish.bite");
            mc.font.draw(pose, text, x + boxW / 2.0f - mc.font.width(text) / 2.0f, y + 6,
                    bright ? 0xFFFF3B3B : 0xFFFFE14D);
            Component hint = Component.translatable("hud.howtofish.bite_hint");
            mc.font.draw(pose, hint, x + boxW / 2.0f - mc.font.width(hint) / 2.0f, y + 16, 0xFFE8E8E8);
        } else if (state == BobberEntity.STATE_HOOKED) {
            Component label = Component.translatable("hud.howtofish.pulling");
            mc.font.draw(pose, label, x + 5, y + 5, 0xFFBFE8FF);

            CustomFishEntity fish = bobber.getSyncedFish();
            if (fish != null) {
                Component fishName = Component.translatable("fish.howtofish." + fish.getFishType().getId());
                int color = fish.getFishType().getColor();
                mc.font.draw(pose, fishName, x + boxW / 2.0f - mc.font.width(fishName) / 2.0f, y + 15,
                        0xFF000000 | color);
            }

            // Progress: how close the fish is to the player.
            float progress = Mth.clamp(1.0f - (player.distanceTo(bobber) - 2.0f) / 28.0f, 0.02f, 1.0f);
            int barX = x + 5;
            int barY = y + boxH - 8;
            int barW = boxW - 10;
            fill(pose, barX, barY, barX + barW, barY + 4, 0xFF2A3A44);
            fill(pose, barX, barY, barX + (int) (barW * progress), barY + 4, 0xFF38D0A0);
            fill(pose, barX + (int) (barW * progress) - 1, barY - 1, barX + (int) (barW * progress) + 1, barY + 5, 0xFFE8FFF6);
        } else {
            Component text = Component.translatable("hud.howtofish.waiting");
            mc.font.draw(pose, text, x + boxW / 2.0f - mc.font.width(text) / 2.0f, y + 6, 0xFF9FC3D4);
            Component hint = Component.translatable("hud.howtofish.waiting_hint");
            mc.font.draw(pose, hint, x + boxW / 2.0f - mc.font.width(hint) / 2.0f, y + 15, 0xFF6E8894);
        }
    }

    private static BobberEntity findBobber(Minecraft mc) {
        Player player = mc.player;
        if (player == null) return null;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof BobberEntity bobber && bobber.getSyncedOwner() == player) {
                return bobber;
            }
        }
        return null;
    }
}
