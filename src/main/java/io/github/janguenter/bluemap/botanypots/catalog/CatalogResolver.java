/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.model.BotanySnapshot;

import java.util.Objects;
import java.util.function.Predicate;

/** Deterministic slot-to-catalog resolver implementing the stable visual fallback contract. */
public final class CatalogResolver {

    public RenderSelection resolve(BotanySnapshot snapshot, NormalizedCatalog catalog) {
        return resolve(snapshot, catalog, ignored -> true);
    }

    public RenderSelection resolve(
            BotanySnapshot snapshot,
            NormalizedCatalog catalog,
            Predicate<CatalogEntry> enabled
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(enabled, "enabled");
        if (snapshot == null || !snapshot.valid() || snapshot.seed() == null) {
            return RenderSelection.shellOnly();
        }
        CatalogEntry soil = catalog.soil(snapshot.soil().itemId()).orElse(null);
        CatalogEntry crop = catalog.crop(snapshot.seed().itemId()).orElse(null);
        if (soil == null || crop == null || !enabled.test(soil) || !enabled.test(crop)
                || !crop.acceptedSoils().contains(snapshot.soil().itemId())) {
            return RenderSelection.shellOnly();
        }
        return new RenderSelection(soil.display(), crop.display());
    }
}
