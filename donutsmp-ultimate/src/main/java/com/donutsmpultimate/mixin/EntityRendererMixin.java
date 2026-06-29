package com.donutsmpultimate.mixin;

import com.donutsmpultimate.feature.NametagFeature;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects after the vanilla nametag render to add our custom balance/stats lines.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("TAIL")
    )
    private void onRenderLabel(T entity, Text text, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light,
                               float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;
        NametagFeature.getInstance().renderExtraLines(player, matrices, vertexConsumers, light);
    }
}
