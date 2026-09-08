package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
 * slot and the big heart, exactly as requested.
 * <p>
 * The layout is anchored to VANILLA hotbar coordinates (cells start at
 * {@code w/2 - 91} with a 20 px pitch, selection frame 3 px left of the cell
 * origin), so even when some vanilla overlay leaks through, nothing can
 * appear "shifted" or doubled. Slots 3-8 of the real inventory stay locked
 * (items are relocated server-side by ModEvents, selection beyond slot 2 is
 * blocked by {@link ClientEvents} and re-synced back by the server).
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HotbarHudOverlay extends GuiComponent {

    private static final ResourceLocation WIDGETS =
            new ResourceLocation("minecraft", "textures/gui/widgets.png");

    /** Vanilla hotbar cell pitch & the 3-cell strip width (1 px border + 3*20). */
    private static final int SLOT_PITCH = 20;
    private static final int STRIP_W = 63;
    private static final int BAIT_GAP = 5;

    /** True when the custom (3-slot) HUD replaces the vanilla bar. */
    public static boolean customMode(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }

    @SubscribeEvent
    public static void onPre(RenderGuiOverlayEvent.Pre event) {
        // Hide the vanilla 9-slot hotbar in survival (creative keeps vanilla).
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            if (customMode(mc.player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || !customMode(player)) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Inventory inv = player.getInventory();

        // --- Layout (vanilla-aligned) -------------------------------------
        // The first slot cell starts at j = w/2 - 91, exactly like vanilla, so
        // everything lines up with whatever vanilla would draw at slot 0.
        int j = w / 2 - 91;
        int stripY = h - 23;

        RenderSystem.setShaderTexture(0, WIDGETS);
        RenderSystem.enableBlend();

        // The 3-slot strip: left border + three cells of the vanilla bar.
        blit(pose, j - 1, stripY, 0, 64, STRIP_W, 22, 256, 256);

        // Bait slot: a standalone vanilla cell right of the strip.
        int baitX = j + STRIP_W + BAIT_GAP;
        blit(pose, baitX, stripY, 0, 64, 1, 22, 256, 256);      // left border
        blit(pose, baitX + 1, stripY, 1, 64, 20, 22, 256, 256); // cell
        blit(pose, baitX + 21, stripY, 181, 64, 1, 22, 256, 256); // right border

        // Selection frame - same math as vanilla Gui#renderHotbar.
        int sel = Mth.clamp(inv.selected, 0, 2);
        blit(pose, j + sel * SLOT_PITCH - 3, stripY - 2, 0, 94, 24, 22, 256, 256);

        // Items in the three slots (vanilla cell interior offsets: +3/+3 of h-20).
        int itemY = h - 20 + 3;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int cellX = j + i * SLOT_PITCH + 3;
            mc.getItemRenderer().renderAndDecorateItem(stack, cellX, itemY);
            drawCount(pose, mc, stack, cellX, itemY);
            drawDurability(pose, stack, cellX + 1, itemY + 16);
        }

        // Bait currently loaded into the held rod.
        ItemStack rod = player.getMainHandItem().getItem() instanceof FishingRodCustomItem
                ? player.getMainHandItem() : player.getOffhandItem();
        ItemStack bait = FishingRodCustomItem.getBait(rod);
        if (!bait.isEmpty()) {
            mc.getItemRenderer().renderAndDecorateItem(bait, baitX + 3, itemY);
            if (bait.getItem() instanceof com.howtofish.mod.item.BaitItem) {
                int uses = com.howtofish.mod.item.BaitItem.MAX_USES
                        - com.howtofish.mod.item.BaitItem.getBaitDamage(bait);
                int green = (int) (12.0f * Mth.clamp(uses / (float) com.howtofish.mod.item.BaitItem.MAX_USES, 0f, 1f));
                pose.pushPose();
                pose.translate(0, 0, 200);
                fill(pose, baitX + 4, itemY + 15, baitX + 4 + Math.max(green, 1), itemY + 17,
                        uses > 5 ? 0xFF55FF55 : 0xFFFF5555);
                pose.popPose();
            }
        } else {
            // Empty bait slot hint: a dim "B".
            mc.font.draw(pose, "B", baitX + 10, itemY + 4, 0x80AABB99);
        }

        // Big heart + HP readout to the right of the bait slot.
        int heartX = baitX + 22 + 6;
        HeartHudOverlay.drawHeart(pose, mc, heartX, stripY + 1, 20);

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
