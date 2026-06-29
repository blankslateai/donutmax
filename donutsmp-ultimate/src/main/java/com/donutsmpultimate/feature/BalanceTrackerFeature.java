package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Toolkit: Balance Tracker
 * When you look at a player, shows their balance (and kill/death stats)
 * in the centre of the screen without typing /bal.
 */
public class BalanceTrackerFeature {

    private static final BalanceTrackerFeature INSTANCE = new BalanceTrackerFeature();
    public static BalanceTrackerFeature getInstance() { return INSTANCE; }

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableBalanceTracker) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.crosshairTarget == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult ehr)) return;

        Entity target = ehr.getEntity();
        if (!(target instanceof PlayerEntity player)) return;

        String name = player.getName().getString();
        PlayerStats stats = DonutAPI.getInstance().getCachedStats(name);

        int sw = client.getWindow().getScaledWidth();
        int y = 4;
        int cx = sw / 2;

        // Player name
        String nameStr = "§e" + name;
        ctx.drawText(client.textRenderer, nameStr,
                cx - client.textRenderer.getWidth(nameStr) / 2, y, 0xFFFFFF, true);
        y += 12;

        // Balance or loading indicator
        String balStr;
        if (stats != null) {
            balStr = "§6Balance: §f$" + PlayerStats.formatMoney(stats.money);
        } else {
            balStr = "§7Balance: §8loading...";
        }
        ctx.drawText(client.textRenderer, balStr,
                cx - client.textRenderer.getWidth(balStr) / 2, y, 0xFFFFFF, true);

        // Extra stats if available
        if (stats != null) {
            y += 12;
            String statsStr = String.format("§aKills: §f%d  §cDeaths: §f%d",
                    stats.kills, stats.deaths);
            ctx.drawText(client.textRenderer, statsStr,
                    cx - client.textRenderer.getWidth(statsStr) / 2, y, 0xFFFFFF, true);
        }
    }
}
