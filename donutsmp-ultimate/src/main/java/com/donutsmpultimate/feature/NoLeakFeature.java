package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * Toolkit: No Leak
 * Visually replaces Bedrock and Tuff blocks with Deepslate so bases using them
 * as "walls" don't stand out on stream. This is purely client-side visual —
 * the actual blocks on the server are unchanged.
 *
 * Called from BlockRenderManagerMixin before any block state is used for rendering.
 */
public class NoLeakFeature {

    private static final NoLeakFeature INSTANCE = new NoLeakFeature();
    public static NoLeakFeature getInstance() { return INSTANCE; }

    /**
     * Returns a replacement BlockState for rendering, or the original if no swap is needed.
     */
    public BlockState getRenderState(BlockState state) {
        if (!DonutConfig.getInstance().enableNoLeak) return state;

        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.TUFF) {
            return Blocks.DEEPSLATE.getDefaultState();
        }
        return state;
    }
}
