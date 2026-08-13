/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.util.Objects;
import java.util.Set;

/** One bundled representative soil or crop display mapping. */
public record CatalogEntry(
        Kind kind,
        ResourceId recipeId,
        String profile,
        String source,
        String derivation,
        Set<ResourceId> inputItems,
        Set<ResourceId> acceptedSoils,
        DisplayProgram display
) {

    public CatalogEntry {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(recipeId, "recipeId");
        profile = wire(profile, "profile");
        source = wire(source, "source");
        derivation = wire(derivation, "derivation");
        Objects.requireNonNull(inputItems, "inputItems");
        Objects.requireNonNull(acceptedSoils, "acceptedSoils");
        Objects.requireNonNull(display, "display");
        inputItems = Set.copyOf(inputItems);
        acceptedSoils = Set.copyOf(acceptedSoils);
        if (inputItems.isEmpty() || inputItems.size() > 4_096
                || kind != Kind.CROP && !acceptedSoils.isEmpty()
                || kind == Kind.DEFAULT_SOIL && inputItems.size() != 1
                || acceptedSoils.size() > 4_096) {
            throw new IllegalArgumentException("catalog entry outside supported bounds");
        }
    }

    private static String wire(String value, String label) {
        Objects.requireNonNull(value, label);
        if (!value.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("invalid " + label);
        }
        return value;
    }

    public enum Kind {
        SOIL,
        DEFAULT_SOIL,
        CROP
    }
}
