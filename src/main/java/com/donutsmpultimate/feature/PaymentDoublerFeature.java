package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.text.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mod 2: Payment Doubler
 * Detects incoming payment messages and appends a [D] button that, when clicked,
 * loads a /pay command to send double the received amount back.
 *
 * Expected message format from DonutSMP: "PlayerName paid you $amount"
 */
public class PaymentDoublerFeature {

    private static final PaymentDoublerFeature INSTANCE = new PaymentDoublerFeature();
    public static PaymentDoublerFeature getInstance() { return INSTANCE; }

    private static final Pattern PAYMENT_PATTERN = Pattern.compile(
            "^([\\w_]+) paid you \\$([\\d,\\.]+[KMBkmb]?)$"
    );

    /**
     * Called for every incoming game message.
     * Returns a modified Text with [D] appended, or null to leave the message unchanged.
     */
    public Text processMessage(Text original) {
        if (!DonutConfig.getInstance().enablePaymentDoubler) return null;

        String plain = original.getString().trim();
        Matcher m = PAYMENT_PATTERN.matcher(plain);
        if (!m.matches()) return null;

        String sender    = m.group(1);
        String amountStr = m.group(2);
        long amount      = parseMoney(amountStr);
        long doubled     = amount * 2;

        String command = "/pay " + sender + " " + doubled;

        MutableText result = original.copy();
        result.append(Text.literal(" "));

        MutableText dButton = Text.literal("§a§l[D]")
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Double payment: /pay " + sender
                                        + " $" + PlayerStats.formatMoney(doubled)))));
        result.append(dButton);
        return result;
    }

    /** Parses abbreviated money strings (1.5B, 300M, 10K) into raw longs. */
    public static long parseMoney(String s) {
        s = s.replace(",", "").trim();
        try {
            char last = Character.toUpperCase(s.charAt(s.length() - 1));
            if (last == 'B') return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1_000_000_000);
            if (last == 'M') return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1_000_000);
            if (last == 'K') return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1_000);
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
