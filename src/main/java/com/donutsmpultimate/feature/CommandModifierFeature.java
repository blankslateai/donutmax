package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mod 2: Command Modifiers
 *
 * 1. Auto Auction Stack — appends "stack" to /ah <item> commands.
 * 2. Faster Gear Buying — expands "/ah maxed chestplate" →
 *    "/ah prot 4 unb 3 mend chestplate" (and similar for all armour types).
 *
 * This is called from ClientSendMessageEvents.MODIFY_COMMAND in the main mod class.
 */
public class CommandModifierFeature {

    private static final CommandModifierFeature INSTANCE = new CommandModifierFeature();
    public static CommandModifierFeature getInstance() { return INSTANCE; }

    // Pattern: "ah <non-stack stuff>" — we append "stack" if not already present.
    private static final Pattern AH_PATTERN = Pattern.compile("^ah (.+)$", Pattern.CASE_INSENSITIVE);

    // Armour enchantment expansion map
    private static final Map<String, String> GEAR_EXPAND = Map.of(
        "maxed chestplate",      "prot 4 unb 3 mend chestplate",
        "maxed neth chestplate", "prot 4 unb 3 mend netherite chestplate",
        "maxed helmet",          "prot 4 unb 3 mend helmet",
        "maxed neth helmet",     "prot 4 unb 3 mend netherite helmet",
        "maxed leggings",        "prot 4 unb 3 mend leggings",
        "maxed neth leggings",   "prot 4 unb 3 mend netherite leggings",
        "maxed boots",           "prot 4 unb 3 mend boots",
        "maxed neth boots",      "prot 4 unb 3 mend netherite boots"
    );

    /**
     * Modifies an outgoing command string before it is sent to the server.
     * @param command the raw command string (without leading '/')
     * @return the (possibly modified) command string
     */
    public String modify(String command) {
        DonutConfig cfg = DonutConfig.getInstance();

        // ── Faster gear buying ──────────────────────────────────────────
        if (cfg.enableFasterGear && command.toLowerCase().startsWith("ah maxed")) {
            String lower = command.substring(3).trim().toLowerCase(); // strip "ah "
            for (Map.Entry<String, String> entry : GEAR_EXPAND.entrySet()) {
                if (lower.equals(entry.getKey())) {
                    command = "ah " + entry.getValue();
                    break;
                }
            }
        }

        // ── Auto auction stack ──────────────────────────────────────────
        if (cfg.enableAutoAuctionStack) {
            Matcher m = AH_PATTERN.matcher(command);
            if (m.matches()) {
                String args = m.group(1).trim();
                // Only add "stack" if it isn't already there
                if (!args.toLowerCase().endsWith(" stack") && !args.equalsIgnoreCase("stack")) {
                    command = "ah " + args + " stack";
                }
            }
        }

        return command;
    }
}
