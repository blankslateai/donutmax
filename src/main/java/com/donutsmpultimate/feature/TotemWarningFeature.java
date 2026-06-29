package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

/**
 * Toolkit: Totem Warning
 * Flashes a red warning on screen when the player does not have a Totem of Undying
 * equipped in their offhand or mainhand.
 */
public class TotemWarningFeature {

    private static final TotemWarningFeature INSTANCE = new TotemWarningFeature();
    public static TotemWarningFeature getInstance() { return INSTANCE; }

    private int flashTick = 0;

    public void tick() {
        flashTick++;
    }

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableTotemWarning) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null) return;

        boolean hasTotem = hasTotem(client);
        if (hasTotem) return;

        // Flash every 20 ticks (1 second)
        boolean visible = (flashTick / 10) % 2 == 0;
        if (!visible) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        String msg = "⚠ NO TOTEM ⚠";
        int tw = client.textRenderer.getWidth(msg);
        int x = (sw - tw) / 2;
        int y = sh / 2 + 20;

        ctx.fill(x - 4, y - 2, x + tw + 4, y + 12, 0xBB880000);
        ctx.drawText(client.textRenderer, "§c§l" + msg, x, y, 0xFFFFFF, true);
    }

    private boolean hasTotem(MinecraftClient client) {
        assert client.player != null;
        ItemStack offhand = client.player.getOffHandStack();
        ItemStack mainhand = client.player.getMainHandStack();
        return offhand.isOf(Items.TOTEM_OF_UNDYING)
            || mainhand.isOf(Items.TOTEM_OF_UNDYING)
            || inventoryContainsTotem(client.player.getInventory());
    }

    private boolean inventoryContainsTotem(PlayerInventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }
}
