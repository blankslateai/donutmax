package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mod 2: Auction Manager
 * - Tracks auction timer and highest bid from chat
 * - Renders a movable HUD overlay showing remaining time and top bidder
 * - Handles "pick auction" by looking at the block you're targeting
 */
public class AuctionFeature {

    private static final AuctionFeature INSTANCE = new AuctionFeature();
    public static AuctionFeature getInstance() { return INSTANCE; }

    // Chat pattern: "[Auction] PlayerName bid $500,000"
    private static final Pattern BID_PATTERN = Pattern.compile(
            "\\[Auction\\]\\s+([\\w_]+)\\s+bid\\s+\\$([\\d,\\.]+[KMBkmb]?)"
    );

    private boolean auctionRunning = false;
    private long auctionEndTime = 0;
    private long highestBid = 0;
    private String highestBidder = "";
    private boolean movingHud = false;

    public boolean isRunning() { return auctionRunning; }

    /** Start or stop an auction. */
    public void toggleAuction() {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableAuction) return;
        if (auctionRunning) {
            stopAuction();
        } else {
            startAuction();
        }
    }

    private void startAuction() {
        DonutConfig cfg = DonutConfig.getInstance();
        auctionRunning = true;
        auctionEndTime = System.currentTimeMillis() + (long) cfg.auctionTimerSeconds * 1000;
        highestBid = 0;
        highestBidder = "";
        sendMessage("§aAuction started! Timer: " + cfg.auctionTimerSeconds + "s");
    }

    private void stopAuction() {
        auctionRunning = false;
        if (highestBidder.isEmpty()) {
            sendMessage("§cAuction stopped. No bids.");
        } else {
            sendMessage("§6Auction ended! Winner: §e" + highestBidder +
                        " §6with §e$" + com.donutsmpultimate.api.PlayerStats.formatMoney(highestBid));
        }
    }

    /**
     * "Pick Auction" — opens the AH for the block you're currently targeting.
     * Sends /ah <block_name> [stack] to chat.
     */
    public void pickAuction() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget == null) return;
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bhr)) return;

        if (client.world == null) return;
        net.minecraft.block.BlockState state = client.world.getBlockState(bhr.getBlockPos());
        String blockName = state.getBlock().getName().getString().toLowerCase().replace(" ", "_");

        String cmd = "ah " + blockName;
        if (DonutConfig.getInstance().enableAutoAuctionStack) cmd += " stack";
        client.getNetworkHandler().sendChatCommand(cmd);
    }

    /** Process incoming chat messages to detect bids. */
    public void processMessage(Text message) {
        if (!auctionRunning) return;
        String plain = message.getString();
        Matcher m = BID_PATTERN.matcher(plain);
        if (!m.find()) return;

        String bidder = m.group(1);
        long bid = PaymentDoublerFeature.parseMoney(m.group(2));
        if (bid > highestBid) {
            highestBid = bid;
            highestBidder = bidder;
        }
    }

    /** Render the auction HUD. Call from HudRenderCallback. */
    public void renderHud(DrawContext ctx, RenderTickCounter ticker) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableAuction || !auctionRunning) return;

        long remaining = auctionEndTime - System.currentTimeMillis();
        if (remaining <= 0) {
            stopAuction();
            return;
        }

        int seconds = (int) (remaining / 1000);
        MinecraftClient client = MinecraftClient.getInstance();

        int x = cfg.auctionHudX;
        int y = cfg.auctionHudY;
        int bgColor = 0x88000000;

        // Background box
        ctx.fill(x - 2, y - 2, x + 160, y + 28, bgColor);

        // Timer line
        String timerStr = "§eAuction §7| §f" + seconds + "s remaining";
        ctx.drawText(client.textRenderer, timerStr, x, y, 0xFFFFFF, true);

        // Highest bid line
        String bidStr;
        if (highestBidder.isEmpty()) {
            bidStr = "§7No bids yet";
        } else {
            bidStr = "§aTop: §e" + highestBidder + " §f$" +
                     com.donutsmpultimate.api.PlayerStats.formatMoney(highestBid);
        }
        ctx.drawText(client.textRenderer, bidStr, x, y + 12, 0xFFFFFF, true);
    }

    public void setMovingHud(boolean v) { movingHud = v; }
    public boolean isMovingHud() { return movingHud; }

    private void sendMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(msg), false);
        }
    }

    /** Tick — auto-stop when timer expires even without new bids. */
    public void tick() {
        if (auctionRunning && System.currentTimeMillis() > auctionEndTime) {
            stopAuction();
        }
    }
}
