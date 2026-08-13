/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.model;

import java.util.Objects;

/** Bounded stable-optics projection of one vanilla ItemStack. */
public record ItemProjection(ResourceId itemId, int count, boolean hasComponents) {

    public ItemProjection {
        Objects.requireNonNull(itemId, "itemId");
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("item count outside supported bounds");
        }
    }
}
