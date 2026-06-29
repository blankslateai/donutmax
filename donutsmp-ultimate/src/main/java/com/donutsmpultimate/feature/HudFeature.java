package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Toolkit: HUD overlay showing balance and current coordinates in the top-left corner.
 */
public class HudFeature {

    private static final HudFeature INSTANCE = new HudFeature();
    public static HudFeature getInstance() { return INSTANCE; }

    /** Last known balance for own player (updated when we see it in chat or API). */
    private String cachedBalance = "...";

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableHUD) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.debugEnabled) return; // Don't show when F3 is open

        int x = 4;
        int y = 4;
        int lineH = 10;

        // Balance line
        if (cfg.hudShowBalance) {
            String ownName = client.player.getName().getString();
            PlayerStats stats = DonutAPI.getInstance().getCachedStats(ownName);
            if (stats != null) cachedBalance = "$" + PlayerStats.formatMoney(stats.money);
            ctx.drawText(client.textRenderer, "§6Balance: §f" + cachedBalance, x, y, 0xFFFFFF, true);
            y += lineH;
        }

        // Coordinates line
        if (cfg.hudShowCoords) {
            PlayerEntity p = client.player;
            BlockPos pos = p.getBlockPos();
            String coords = String.format("§7XYZ: §f%d §8/ §f%d §8/ §f%d", pos.getX(), pos.getY(), pos.getZ());
            ctx.drawText(client.textRenderer, coords, x, y, 0xFFFFFF, true);
        }
    }

    public void onBalanceChatLine(String line) {
        // Try to parse "Your balance: $X" style messages from the server
        line = line.strip();
        if (line.startsWith("Your balance: $") || line.startsWith("Balance: $")) {
            cachedBalance = line.substring(line.indexOf('$'));
        }
    }
}
