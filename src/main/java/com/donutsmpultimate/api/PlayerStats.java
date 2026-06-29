package com.donutsmpultimate.api;

/**
 * Holds all stats fetched from the DonutSMP API for a single player.
 * Maps to the API response fields: money, shards, sell_money, shop_money, deaths, kills.
 */
public class PlayerStats {
    public final String username;
    public final long money;
    public final long shards;
    public final long sellMoney;
    public final long shopMoney;
    public final int deaths;
    public final int kills;
    public final long fetchedAt;

    public PlayerStats(String username, long money, long shards, long sellMoney,
                       long shopMoney, int deaths, int kills) {
        this.username = username;
        this.money = money;
        this.shards = shards;
        this.sellMoney = sellMoney;
        this.shopMoney = shopMoney;
        this.deaths = deaths;
        this.kills = kills;
        this.fetchedAt = System.currentTimeMillis();
    }

    /** Returns true if this cache entry is older than ttlMillis. */
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - fetchedAt > ttlMillis;
    }

    /** Formats a money value as a human-readable abbreviated string (e.g., 1.5B, 300M). */
    public static String formatMoney(long amount) {
        if (amount >= 1_000_000_000L) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000L) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000L) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }

    /**
     * Returns the color int for the money amount.
     * Red (0xFF5555) for 10B+, Gold (0xFFAA00) for 1B+, Green (0x55FF55) for under 1B.
     */
    public static int moneyColor(long money) {
        if (money >= 10_000_000_000L) return 0xFF5555; // Red
        if (money >= 1_000_000_000L)  return 0xFFAA00; // Gold
        return 0x55FF55;                                // Green
    }
}
