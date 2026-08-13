/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.model;

/** Converts a validated inventory DTO into the deliberately narrow visual view. */
public final class BotanySnapshotDecoder {

    public BotanySnapshot decode(BotanyInventoryProjection inventory) {
        if (inventory == null || !inventory.valid()) {
            return BotanySnapshot.invalid();
        }
        ItemProjection soil = inventory.slot(0).orElse(null);
        ItemProjection seed = inventory.slot(1).orElse(null);
        if (soil == null || soil.hasComponents()
                || seed != null && seed.hasComponents()) {
            return BotanySnapshot.invalid();
        }
        return new BotanySnapshot(true, soil, seed);
    }
}
