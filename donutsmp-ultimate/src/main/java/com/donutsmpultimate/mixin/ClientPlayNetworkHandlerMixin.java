package com.donutsmpultimate.mixin;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts incoming game messages to:
 * 1. Auto-capture the API key when the server sends it after /api
 * 2. Pass messages to chat-processing features (auction bids, payment doubler, anti-scam, HUD)
 *
 * This does NOT modify packets or send anything to the server.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    // DonutSMP sends the key in a message matching this pattern after /api
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "[Aa][Pp][Ii]\\s*[Kk]ey[:\\s]+([A-Za-z0-9\\-_]{8,})"
    );

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String plain = packet.content().getString();

        // ── 1. API key auto-capture ────────────────────────────────────
        Matcher m = API_KEY_PATTERN.matcher(plain);
        if (m.find()) {
            String key = m.group(1).trim();
            DonutConfig cfg = DonutConfig.getInstance();
            if (!cfg.apiKey.equals(key)) {
                cfg.apiKey = key;
                cfg.save();
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(
                        net.minecraft.text.Text.literal("§a[DonutSMP Ultimate] API key saved automatically!"), false);
                }
            }
        }

        // ── 2. Route to feature processors ────────────────────────────
        net.minecraft.text.Text msg = packet.content();

        // Anti-scam check
        com.donutsmpultimate.feature.AntiScamFeature.getInstance().isScam(msg);

        // Auction bid tracking
        com.donutsmpultimate.feature.AuctionFeature.getInstance().processMessage(msg);

        // HUD balance update
        com.donutsmpultimate.feature.HudFeature.getInstance().onBalanceChatLine(plain);
    }
}
