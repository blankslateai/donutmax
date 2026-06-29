package com.donutsmpultimate.mixin;

import com.donutsmpultimate.feature.NoLeakFeature;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts BlockRenderManager.renderBlock to swap bedrock/tuff → deepslate.
 * The swap only affects rendering — the server sees the real blocks.
 */
@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {

    @ModifyVariable(
        method = "renderBlock",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1   // BlockState parameter
    )
    private BlockState onRenderBlock(BlockState state) {
        return NoLeakFeature.getInstance().getRenderState(state);
    }
}
