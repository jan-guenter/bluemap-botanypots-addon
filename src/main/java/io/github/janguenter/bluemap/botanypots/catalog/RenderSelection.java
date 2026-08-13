/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

/** Resolved overlays; crop members are always treated as one atomic sequence. */
public record RenderSelection(DisplayProgram soil, DisplayProgram crop) {

    public static RenderSelection shellOnly() {
        return new RenderSelection(null, null);
    }

    public boolean hasSoil() {
        return soil != null;
    }

    public boolean hasCrop() {
        return crop != null;
    }
}
