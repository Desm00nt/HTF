package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The BIG health heart. It sits at the BOTTOM of the screen, right of the
 * custom hotbar (drawn by {@link HotbarHudOverlay} which calls
 * {@link #drawHeart}), and fills from the bottom up according to the player's
 * health. Uses smooth (non pixel-art) generated textures and pulses when the
 * player is badly hurt. The vanilla hearts row is hidden.
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HeartHudOverlay extends GuiComponent {

    private static final ResourceLocation HEART_EMPTY =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/gui/heart_empty.png");
    private static final ResourceLocation HEART_FULL =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/gui/heart_full.png");

    private static final int TEX = 64;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        // Hide the vanilla hearts row - our big heart lives next to the hotbar.
        event.setCanceled(true);
    }

    /**
     * Draws the big heart with its bottom-up health fill at the given GUI
     * position (top-left corner), scaled to {@code size} GUI pixels.
     */
    public static void drawHeart(PoseStack pose, Minecraft mc, int x, int y, int size) {
        if (mc.player == null) return;
        float hp = Mth.clamp(mc.player.getHealth() / mc.player.getMaxHealth(), 0.0f, 1.0f);

        // Panic pulse when low on health.
        float scale = 1.0f;
        if (hp <= 0.3f) {
            scale = 1.0f + Mth.sin((mc.player.tickCount + mc.getFrameTime()) * 0.35f) * 0.07f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        pose.pushPose();
        // Pulse around the heart's center.
        pose.translate(x + size / 2.0f, y + size / 2.0f, 0);
        pose.scale(scale, scale, 1.0f);
        pose.translate(-size / 2.0f, -size / 2.0f, 0);

        // Empty shell.
        RenderSystem.setShaderTexture(0, HEART_EMPTY);
        GuiComponent.blit(pose, 0, 0, 0.0f, 0.0f, size, size, TEX, TEX);

        // Fill: bottom-up, one GUI-pixel row per blit.
        int rowsVisible = Math.max(1, Mth.ceil(hp * size));
        for (int row = 0; row < rowsVisible; row++) {
            int dstY = size - row - 1;                                     // screen row (bottom-up)
            float srcY = (TEX - 1) * (1.0f - (row + 0.5f) / (float) size); // sampled texture row
            RenderSystem.setShaderTexture(0, HEART_FULL);
            GuiComponent.blit(pose, 0, dstY, size, 1, 0.0f, srcY, TEX, 1, TEX, TEX);
        }

        pose.popPose();

        // HP number right of the heart, vertically centered.
        String text = (int) Math.ceil(mc.player.getHealth()) + "/" + (int) mc.player.getMaxHealth();
        mc.font.drawShadow(pose, text, x + size + 3, y + size / 2.0f - 4, 0xFFFF7070);

        RenderSystem.disableBlend();
    }
}
