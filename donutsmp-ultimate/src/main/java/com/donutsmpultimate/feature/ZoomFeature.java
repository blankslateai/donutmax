package com.donutsmpultimate.feature;

import com.donutsmpultimate.config.DonutConfig;

/**
 * Toolkit: Zoom
 * Reduces the FOV divisor while the zoom key is held.
 * Applied in GameRendererMixin.getFov().
 */
public class ZoomFeature {

    private static final ZoomFeature INSTANCE = new ZoomFeature();
    public static ZoomFeature getInstance() { return INSTANCE; }

    private boolean zoomKeyHeld = false;
    private float   currentZoom = 1.0f;

    public void setZoomHeld(boolean held) {
        this.zoomKeyHeld = held;
    }

    public boolean isZooming() {
        return zoomKeyHeld && DonutConfig.getInstance().enableZoom;
    }

    /** Called every tick to smoothly interpolate zoom level. */
    public void tick() {
        float target = zoomKeyHeld && DonutConfig.getInstance().enableZoom
                ? DonutConfig.getInstance().zoomFovDivisor
                : 1.0f;
        // Smooth step towards target
        currentZoom += (target - currentZoom) * 0.3f;
    }

    /**
     * Returns the multiplier to divide the FOV by.
     * When not zooming, returns 1.0 (no change).
     */
    public float getFovMultiplier() {
        return 1.0f / currentZoom;
    }
}
