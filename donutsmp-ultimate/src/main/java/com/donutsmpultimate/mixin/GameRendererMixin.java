package com.donutsmpultimate.mixin;

import com.donutsmpultimate.feature.ZoomFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyReturnValue;

/**
 * Multiplies the returned FOV value when zoom is active.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(
        method = "getFov",
        at = @At("RETURN")
    )
    private double onGetFov(double original) {
        ZoomFeature zoom = ZoomFeature.getInstance();
        if (zoom.isZooming()) {
            return original * zoom.getFovMultiplier();
        }
        return original;
    }
}
