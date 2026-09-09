package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.network.ModNetwork;
import com.howtofish.mod.network.OpenRodMenuPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side input & screen events:
 * - "B" key with the rod in hand opens the rod's BAIT menu,
 * - the vanilla inventory screen is replaced by the trimmed
 *   {@link FishingInventoryScreen} (3 hotbar slots + 27 storage slots),
 * - slot selection stays locked to the three visible hotbar cells: number
 *   keys 4-9 are swallowed, and the SCROLL WHEEL keeps working but cycles
 *   0-1-2 only (vanilla's 9-slot cycle is replaced, then echoed to the
 *   server; the server clamps as a safety net).
 */
public class ClientEvents {

    public static final KeyMapping ROD_MENU = new KeyMapping(
            "key.howtofish.rod_menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.howtofish");

    @Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(ROD_MENU);
        }
    }

    @Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (ROD_MENU.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ModNetwork.CHANNEL.sendToServer(new OpenRodMenuPacket());
                }
            }
            // LMB is the REEL STROKE during a hooked fight (RMB stays
            // cast/hook only). Edge-detected straight from the window so we
            // never consume the vanilla attack input.
            if (mc.player != null && mc.level != null && mc.screen == null) {
                boolean left = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                        mc.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                        == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                if (left && !prevLeftDown && fightActive(mc)) {
                    ModNetwork.CHANNEL.sendToServer(new com.howtofish.mod.network.ReelClickPacket());
                }
                prevLeftDown = left;
            } else {
                prevLeftDown = false;
            }
        }

        private static boolean prevLeftDown;

        private static boolean fightActive(Minecraft mc) {
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof com.howtofish.mod.entity.BobberEntity bobber
                        && bobber.getSyncedOwner() == mc.player
                        && bobber.getState() == com.howtofish.mod.entity.BobberEntity.STATE_HOOKED) {
                    return true;
                }
            }
            return false;
        }

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getScreen() instanceof InventoryScreen
                    && !(event.getScreen() instanceof FishingInventoryScreen)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && !mc.player.getAbilities().instabuild) {
                    event.setNewScreen(new FishingInventoryScreen(mc.player));
                }
            }
        }

        /** Block digit keys 4..9 while the 3-slot hotbar is active. */
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;
            if (!HotbarHudOverlay.customMode(mc.player)) return;
            int idx = event.getKey() - GLFW.GLFW_KEY_1;
            if (idx >= 0 && idx < 9 && idx > 2) {
                event.setCanceled(true);
            }
        }

        /**
         * The wheel STILL WORKS in custom mode - it just cycles the three
         * visible cells (0..2) instead of all nine. We cancel vanilla's own
         * wheel handling to replace it, so nothing can end up "half selected"
         * and no vanilla frame is drawn over our HUD.
         */
        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;
            Player player = mc.player;
            if (!HotbarHudOverlay.customMode(player)) return;
            double dy = event.getScrollDelta();
            if (dy == 0.0) return;
            event.setCanceled(true);
            int dir = dy > 0 ? -1 : 1; // scroll up = previous cell, like vanilla
            int current = player.getInventory().selected;
            int next = Mth.clamp(current + dir, 0, 2);
            if (next != current) {
                player.getInventory().selected = next;
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(next));
            }
        }
    }
}
