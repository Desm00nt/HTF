package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.item.BaitKind;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Custom HUD hotbar for the "How To Fish" experience: instead of nine vanilla
 * cells the player sees THREE active slots, the rod's BAIT cell and a big
 * heart. Everything is hand-drawn (no vanilla widgets.png sampling - that was
 * the source of the "shifted / half-hidden" glitch): cells, borders and the
 * selection frame are plain rects at fixed pixel offsets, items use the same
 * vertical offset vanilla uses for slot contents, so nothing can drift.
 * <p>
 * The vanilla HOTBAR overlay is cancelled while this mode is on (survival);
 * creative/spectator keep the vanilla bar untouched. Slots 3-8 stay REAL
 * inventory slots (they are shown in the inventory screen) - nothing is ever
 * moved out of them, so items can never "disappear into a hidden row".
 */
@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HotbarHudOverlay extends GuiComponent {

    /** Vanilla hotbar geometry: cell pitch, first-cell x anchor, top of the bar. */
    private static final int SLOT_PITCH = 20;
    private static final int CELL_X = 18;   // cell + border footprint
    private static final int BAIT_GAP = 6;
    private static final int HEART_GAP = 8;

    /** True when the custom (3-slot) HUD replaces the vanilla bar. */
    public static boolean customMode(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }

    @SubscribeEvent
    public static void onPreRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !customMode(mc.player)) return;
        // The vanilla bar is fully replaced by ours.
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || !customMode(player)) return;

        PoseStack pose = event.getPoseStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Inventory inv = player.getInventory();

        // --- Layout: identical anchor vanilla uses, so it never "shifts". ---
        int j = w / 2 - 91;          // x of first cell content
        int barY = h - 22;           // top of a hotbar cell (vanilla)
        int itemY = h - 20;          // items draw 2 px BELOW the cell top (vanilla!)
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Backdrop strip behind the three cells.
        fill(pose, j - 3, barY - 2, j + 3 * SLOT_PITCH + 1, barY + 20, 0x90101820);
        for (int i = 0; i < 3; i++) {
            int cx = j + i * SLOT_PITCH;
            // Cell interior + border.
            fill(pose, cx - 1, barY - 1, cx + CELL_X - 1, barY + CELL_X - 1, 0x80000000);
            drawBorder(pose, cx - 2, barY - 2, CELL_X + 2, 0x70FFFFFF);
        }
        // Selection frame around the ACTIVE cell (a bright 2 px ring - the only
        // thing that ever highlights a slot, so it can never land "half-way").
        int sel = Mth.clamp(inv.selected, 0, 2);
        drawFrame(pose, j + sel * SLOT_PITCH - 3, barY - 3, 0xFFFFD75E);
        // Dim outline on inactive cells for readability at night.
        for (int i = 0; i < 3; i++) {
            if (i == sel) continue;
            drawBorder(pose, j + i * SLOT_PITCH - 2, barY - 2, CELL_X + 2, 0x30FFFFFF);
        }

        // Slot items (vanilla content offsets: interior starts at +2,+2 of the cell).
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int cellX = j + i * SLOT_PITCH + 2;
            // In RenderGuiEvent.Post nothing guarantees the GUI pipeline state,
            // so set exactly what vanilla sets before item rendering.
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            mc.getItemRenderer().renderAndDecorateItem(stack, cellX, itemY);
            drawCount(pose, mc, stack, cellX, itemY);
            drawDurability(pose, stack, cellX + 1, itemY + 14);
        }

        // --- Bait cell of the held rod. -------------------------------------
        int baitX = j + 3 * SLOT_PITCH + BAIT_GAP;
        fill(pose, baitX - 2, barY - 2, baitX + CELL_X, barY + CELL_X - 1, 0x90101820);
        drawBorder(pose, baitX - 2, barY - 2, CELL_X + 2, 0x809BFF5E); // green-ish = bait
        ItemStack rod = player.getMainHandItem().getItem() instanceof FishingRodCustomItem
                ? player.getMainHandItem() : player.getOffhandItem();
        ItemStack bait = FishingRodCustomItem.getBait(rod);
        if (!bait.isEmpty()) {
            mc.getItemRenderer().renderAndDecorateItem(bait, baitX + 2, itemY);
            if (BaitKind.of(bait) == BaitKind.GOLDEN) {
                int uses = com.howtofish.mod.item.BaitItem.MAX_USES
                        - com.howtofish.mod.item.BaitItem.getBaitDamage(bait);
                int green = (int) (11.0f * Mth.clamp(uses / (float) com.howtofish.mod.item.BaitItem.MAX_USES, 0f, 1f));
                fill(pose, baitX + 3, itemY + 13, baitX + 3 + Math.max(green, 1), itemY + 15,
                        uses > 5 ? 0xFF55FF55 : 0xFFFF5555);
            } else {
                // Single-use lures show a dot instead of a bar.
                fill(pose, baitX + 7, itemY + 13, baitX + 11, itemY + 15, 0xFF9BE8FF);
            }
        } else {
            mc.font.draw(pose, "B", baitX + 7, itemY + 5, 0x80AABB99);
        }

        // --- Big heart + HP, right of the bait cell. -------------------------
        HeartHudOverlay.drawHeart(pose, mc, baitX + CELL_X + HEART_GAP, barY - 1, 20);
    }

    /** 1 px rectangular border. */
    private static void drawBorder(PoseStack pose, int x, int y, int size, int color) {
        fill(pose, x, y, x + size, y + 1, color);
        fill(pose, x, y + size - 1, x + size, y + size, color);
        fill(pose, x, y + 1, x + 1, y + size - 1, color);
        fill(pose, x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    /** 2 px thick selection frame (vanilla frame footprint 24x24 at -3,-3). */
    private static void drawFrame(PoseStack pose, int x, int y, int color) {
        int size = 24;
        fill(pose, x, y, x + size, y + 2, color);
        fill(pose, x, y + size - 2, x + size, y + size, color);
        fill(pose, x, y + 2, x + 2, y + size - 2, color);
        fill(pose, x + size - 2, y + 2, x + size, y + size - 2, color);
    }

    private static void drawCount(PoseStack pose, Minecraft mc, ItemStack stack, int x, int y) {
        if (stack.getCount() <= 1) return;
        String text = String.valueOf(stack.getCount());
        mc.font.drawShadow(pose, text, x + 16 - mc.font.width(text), y + 9, 0xFFFFFFFF);
    }

    private static void drawDurability(PoseStack pose, ItemStack stack, int x, int y) {
        if (!stack.isDamageableItem()) return;
        float frac = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
        if (frac >= 1.0f) return;
        int w = (int) (13.0f * frac);
        int color = frac > 0.5f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
        fill(pose, x, y, x + 13, y + 2, 0xFF202020);
        fill(pose, x, y, x + w, y + 2, color);
    }
}
