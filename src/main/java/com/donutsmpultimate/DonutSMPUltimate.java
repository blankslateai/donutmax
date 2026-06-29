package com.donutsmpultimate;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.config.DonutConfig;
import com.donutsmpultimate.feature.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║              DonutSMP Ultimate — v1.0.0                 ║
 * ║  Combines:                                              ║
 * ║  • Donut SMP Money Display Mod (balance nametags)       ║
 * ║  • DonutExtras (stats display, payment doubler,         ║
 * ║    auction, look-pay, command mods)                     ║
 * ║  • donut_auctions (AH price tooltips)                   ║
 * ║  • DonutSMP Toolkit (HUD, keystrokes, chest filter,     ║
 * ║    coord saver, totem warning, zoom, region map,        ║
 * ║    balance tracker, ender chest viewer, fake loot,      ║
 * ║    no leak, anti-scam, Spotify HUD)                     ║
 * ║                                                         ║
 * ║  Network: ONLY communicates with api.donutsmp.net       ║
 * ║  No RATs. No telemetry. No obfuscation.                 ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class DonutSMPUltimate implements ClientModInitializer {

    public static final String MOD_ID = "donutsmpultimate";
    public static final Logger LOGGER  = LoggerFactory.getLogger("DonutSMPUltimate");

    // ── Keybinds ─────────────────────────────────────────────────────────
    public static KeyBinding KEY_LOOK_PAY;
    public static KeyBinding KEY_START_AUCTION;
    public static KeyBinding KEY_PICK_AUCTION;
    public static KeyBinding KEY_MOVE_AUCTION_HUD;
    public static KeyBinding KEY_SAVE_COORDS;
    public static KeyBinding KEY_ZOOM;
    public static KeyBinding KEY_OPEN_ENDER_CHEST;
    public static KeyBinding KEY_OPEN_CONFIG;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DonutSMP Ultimate] Initialising...");

        // Load config first (so all features have settings available)
        DonutConfig.getInstance();

        // ── Register keybinds ────────────────────────────────────────────
        KEY_LOOK_PAY = reg("look_pay",        GLFW.GLFW_KEY_V);
        KEY_START_AUCTION    = reg("start_auction",  GLFW.GLFW_KEY_B);
        KEY_PICK_AUCTION     = reg("pick_auction",   GLFW.GLFW_KEY_N);
        KEY_MOVE_AUCTION_HUD = reg("move_auction_hud", GLFW.GLFW_KEY_UNKNOWN);
        KEY_SAVE_COORDS      = reg("save_coords",    GLFW.GLFW_KEY_Y);
        KEY_ZOOM             = reg("zoom",           GLFW.GLFW_KEY_C);
        KEY_OPEN_ENDER_CHEST = reg("open_ender_chest", GLFW.GLFW_KEY_UNKNOWN);
        KEY_OPEN_CONFIG      = reg("open_config",    GLFW.GLFW_KEY_UNKNOWN);

        // ── Register features ────────────────────────────────────────────
        AuctionPriceTooltip.register();
        SpotifyHudFeature.getInstance().start();

        // ── HUD rendering ────────────────────────────────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            HudFeature.getInstance().render(drawContext, tickCounter);
            KeystrokesFeature.getInstance().render(drawContext, tickCounter);
            TotemWarningFeature.getInstance().render(drawContext, tickCounter);
            BalanceTrackerFeature.getInstance().render(drawContext, tickCounter);
            AuctionFeature.getInstance().renderHud(drawContext, tickCounter);
            RegionMapFeature.getInstance().render(drawContext, tickCounter);
            SpotifyHudFeature.getInstance().render(drawContext, tickCounter);
        });

        // ── Per-tick logic ───────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Zoom held-state tracking
            boolean zoomHeld = KEY_ZOOM.isPressed();
            ZoomFeature.getInstance().setZoomHeld(zoomHeld);
            ZoomFeature.getInstance().tick();

            // Totem warning tick
            TotemWarningFeature.getInstance().tick();

            // Auction timer tick
            AuctionFeature.getInstance().tick();

            // ── Keybind press detection (one-shot) ──────────────────────
            if (KEY_LOOK_PAY.wasPressed()) {
                LookPayFeature.getInstance().execute();
            }

            if (KEY_START_AUCTION.wasPressed()) {
                AuctionFeature.getInstance().toggleAuction();
            }

            if (KEY_PICK_AUCTION.wasPressed()) {
                AuctionFeature.getInstance().pickAuction();
            }

            if (KEY_SAVE_COORDS.wasPressed()) {
                CoordinateSaverFeature.getInstance().saveCurrentPosition();
            }

            if (KEY_OPEN_ENDER_CHEST.wasPressed()) {
                openEnderChestViewer(client);
            }

            if (KEY_OPEN_CONFIG.wasPressed()) {
                // Open config screen (requires ModMenu integration — see DonutConfigScreen)
                LOGGER.info("Config screen keybind pressed (wire up to your config screen).");
            }

            // Pre-fetch stats for nearby players
            if (client.world != null && client.player != null
                    && !DonutConfig.getInstance().apiKey.isEmpty()) {
                client.world.getPlayers().forEach(p -> {
                    if (p != client.player) {
                        DonutAPI.getInstance().fetchStatsAsync(p.getName().getString());
                    }
                });
                // Also fetch own stats (for HUD balance)
                DonutAPI.getInstance().fetchStatsAsync(client.player.getName().getString());
            }
        });

        // ── Outgoing command modification ─────────────────────────────────
        // Auto auction stack + faster gear buying (Mod 2)
        ClientSendMessageEvents.MODIFY_COMMAND.register(command ->
            CommandModifierFeature.getInstance().modify(command)
        );

        // ── Incoming message processing ───────────────────────────────────
        // Payment doubler: inject [D] button into payment messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // Anti-scam: cancel the message entirely if it's a scam
            if (AntiScamFeature.getInstance().isScam(message)) return false;
            return true;
        });

        // Modify game messages to add [D] button to payment messages
        ClientReceiveMessageEvents.MODIFY_GAME_MESSAGE.register((message, overlay) -> {
            Text modified = PaymentDoublerFeature.getInstance().processMessage(message);
            return modified != null ? modified : message;
        });

        LOGGER.info("[DonutSMP Ultimate] Ready! Run /api on DonutSMP to auto-save your API key.");
    }

    /** Shorthand for KeyBindingHelper.registerKeyBinding. */
    private static KeyBinding reg(String id, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + "." + id,
            InputUtil.Type.KEYSYM,
            defaultKey,
            "key.categories." + MOD_ID
        ));
    }

    /** Opens the saved ender chest viewer screen. */
    private static void openEnderChestViewer(MinecraftClient client) {
        if (!DonutConfig.getInstance().enableEnderChestViewer) return;
        if (!EnderChestViewerFeature.getInstance().hasSavedContents()) {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§cNo ender chest saved yet. Open your EC first!"), true);
            }
            return;
        }
        // Open a simple read-only chest screen to display the saved contents.
        // A full GenericContainerScreen requires a server-side screen handler,
        // so we just print the contents to chat for now — replace with a
        // custom Screen implementation (DonutEnderChestScreen) if desired.
        var contents = EnderChestViewerFeature.getInstance().getSavedContents();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§6--- Saved Ender Chest ---"), false);
            for (int i = 0; i < contents.size(); i++) {
                var stack = contents.get(i);
                if (!stack.isEmpty()) {
                    client.player.sendMessage(
                        Text.literal("§7Slot " + i + ": §f" + stack.getCount() + "x " + stack.getName().getString()),
                        false);
                }
            }
        }
    }
}
