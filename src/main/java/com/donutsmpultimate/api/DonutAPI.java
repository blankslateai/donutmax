package com.donutsmpultimate.api;

import com.donutsmpultimate.config.DonutConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handles ALL network communication for this mod.
 * Only communicates with api.donutsmp.net — nowhere else.
 * Uses async/non-blocking HTTP to avoid freezing the game thread.
 */
public class DonutAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger("DonutSMPUltimate/API");

    // ── ONLY allowed host ──────────────────────────────────────────────────
    private static final String API_BASE = "https://api.donutsmp.net/v1";
    // ──────────────────────────────────────────────────────────────────────

    private static DonutAPI INSTANCE;

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    /** Stats cache: username → PlayerStats */
    private final Map<String, PlayerStats> statsCache = new ConcurrentHashMap<>();
    /** Set of usernames currently being fetched (prevents duplicate requests) */
    private final Set<String> pendingFetches = ConcurrentHashMap.newKeySet();
    /** Auction price cache: cacheKey → lowest price */
    private final Map<String, Long> auctionPriceCache = new ConcurrentHashMap<>();
    /** Tracks last request time for rate limiting (max 250 req/min) */
    private final Deque<Long> requestTimestamps = new ArrayDeque<>();

    private DonutAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        // Background thread for scheduled refreshes
        this.scheduler = Executors.newScheduledThreadPool(2,
                r -> { Thread t = new Thread(r, "DonutAPI-Worker"); t.setDaemon(true); return t; });
    }

    public static DonutAPI getInstance() {
        if (INSTANCE == null) INSTANCE = new DonutAPI();
        return INSTANCE;
    }

    // ───────────────────────── Rate limiting ──────────────────────────────

    private synchronized boolean canRequest() {
        long now = System.currentTimeMillis();
        // Remove timestamps older than 60 seconds
        while (!requestTimestamps.isEmpty() && now - requestTimestamps.peekFirst() > 60_000) {
            requestTimestamps.pollFirst();
        }
        if (requestTimestamps.size() >= 245) return false; // Stay under 250/min
        requestTimestamps.addLast(now);
        return true;
    }

    // ───────────────────────── Player Stats ───────────────────────────────

    /**
     * Returns cached stats for a player, or null if not yet fetched.
     * Kicks off an async fetch if needed.
     */
    public PlayerStats getCachedStats(String username) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (cfg.apiKey.isEmpty()) return null;

        PlayerStats cached = statsCache.get(username.toLowerCase());
        if (cached == null || cached.isExpired(cfg.statsRefreshMs)) {
            fetchStatsAsync(username);
        }
        return cached;
    }

    /** Fetches stats asynchronously. Result is stored in statsCache when done. */
    public void fetchStatsAsync(String username) {
        String key = username.toLowerCase();
        if (pendingFetches.contains(key)) return;
        if (!canRequest()) return;

        pendingFetches.add(key);
        CompletableFuture.supplyAsync(() -> fetchStatsBlocking(username), scheduler)
                .thenAccept(stats -> {
                    if (stats != null) statsCache.put(key, stats);
                    pendingFetches.remove(key);
                })
                .exceptionally(ex -> {
                    pendingFetches.remove(key);
                    LOGGER.warn("Failed to fetch stats for {}: {}", username, ex.getMessage());
                    return null;
                });
    }

    private PlayerStats fetchStatsBlocking(String username) {
        String url = API_BASE + "/player/" + username;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + DonutConfig.getInstance().apiKey)
                    .header("User-Agent", "DonutSMPUltimate/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.debug("Stats API returned {} for {}", response.statusCode(), username);
                return null;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return parsePlayerStats(username, json);
        } catch (Exception e) {
            LOGGER.debug("Error fetching stats for {}: {}", username, e.getMessage());
            return null;
        }
    }

    private PlayerStats parsePlayerStats(String username, JsonObject json) {
        // Handle both flat and nested response formats
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : json;
        long money     = getlong(data, "money", 0);
        long shards    = getlong(data, "shards", 0);
        long sellMoney = getlong(data, "sell_money", 0);
        long shopMoney = getlong(data, "shop_money", 0);
        int deaths     = getInt(data, "deaths", 0);
        int kills      = getInt(data, "kills", 0);
        return new PlayerStats(username, money, shards, sellMoney, shopMoney, deaths, kills);
    }

    // ───────────────────────── Auction Prices ─────────────────────────────

    /**
     * Returns the cached lowest auction price for the given item stack, or -1 if unknown.
     * Kicks off an async fetch if needed.
     */
    public long getCachedAuctionPrice(ItemStack stack) {
        if (DonutConfig.getInstance().apiKey.isEmpty()) return -1;
        String cacheKey = buildAuctionKey(stack);
        Long cached = auctionPriceCache.get(cacheKey);
        if (cached == null) {
            fetchAuctionPriceAsync(stack, cacheKey);
            return -1;
        }
        return cached;
    }

    private void fetchAuctionPriceAsync(ItemStack stack, String cacheKey) {
        if (pendingFetches.contains("ah:" + cacheKey)) return;
        if (!canRequest()) return;
        pendingFetches.add("ah:" + cacheKey);

        CompletableFuture.supplyAsync(() -> fetchAuctionPriceBlocking(stack), scheduler)
                .thenAccept(price -> {
                    if (price >= 0) auctionPriceCache.put(cacheKey, price);
                    pendingFetches.remove("ah:" + cacheKey);
                })
                .exceptionally(ex -> {
                    pendingFetches.remove("ah:" + cacheKey);
                    return null;
                });
    }

    private long fetchAuctionPriceBlocking(ItemStack stack) {
        String material = Registries.ITEM.getId(stack.getItem()).getPath().replace("_", " ");
        // Build enchantment query string
        StringBuilder query = new StringBuilder(material);
        stack.getEnchantments().getEnchantments().forEach(entry ->
                query.append(" ").append(Registries.ENCHANTMENT.getKey(entry.value()).map(k -> k.getValue().getPath()).orElse(""))
        );

        String url = API_BASE + "/auctions?item=" + query.toString().trim().replace(" ", "%20");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + DonutConfig.getInstance().apiKey)
                    .header("User-Agent", "DonutSMPUltimate/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return -1;

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray auctions = json.has("auctions") ? json.getAsJsonArray("auctions") : new JsonArray();

            long lowest = Long.MAX_VALUE;
            for (JsonElement elem : auctions) {
                JsonObject listing = elem.getAsJsonObject();
                long price = getlong(listing, "price", Long.MAX_VALUE);
                if (price < lowest) lowest = price;
            }
            return lowest == Long.MAX_VALUE ? -1 : lowest;
        } catch (Exception e) {
            return -1;
        }
    }

    // ───────────────────────── Helpers ────────────────────────────────────

    private String buildAuctionKey(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        StringBuilder key = new StringBuilder(id.toString());
        stack.getEnchantments().getEnchantments().forEach(entry ->
                key.append("|").append(Registries.ENCHANTMENT.getKey(entry.value()).map(k -> k.getValue().toString()).orElse(""))
        );
        return key.toString();
    }

    private long getlong(JsonObject obj, String key, long def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : def;
    }

    private int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
