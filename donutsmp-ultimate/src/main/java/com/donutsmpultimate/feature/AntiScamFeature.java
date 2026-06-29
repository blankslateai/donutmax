package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Toolkit: Anti-Scam
 * Detects common DonutSMP scam/trap patterns in incoming chat messages:
 * - Commands embedded in messages asking you to click/run them
 * - Fake /pay messages asking you to confirm
 * - Teleport trap language
 *
 * Blocked messages are replaced with a warning; the original is hidden.
 */
public class AntiScamFeature {

    private static final AntiScamFeature INSTANCE = new AntiScamFeature();
    public static AntiScamFeature getInstance() { return INSTANCE; }

    // Patterns that indicate a scam/trap message
    private static final List<Pattern> SCAM_PATTERNS = List.of(
        // Messages containing /pay or /withdraw commands targeting YOU
        Pattern.compile("/pay\\s+\\S+\\s+[\\d,\\.]+", Pattern.CASE_INSENSITIVE),
        // Fake "confirm" pay requests
        Pattern.compile("confirm\\s+payment", Pattern.CASE_INSENSITIVE),
        // Common trap language
        Pattern.compile("free\\s+(items|gear|money|stuff).*tp", Pattern.CASE_INSENSITIVE),
        Pattern.compile("tp\\s+to\\s+me.*free", Pattern.CASE_INSENSITIVE),
        // "/withdraw" scam
        Pattern.compile("/withdraw\\s+[\\d,]+", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Checks an incoming message for scam patterns.
     * @return true if the message should be BLOCKED (cancelled + warning shown), false if safe.
     */
    public boolean isScam(Text message) {
        if (!DonutConfig.getInstance().enableAntiScam) return false;
        String plain = message.getString();
        for (Pattern p : SCAM_PATTERNS) {
            if (p.matcher(plain).find()) {
                showWarning(plain);
                return true; // cancel original message
            }
        }
        return false;
    }

    private void showWarning(String original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.player.sendMessage(
            Text.literal("§c§l[ANTI-SCAM] §r§cPotential scam/trap blocked:\n§7" + original), false);
    }
}
