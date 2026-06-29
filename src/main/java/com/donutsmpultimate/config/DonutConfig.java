package com.donutsmpultimate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

/**
 * Single config class for every feature in the mod.
 * Stored at .minecraft/config/donutsmp-ultimate.json
 */
public class DonutConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("DonutSMPUltimate/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DonutConfig INSTANCE;

    // ── API ───────────────────────────────────────────────────────────────
    public String apiKey = "";

    // ── Nametag / Stats Display (Mod 1 + Mod 2) ──────────────────────────
    public boolean enableBalanceDisplay   = true;   // floating balance above head
    public boolean enableStatsDisplay     = true;   // full stats line above nametag
    public String  statsFormat            = "§6{money} §7| §b{kills}k §c{deaths}d";
    public long    statsRefreshMs         = 5000;   // how often to re-fetch per player

    // ── Payment Doubler (Mod 2) ───────────────────────────────────────────
    public boolean enablePaymentDoubler   = true;

    // ── Auction (Mod 2) ───────────────────────────────────────────────────
    public boolean enableAuction          = true;
    public int     auctionTimerSeconds    = 60;
    public int     auctionHudX            = 10;
    public int     auctionHudY            = 80;

    // ── Look-Pay (Mod 2) ──────────────────────────────────────────────────
    public boolean enableLookPay          = true;
    public long    lookPayAmount          = 1000;

    // ── Command Modifiers (Mod 2) ─────────────────────────────────────────
    public boolean enableAutoAuctionStack = true;
    public boolean enableFasterGear       = true;

    // ── Auction Price Tooltip (Mod 3) ─────────────────────────────────────
    public boolean enableAuctionPrice     = true;

    // ── HUD (Toolkit) ─────────────────────────────────────────────────────
    public boolean enableHUD              = true;
    public boolean hudShowBalance         = true;
    public boolean hudShowCoords          = true;

    // ── Keystrokes (Toolkit) ──────────────────────────────────────────────
    public boolean enableKeystrokes       = true;

    // ── Chest Filter (Toolkit) ────────────────────────────────────────────
    public boolean enableChestFilter      = true;

    // ── Coordinate Saver (Toolkit) ────────────────────────────────────────
    public boolean enableCoordSaver       = true;

    // ── Totem Warning (Toolkit) ───────────────────────────────────────────
    public boolean enableTotemWarning     = true;

    // ── Zoom (Toolkit) ────────────────────────────────────────────────────
    public boolean enableZoom             = true;
    public float   zoomFovDivisor         = 4.0f;

    // ── Region Map (Toolkit) ──────────────────────────────────────────────
    public boolean enableRegionMap        = true;

    // ── Balance Tracker (Toolkit) ─────────────────────────────────────────
    public boolean enableBalanceTracker   = true;

    // ── Ender Chest Viewer (Toolkit) ──────────────────────────────────────
    public boolean enableEnderChestViewer = true;

    // ── Fake Loot (Toolkit) ───────────────────────────────────────────────
    public boolean enableFakeLoot         = false;
    public String  fakeLootItem           = "minecraft:diamond";

    // ── No Leak (Toolkit) ─────────────────────────────────────────────────
    public boolean enableNoLeak           = true;

    // ── Anti-Scam (Toolkit) ───────────────────────────────────────────────
    public boolean enableAntiScam         = true;

    // ── Spotify HUD (Toolkit, Windows only) ───────────────────────────────
    public boolean enableSpotifyHUD       = false;

    // ─────────────────────────────────────────────────────────────────────

    public static DonutConfig getInstance() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutsmp-ultimate.json");
    }

    public static DonutConfig load() {
        File file = configPath().toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                DonutConfig cfg = GSON.fromJson(reader, DonutConfig.class);
                if (cfg != null) {
                    INSTANCE = cfg;
                    return cfg;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults: {}", e.getMessage());
            }
        }
        INSTANCE = new DonutConfig();
        INSTANCE.save();
        return INSTANCE;
    }

    public void save() {
        try (Writer writer = new FileWriter(configPath().toFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}
