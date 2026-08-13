/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Strict projection of the 15-slot Botany Pots inventory list. */
public record BotanyInventoryProjection(boolean valid, Map<Integer, ItemProjection> slots) {

    public static final int MAXIMUM_ENTRIES = 15;
    public static final int MAXIMUM_SLOT = 14;

    public BotanyInventoryProjection {
        Objects.requireNonNull(slots, "slots");
        slots = Map.copyOf(slots);
        if (slots.size() > MAXIMUM_ENTRIES
                || slots.keySet().stream().anyMatch(slot -> slot < 0 || slot > MAXIMUM_SLOT)) {
            throw new IllegalArgumentException("inventory projection outside supported bounds");
        }
    }

    public static BotanyInventoryProjection invalid() {
        return new BotanyInventoryProjection(false, Map.of());
    }

    public Optional<ItemProjection> slot(int slot) {
        return Optional.ofNullable(slots.get(slot));
    }
}
