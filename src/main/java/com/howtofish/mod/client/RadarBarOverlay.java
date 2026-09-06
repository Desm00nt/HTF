package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bossbar-style heading strip at the TOP of the screen, shown while the radar
 * is active. The bright marker slides along the bar to show the bearing of
 * the second island relative to where the player is looking; the caption
 * shows the distance in blocks.
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class RadarBarOverlay extends GuiComponent {

    /** Mirrored from the server via SyncRadarPacket. */
    public static volatile boolean clientRadarActive = false;

    private static final int BAR_W = 182;
    private static final int BAR_H = 7;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;
        if (!clientRadarActive) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();

        int x = w / 2 - BAR_W / 2;
        int y = 6;

        Vec3 island = new Vec3(
                com.howtofish.mod.world.IslandBuilder.SECOND_ISLAND_ORIGIN.getX() + 0.5,
                0,
                com.howtofish.mod.world.IslandBuilder.SECOND_ISLAND_ORIGIN.getZ() + 0.5);

        double distX = island.x - player.getX();
        double distZ = island.z - player.getZ();
        double dist = Math.sqrt(distX * distX + distZ * distZ);

        // Bearing of the island minus player's yaw -> relative angle in [-180, 180].
        float playerYaw = player.getYRot();
        float angleToIsland = (float) (Math.toDegrees(Mth.atan2(distZ, distX))) - 90.0f;
        float rel = Mth.wrapDegrees(angleToIsland - playerYaw);

        // Panel.
        fill(pose, x - 4, y - 4, x + BAR_W + 4, y + BAR_H + 14, 0x88000000);
        // Bar background (dark) + segments like a bossbar.
        fill(pose, x, y, x + BAR_W, y + BAR_H, 0xFF24191F);
        for (int i = 0; i < BAR_W; i += 13) {
            fill(pose, x + i, y, x + i + 1, y + BAR_H, 0x55FFFFFF);
        }
        // Colored "signal" gradient towards the island direction marker.
        float marker = Mth.clamp(rel / 90.0f, -1.0f, 1.0f);
        int markerX = x + BAR_W / 2 + (int) (marker * (BAR_W / 2 - 4));

        fill(pose, x, y, x + BAR_W, y + 1, 0xFF0F0A0D);
        fill(pose, x, y + BAR_H - 1, x + BAR_W, y + BAR_H, 0xFF3A2A33);
        // Signal strip between center and marker.
        int stripLeft = Math.min(x + BAR_W / 2, markerX);
        int stripRight = Math.max(x + BAR_W / 2, markerX);
        fill(pose, stripLeft, y + 1, stripRight, y + BAR_H - 1, 0xFF2F8F6B);

        // Center tick (player heading).
        fill(pose, x + BAR_W / 2 - 1, y - 3, x + BAR_W / 2 + 1, y + BAR_H + 3, 0xFFE8E8E8);

        // Island marker: bright diamond.
        fill(pose, markerX - 1, y - 4, markerX + 1, y + BAR_H + 4, 0xFFFFD75A);
        fill(pose, markerX - 3, y - 2, markerX + 3, y + BAR_H + 2, 0xFFFFD75A);
        fill(pose, markerX - 2, y - 3, markerX + 2, y + BAR_H + 3, 0xFFFFE88A);

        // Caption.
        Component caption = Component.translatable("hud.howtofish.radar_caption",
                (int) dist, rel > 15 ? "\u25B8" : rel < -15 ? "\u25C2" : "\u25B2");
        mc.font.drawShadow(pose, caption, w / 2.0f - mc.font.width(caption) / 2.0f, y + BAR_H + 2, 0xFFFFE9A8);
    }
}
