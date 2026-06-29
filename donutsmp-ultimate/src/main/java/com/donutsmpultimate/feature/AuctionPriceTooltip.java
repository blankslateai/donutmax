package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Mod 3: Auction Price Checker
 * Adds the lowest AH price to the tooltip of any item you hover over.
 * Uses the DonutSMP API to query live prices asynchronously.
 */
public class AuctionPriceTooltip {

    public static void register() {
        ItemTooltipCallback.EVENT.register(AuctionPriceTooltip::onTooltip);
    }

    private static void onTooltip(ItemStack stack, ItemTooltipCallback.Context ctx,
                                   net.minecraft.item.tooltip.TooltipType type,
                                   List<Text> lines) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableAuctionPrice || cfg.apiKey.isEmpty()) return;
        if (stack.isEmpty()) return;

        long price = DonutAPI.getInstance().getCachedAuctionPrice(stack);

        if (price < 0) {
            // Still fetching — show a loading indicator
            lines.add(Text.literal("§7AH Price: §8...fetching..."));
        } else {
            String formatted = PlayerStats.formatMoney(price);
            lines.add(Text.literal("§6AH Lowest: §e$" + formatted));
        }
    }
}
