package com.donutsmpultimate.feature;

import com.donutsmpultimate.api.DonutAPI;
import com.donutsmpultimate.api.PlayerStats;
import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * Renders balance and stats lines above player nametags.
 * Called from EntityRendererMixin after the vanilla label is rendered.
 * Combines Mod 1 (balance color) + Mod 2 (stats format/placeholders).
 */
public class NametagFeature {

    private static final NametagFeature INSTANCE = new NametagFeature();
    public static NametagFeature getInstance() { return INSTANCE; }

    /**
     * Called from the EntityRendererMixin when a player's nametag is being rendered.
     * @param entity      the player being rendered
     * @param matrices    matrix stack (already positioned above entity)
     * @param vertexConsumers vertex consumer provider
     * @param light       packed light value
     * @param lineOffset  current Y offset (negative = above entity). Start at -0.4f for first extra line.
     */
    public void renderExtraLines(PlayerEntity entity, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light) {
        DonutConfig cfg = DonutConfig.getInstance();
        if (!cfg.enableBalanceDisplay && !cfg.enableStatsDisplay) return;
        if (cfg.apiKey.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String name = entity.getName().getString();
        PlayerStats stats = DonutAPI.getInstance().getCachedStats(name);

        TextRenderer tr = client.textRenderer;
        float scale = 0.025f;

        // Each extra line goes 0.35 units higher than the vanilla nametag
        // We render above the vanilla nametag so the first custom line is at -0.35 * 2 etc.
        // The vanilla nametag sits at y = -entity.getHeight() - 0.5, handled by the mixin position.
        // Here the matrix is already translated to label position; we add more offsets.

        int lineIndex = 1; // vanilla nametag is line 0; we count up above it

        // ── Stats line (Mod 2) ──────────────────────────────────────────
        if (cfg.enableStatsDisplay && stats != null) {
            String formatted = applyPlaceholders(cfg.statsFormat, name, stats);
            renderLine(tr, matrices, vertexConsumers, light, scale, formatted, lineIndex++);
        }

        // ── Balance line (Mod 1) ────────────────────────────────────────
        if (cfg.enableBalanceDisplay) {
            String balText;
            int color;
            if (stats != null) {
                balText = "§l$" + PlayerStats.formatMoney(stats.money);
                color = PlayerStats.moneyColor(stats.money);
            } else {
                balText = "§7$...";
                color = 0xAAAAAA;
            }
            renderLine(tr, matrices, vertexConsumers, light, scale, balText, lineIndex);
        }
    }

    /** Renders a single text line at the given vertical stack index above the entity. */
    private void renderLine(TextRenderer tr, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers, int light,
                             float scale, String text, int lineIndex) {
        matrices.push();
        // Move up by lineIndex * 0.3 world units above the nametag position
        matrices.translate(0.0, lineIndex * 0.3, 0.0);
        matrices.scale(-scale, -scale, scale);

        float x = -tr.getWidth(text) / 2.0f;
        int bgColor = (int) (MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f) * 255.0f) << 24;

        tr.draw(Text.of(text), x, 0, 0xFFFFFF,
                false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH,
                bgColor, light);

        matrices.pop();
    }

    /** Replaces {name}, {money}, {shards}, etc. with actual values. */
    public static String applyPlaceholders(String format, String name, PlayerStats stats) {
        return format
            .replace("{name}", name)
            .replace("{name_unformatted}", name)
            .replace("{money}", "$" + PlayerStats.formatMoney(stats.money))
            .replace("{shards}", PlayerStats.formatMoney(stats.shards))
            .replace("{sell_money}", "$" + PlayerStats.formatMoney(stats.sellMoney))
            .replace("{shop_money}", "$" + PlayerStats.formatMoney(stats.shopMoney))
            .replace("{deaths}", String.valueOf(stats.deaths))
            .replace("{kills}", String.valueOf(stats.kills));
    }
}
