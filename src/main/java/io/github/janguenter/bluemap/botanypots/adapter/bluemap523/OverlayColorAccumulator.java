/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.math.Color;

/** Transactional low-resolution color accumulator for soil and crop overlays. */
final class OverlayColorAccumulator {

    private final Color destination;
    private final Color working;
    private final RenderSettings renderSettings;
    private float maximumOpacity;

    OverlayColorAccumulator(Color destination, RenderSettings renderSettings) {
        this.destination = destination;
        this.working = new Color().set(destination).premultiplied();
        this.renderSettings = renderSettings;
        this.maximumOpacity = destination.a;
    }

    void add(Texture texture, int tintArgb, int sunlight, int blocklight) {
        Color sample = new Color().set(texture.getColorPremultiplied());
        float tintAlpha = ((tintArgb >>> 24) & 0xFF) / 255F;
        float tintRed = ((tintArgb >>> 16) & 0xFF) / 255F;
        float tintGreen = ((tintArgb >>> 8) & 0xFF) / 255F;
        float tintBlue = (tintArgb & 0xFF) / 255F;
        sample.r *= tintRed * tintAlpha;
        sample.g *= tintGreen * tintAlpha;
        sample.b *= tintBlue * tintAlpha;
        sample.a *= tintAlpha;

        float light = Math.max(sunlight, blocklight) / 15F;
        light = (1F - renderSettings.getAmbientLight()) * light
                + renderSettings.getAmbientLight();
        sample.r *= light;
        sample.g *= light;
        sample.b *= light;
        maximumOpacity = Math.max(maximumOpacity, sample.a);
        working.add(sample);
    }

    void commit() {
        destination.set(working);
        if (destination.a > 0F) {
            destination.flatten().straight();
            destination.a = maximumOpacity;
        }
    }

    Checkpoint checkpoint() {
        return new Checkpoint(new Color().set(working), maximumOpacity);
    }

    void restore(Checkpoint checkpoint) {
        working.set(checkpoint.color());
        maximumOpacity = checkpoint.maximumOpacity();
    }

    record Checkpoint(Color color, float maximumOpacity) {
    }
}
