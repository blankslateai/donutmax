package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;

/**
 * Toolkit: Region Map
 * Draws a simple labelled grid representing the DonutSMP region layout.
 * Each region is 1000×1000 blocks. The player's current position is highlighted.
 *
 * Toggle with the open_config keybind or via the config screen.
 */
public class RegionMapFeature {

    private static final RegionMapFeature INSTANCE = new RegionMapFeature();
    public static RegionMapFeature getInstance() { return INSTANCE; }

    private boolean visible = false;

    public void toggleVisible() { visible = !visible; }
    public boolean isVisible()  { return visible; }

    // Each cell represents a 1000×1000 block region.
    // Label and approximate world centre (X, Z).
    private static final Object[][] REGIONS = {
        { "Spawn",   0,      0    },
        { "North",   0,     -2000 },
        { "South",   0,      2000 },
        { "East",    2000,   0    },
        { "West",   -2000,   0    },
        { "NE",      2000,  -2000 },
        { "NW",     -2000,  -2000 },
        { "SE",      2000,   2000 },
        { "SW",     -2000,   2000 },
    };

    private static final int CELL = 28;   // pixels per region cell
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int WORLD_RANGE = 5000; // half-width of displayed area

    public void render(DrawContext ctx, RenderTickCounter ticker) {
        if (!DonutConfig.getInstance().enableRegionMap || !visible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int mapW = COLS * CELL;
        int mapH = ROWS * CELL;
        int ox = sw - mapW - 10;
        int oy = 10;

        // Background
        ctx.fill(ox - 2, oy - 2, ox + mapW + 2, oy + mapH + 2 + 10, 0xCC000000);
        ctx.drawText(client.textRenderer, "§eRegion Map", ox, oy + mapH + 2, 0xFFFFFF, true);

        BlockPos pos = client.player.getBlockPos();
        float playerMapX = ox + mapW / 2.0f + (pos.getX() / (float) WORLD_RANGE) * (mapW / 2.0f);
        float playerMapZ = oy + mapH / 2.0f + (pos.getZ() / (float) WORLD_RANGE) * (mapH / 2.0f);

        // Draw grid
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int cx = ox + col * CELL;
                int cy = oy + row * CELL;
                ctx.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0x55334455);
                // Grid lines
                ctx.fill(cx, cy, cx + CELL, cy + 1, 0x88AAAAAA);
                ctx.fill(cx, cy, cx + 1, cy + CELL, 0x88AAAAAA);
            }
        }

        // Highlight regions by name
        for (Object[] region : REGIONS) {
            String name = (String) region[0];
            int wx = (int) region[1];
            int wz = (int) region[2];
            float rx = ox + mapW / 2.0f + (wx / (float) WORLD_RANGE) * (mapW / 2.0f);
            float rz = oy + mapH / 2.0f + (wz / (float) WORLD_RANGE) * (mapH / 2.0f);
            int tx = (int) rx - client.textRenderer.getWidth(name) / 2;
            int tz = (int) rz - 4;
            ctx.drawText(client.textRenderer, "§7" + name, tx, tz, 0xFFFFFF, false);
        }

        // Player dot
        int px = Math.round(playerMapX);
        int pz = Math.round(playerMapZ);
        px = Math.max(ox, Math.min(ox + mapW - 2, px));
        pz = Math.max(oy, Math.min(oy + mapH - 2, pz));
        ctx.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFFFF5555);
    }
}
