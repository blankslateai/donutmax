package com.donutsmpultimate.mixin;

import com.donutsmpultimate.feature.ZoomFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Multiplies the returned FOV value when zoom is active.
 * Uses @Inject + CallbackInfoReturnable instead of @ModifyReturnValue
 * for compatibility with the Mixin version bundled in Fabric Loader 0.16.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Double> cir) {
        ZoomFeature zoom = ZoomFeature.getInstance();
        if (zoom.isZooming()) {
            cir.setReturnValue(cir.getReturnValue() * zoom.getFovMultiplier());
        }
    }
}
