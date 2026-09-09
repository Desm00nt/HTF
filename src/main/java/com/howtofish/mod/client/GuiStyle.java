package com.howtofish.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;

/**
 * The mod's one and only GUI language, shared by every custom screen and HUD
 * so nothing ever looks "borrowed from vanilla": deep sea-night panel, thin
 * slate frame with a light top rail, gold headers, ice-cyan labels and teal
 * accents - the exact dialect of the radar bar and the custom hotbar.
 */
public final class GuiStyle extends GuiComponent {

    // Palette (single source of truth).
    public static final int PANEL_BG    = 0xF0111F28; // deep sea night
    public static final int PANEL_TOP   = 0xFF3E5A66; // light slate top rail
    public static final int PANEL_EDGE  = 0xFF2A3E47;
    public static final int PANEL_BOT   = 0xFF0C1418;
    public static final int HEADER_BG   = 0xF022343E; // slightly lighter header band
    public static final int SLOT_BG     = 0x4C000000; // faint inset shadow (vanilla-like)
    public static final int SLOT_EDGE   = 0xFF33505E;
    public static final int SLOT_HOVER  = 0x2AFFFFFF; // soft light wash on hover
    public static final int GOLD        = 0xFFE9D9A8;
    public static final int GOLD_FRAME  = 0xFFC89B3C;
    public static final int ACTIVE_GLOW = 0x1E38D0A0; // very light teal tint for "active" cells
    public static final int TEXT_LABEL  = 0xFFBFE8FF;
    public static final int TEXT_DIM    = 0xFF6E8894;
    public static final int TEAL        = 0xFF38D0A0;

    private GuiStyle() {}

    /** Panel body: bg + 1px frame + header band separator. */
    public static void panel(PoseStack pose, int x, int y, int w, int h, boolean headerBand) {
        fill(pose, x, y, x + w, y + h, PANEL_BG);
        fill(pose, x, y, x + w, y + 1, PANEL_TOP);
        fill(pose, x, y + h - 1, x + w, y + h, PANEL_BOT);
        fill(pose, x, y, x + 1, y + h, PANEL_EDGE);
        fill(pose, x + w - 1, y, x + w, y + h, PANEL_EDGE);
        if (headerBand) {
            fill(pose, x + 1, y + 1, x + w - 1, y + 18, HEADER_BG);
            fill(pose, x + 1, y + 18, x + w - 1, y + 19, 0xFF0A161E);
            // twin porthole rivets on the header for character
            fill(pose, x + 4, y + 9, 2, 2, 0xFF5A7A8C);
            fill(pose, x + w - 6, y + 9, 2, 2, 0xFF5A7A8C);
        }
    }

    /** One recessed 18px slot cell (interior at x+1..x+17), radar-bar line style. */
    public static void slotCell(PoseStack pose, int x, int y, boolean active, boolean hovered) {
        fill(pose, x, y, x + 18, y + 18, SLOT_BG);
        if (active) fill(pose, x + 1, y + 1, x + 17, y + 17, ACTIVE_GLOW);
        if (hovered) fill(pose, x + 1, y + 1, x + 17, y + 17, SLOT_HOVER);
        fill(pose, x, y, x + 18, y + 1, PANEL_EDGE);
        fill(pose, x, y, x + 1, y + 18, PANEL_EDGE);
        fill(pose, x + 17, y + 1, x + 18, y + 18, PANEL_BOT);
        fill(pose, x + 1, y + 17, x + 18, y + 18, PANEL_BOT);
        if (active) {
            fill(pose, x - 1, y - 1, x + 19, y, GOLD_FRAME);
            fill(pose, x - 1, y + 18, x + 19, y + 19, GOLD_FRAME);
            fill(pose, x - 1, y, x, y + 18, GOLD_FRAME);
            fill(pose, x + 18, y, x + 19, y + 18, GOLD_FRAME);
        }
    }

    /** Centered gold title over the header band. */
    public static void title(PoseStack pose, Font font, Component text, int cx, int y) {
        font.drawShadow(pose, text, cx - font.width(text) / 2.0f, y, GOLD);
    }

    /** Small caps-ish dim caption, centered. */
    public static void caption(PoseStack pose, Font font, Component text, int cx, int y) {
        font.drawShadow(pose, text, cx - font.width(text) / 2.0f, y, TEXT_LABEL);
    }

    /** Thin teal separator line. */
    public static void separator(PoseStack pose, int x, int y, int w) {
        fill(pose, x, y, x + w, y + 1, 0xFF1C333F);
        fill(pose, x + w / 2 - 12, y, x + w / 2 + 12, y + 1, TEAL);
    }
}
