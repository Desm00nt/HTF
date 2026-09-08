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
        int boxH = state == BobberEntity.STATE_HOOKED ? 40 : 26;
        int x = w - boxW - 6;
        int y = h / 2 - boxH / 2;

        // Unified dialect: same panel language as the radar bar (GuiStyle).
        GuiStyle.panel(pose, x, y, boxW, boxH, false);

        if (state == BobberEntity.STATE_BITE) {
            // Flashing "HIT IT!" alert.
            boolean bright = ((int) (time * 8.0f)) % 2 == 0;
            Component text = Component.translatable("hud.howtofish.bite");
            mc.font.draw(pose, text, x + boxW / 2.0f - mc.font.width(text) / 2.0f, y + 6,
                    bright ? 0xFFFF3B3B : 0xFFFFE14D);
            Component hint = Component.translatable("hud.howtofish.bite_hint");
            mc.font.draw(pose, hint, x + boxW / 2.0f - mc.font.width(hint) / 2.0f, y + 16, 0xFFE8E8E8);
        } else if (state == BobberEntity.STATE_HOOKED) {
            CustomFishEntity fish = bobber.getSyncedFish();
            Component fishName = fish != null
                    ? Component.translatable("fish.howtofish." + fish.getFishType().getId())
                    : Component.translatable("hud.howtofish.pulling");
            mc.font.draw(pose, fishName, x + boxW / 2.0f - mc.font.width(fishName) / 2.0f, y + 4,
                    fish != null ? 0xFF000000 | fish.getFishType().getColor() : 0xFFBFE8FF);

            // Progress: distance reeled in since the hook-up, in percent.
            float hookDist = bobber.getHookDistance();
            float span = Math.max(4.0f, hookDist - 2.0f);
            float progress = Mth.clamp(1.0f - (player.distanceTo(bobber) - 2.0f) / span, 0.02f, 1.0f);
            int barX = x + 5;
            int barY = y + boxH - 8;
            int barW = boxW - 10;
            fill(pose, barX, barY, barX + barW, barY + 4, 0xFF2A3A44);
            fill(pose, barX, barY, barX + (int) (barW * progress), barY + 4, 0xFF38D0A0);
            fill(pose, barX + (int) (barW * progress) - 1, barY - 1, barX + (int) (barW * progress) + 1, barY + 5, 0xFFE8FFF6);
            String pct = (int) (progress * 100) + "%";
            mc.font.drawShadow(pose, pct, x + boxW - mc.font.width(pct) - 5, y + 14, 0xFFBFE8FF);

            // Flashing command while the fish dashes: the ONLY moment it matters
            // to click - teaching the mechanic right where it used to confuse.
            if (bobber.isFishStruggling()) {
                boolean bright = ((int) (time * 10.0f)) % 2 == 0;
                Component go = Component.translatable("hud.howtofish.click_now");
                mc.font.draw(pose, go, x + boxW / 2.0f - mc.font.width(go) / 2.0f, y + 23,
                        bright ? 0xFFFF4040 : 0xFFFFE14D);
            } else {
                Component ok = Component.translatable("hud.howtofish.hold");
                mc.font.draw(pose, ok, x + boxW / 2.0f - mc.font.width(ok) / 2.0f, y + 23, 0xFF7FA8B8);
            }
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
