package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Mod 3: Auction Price Checker
 * Adds the lowest AH price to the tooltip of any item you hover over.
 */
public class AuctionPriceTooltip {

    public static void register() {
        ItemTooltipCallback.EVENT.register(AuctionPriceTooltip::onTooltip);
    }

    private static void onTooltip(ItemStack stack, Item.TooltipContext context,
                                   TooltipType type, List<Text> lines) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableAuctionPrice || cfg.apiKey.isEmpty()) return;
        if (stack.isEmpty()) return;

        long price = DonutAPI.getInstance().getCachedAuctionPrice(stack);

        if (price < 0) {
            lines.add(Text.literal("§7AH Price: §8...fetching..."));
        } else {
            lines.add(Text.literal("§6AH Lowest: §e$" + PlayerStats.formatMoney(price)));
        }
    }
}
