package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Custom hotbar for the "How To Fish" experience: instead of the vanilla
 * nine-slot bar the player sees THREE equipment slots plus the rod's BAIT
 * slot, exactly as requested. Slots 3-8 of the real inventory stay locked
 * (items are automatically moved out of them server-side by ModEvents).
 * Also draws the fishing line durability bars, the selection frame and the
 * bait currently inserted into the held rod.
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HotbarHudOverlay extends GuiComponent {

    private static final ResourceLocation WIDGETS =
            new ResourceLocation("minecraft", "textures/gui/widgets.png");

    private static final int SLOT = 20;      // hotbar cell size (px)
    private static final int FRAME_H = 22;

    @SubscribeEvent
    public static void onPre(RenderGuiOverlayEvent.Pre event) {
        // Hide the vanilla 9-slot hotbar in survival (creative keeps vanilla).
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Inventory inv = player.getInventory();

        // Layout: [3 slots] [bait slot] [big heart + hp]  - bottom center.
        int hotbarW = 61;               // left border + 3 cells (61 px of the strip)
        int baitW = 22;                 // standalone cell box
        int heartSize = 20;
        int totalW = hotbarW + 3 + baitW + 6 + heartSize + 2 + 22;
        int x = w / 2 - totalW / 2;
        int y = h - FRAME_H;

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WIDGETS);

        // 3 hotbar cells (left part of the vanilla strip).
        blit(pose, x, y, 0, 64, hotbarW, FRAME_H, 256, 256);

        // Bait slot: separate square built from the same strip.
        int baitX = x + hotbarW + 3;
        blit(pose, baitX, y, 0, 64, 1, FRAME_H, 256, 256);                 // left border
        blit(pose, baitX + 1, y, 1, 64, 20, FRAME_H, 256, 256);            // one cell
        blit(pose, baitX + 21, y, 181, 64, 1, FRAME_H, 256, 256);          // right border

        // Selection frame around the selected slot (0..2).
        int sel = Mth.clamp(inv.selected, 0, 2);
        blit(pose, x - 1 + sel * SLOT, y - 1, 0, 94, 24, 23, 256, 256);

        // Items in the three slots.
        ItemRenderer ir = mc.getItemRenderer();
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            ir.renderAndDecorateItem(stack, x + 1 + i * SLOT + 2, y + 3);
            drawCount(pose, mc, stack, x + 1 + i * SLOT + 2, y + 3);
            drawDurability(pose, stack, x + 1 + i * SLOT + 3, y + 3 + 16);
        }

        // Bait inside the held rod.
        ItemStack rod = player.getMainHandItem().getItem() instanceof FishingRodCustomItem
                ? player.getMainHandItem() : player.getOffhandItem();
        ItemStack bait = FishingRodCustomItem.getBait(rod);
        if (!bait.isEmpty()) {
            ir.renderAndDecorateItem(bait, baitX + 3, y + 3);
            // Bait uses left (golden bait: Damage counts down from MAX_USES).
            if (bait.getItem() instanceof com.howtofish.mod.item.BaitItem b) {
                int uses = b.MAX_USES - com.howtofish.mod.item.BaitItem.getBaitDamage(bait);
                int green = (int) (12.0f * Mth.clamp(uses / (float) b.MAX_USES, 0f, 1f));
                fill(pose, baitX + 4, y + 17, baitX + 4 + green, y + 19,
                        uses > 5 ? 0xFF55FF55 : 0xFFFF5555);
            }
        } else {
            // Empty bait slot hint: a dim "B".
            mc.font.draw(pose, "B", baitX + 8, y + 7, 0x80667766);
        }

        // Big heart to the right of the bait slot (delegated to HeartHudOverlay logic).
        int heartX = baitX + baitW + 6;
        HeartHudOverlay.drawHeart(pose, mc, heartX, y + 1, heartSize);
        RenderSystem.disableBlend();
    }

    private static void drawCount(PoseStack pose, Minecraft mc, ItemStack stack, int x, int y) {
        if (stack.getCount() <= 1) return;
        String text = String.valueOf(stack.getCount());
        pose.pushPose();
        pose.translate(0, 0, 200);
        mc.font.drawShadow(pose, text, x + 17 - mc.font.width(text), y + 9, 0xFFFFFFFF);
        pose.popPose();
    }

    private static void drawDurability(PoseStack pose, ItemStack stack, int x, int y) {
        if (!stack.isDamageableItem()) return;
        float frac = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
        if (frac >= 1.0f) return;
        int w = (int) (13.0f * frac);
        int color = frac > 0.5f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
        pose.pushPose();
        pose.translate(0, 0, 200);
        fill(pose, x, y, x + 13, y + 2, 0xFF202020);
        fill(pose, x, y, x + w, y + 2, color);
        pose.popPose();
    }
}
