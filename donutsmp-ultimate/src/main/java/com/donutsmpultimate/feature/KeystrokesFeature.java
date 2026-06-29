package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.option.KeyBinding;

/**
 * Toolkit: Keystrokes — renders WASD + Space + LMB/RMB on screen.
 */
public class KeystrokesFeature {

    private static final KeystrokesFeature INSTANCE = new KeystrokesFeature();
    public static KeystrokesFeature getInstance() { return INSTANCE; }

    private static final int KEY_W    = 16; // width of one key box
    private static final int KEY_H    = 14;
    private static final int GAP      = 2;
    private static final int COL_ACTIVE   = 0xDD55FF55;
    private static final int COL_INACTIVE = 0x88222222;
    private static final int TEXT_ACTIVE   = 0xFF000000;
    private static final int TEXT_INACTIVE = 0xFFAAAAAA;

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableKeystrokes) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null) return; // hide when in a menu

        // Bottom-right anchored
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int baseX = screenW - (KEY_W * 3 + GAP * 2) - 8;
        int baseY = screenH - (KEY_H * 3 + GAP * 2) - 8;

        boolean w     = client.options.forwardKey.isPressed();
        boolean a     = client.options.leftKey.isPressed();
        boolean s     = client.options.backKey.isPressed();
        boolean d     = client.options.rightKey.isPressed();
        boolean space = client.options.jumpKey.isPressed();
        boolean lmb   = client.options.attackKey.isPressed();
        boolean rmb   = client.options.useKey.isPressed();

        // Row 1: W (centred)
        drawKey(ctx, client, baseX + KEY_W + GAP, baseY,             "W",     w);
        // Row 2: A S D
        drawKey(ctx, client, baseX,                baseY + KEY_H + GAP, "A",   a);
        drawKey(ctx, client, baseX + KEY_W + GAP,  baseY + KEY_H + GAP, "S",  s);
        drawKey(ctx, client, baseX + (KEY_W+GAP)*2,baseY + KEY_H + GAP, "D",  d);
        // Row 3: Space (wide)
        int spaceW = KEY_W * 3 + GAP * 2;
        drawKeyW(ctx, client, baseX, baseY + (KEY_H + GAP) * 2, spaceW, KEY_H, "SPACE", space);
        // LMB / RMB small above WASD
        drawKey(ctx, client, baseX,                baseY - KEY_H - GAP, "LMB", lmb);
        drawKey(ctx, client, baseX + (KEY_W+GAP)*2,baseY - KEY_H - GAP, "RMB", rmb);
    }

    private void drawKey(DrawContext ctx, MinecraftClient client,
                          int x, int y, String label, boolean pressed) {
        drawKeyW(ctx, client, x, y, KEY_W, KEY_H, label, pressed);
    }

    private void drawKeyW(DrawContext ctx, MinecraftClient client,
                           int x, int y, int w, int h, String label, boolean pressed) {
        int bg   = pressed ? COL_ACTIVE   : COL_INACTIVE;
        int text = pressed ? TEXT_ACTIVE  : TEXT_INACTIVE;
        ctx.fill(x, y, x + w, y + h, bg);
        // Centred text
        int tw = client.textRenderer.getWidth(label);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - 8) / 2;
        ctx.drawText(client.textRenderer, label, tx, ty, text, false);
    }
}
