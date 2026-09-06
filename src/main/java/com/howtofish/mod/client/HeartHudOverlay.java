package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces the vanilla hearts row with one BIG heart in the middle of the
 * screen. The heart fills from the bottom according to the player's health,
 * using a vanilla-style heart texture (empty shell + full fill).
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HeartHudOverlay extends GuiComponent {

    private static final ResourceLocation HEART_EMPTY =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/gui/heart_empty.png");
    private static final ResourceLocation HEART_FULL =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/gui/heart_full.png");

    private static final int TEX_SIZE = 16;   // source png is 16x16
    private static final int ART = 13;        // visible art rows/cols inside
    private static final int GUI_SCALE = 4;   // big heart: 4x -> 64 px on screen

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        // Hide the vanilla hearts row - we draw our own big heart instead.
        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        float hp = Mth.clamp(mc.player.getHealth() / mc.player.getMaxHealth(), 0.0f, 1.0f);

        // Panic pulse when low on health.
        float pulse = 1.0f;
        if (hp <= 0.3f) {
            pulse = 1.0f + Mth.sin((mc.player.tickCount + mc.getFrameTime()) * 0.35f) * 0.06f;
        }

        int size = ART * GUI_SCALE;
        int cx = w / 2;
        int cy = h / 2 - 52;

        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(pulse, pulse, 1.0f);
        pose.translate(-size / 2.0f, 0, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Empty shell.
        RenderSystem.setShaderTexture(0, HEART_EMPTY);
        blit(pose, 0, 0, size, size, 0.0f, 0.0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);

        // Full heart, revealed bottom-up row by row based on health.
        RenderSystem.setShaderTexture(0, HEART_FULL);
        float visibleRows = hp * ART;
        for (int row = 0; row < ART; row++) {
            // Row 0 = top of the art. Fill from the bottom.
            if (row >= ART - visibleRows) {
                blit(pose, 0, row * GUI_SCALE, size, GUI_SCALE, 0.0f, (float) row, TEX_SIZE, 1, TEX_SIZE, TEX_SIZE);
            }
        }

        RenderSystem.disableBlend();
        pose.popPose();

        // Numeric health under the heart.
        String text = (int) Math.ceil(mc.player.getHealth()) + "/" + (int) mc.player.getMaxHealth();
        mc.font.drawShadow(pose, text, cx - mc.font.width(text) / 2.0f, cy + size + 3, 0xFFFFFFFF);
        // A tiny label so the newcomer understands what this means.
        Component label = Component.translatable("hud.howtofish.health");
        mc.font.drawShadow(pose, label, cx - mc.font.width(label) / 2.0f, cy + size + 12, 0xFFB8D8C8);
    }
}
