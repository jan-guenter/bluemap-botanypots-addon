/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.model;

/** Stable visual inputs; both supported pair members are required for an overlay. */
public record BotanySnapshot(boolean valid, ItemProjection soil, ItemProjection seed) {

    public static BotanySnapshot invalid() {
        return new BotanySnapshot(false, null, null);
    }
}
