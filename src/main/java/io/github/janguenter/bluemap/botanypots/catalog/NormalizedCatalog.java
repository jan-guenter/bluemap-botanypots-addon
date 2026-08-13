/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Immutable, ambiguity-rejecting runtime catalog. */
public final class NormalizedCatalog {

    private final List<CatalogEntry> entries;
    private final Map<ResourceId, CatalogEntry> soils;
    private final Map<ResourceId, CatalogEntry> defaultSoils;
    private final Map<ResourceId, CatalogEntry> crops;

    public NormalizedCatalog(Collection<CatalogEntry> source) {
        Objects.requireNonNull(source, "source");
        if (source.size() > 2_048) {
            throw new IllegalArgumentException("catalog outside supported bounds");
        }
        List<CatalogEntry> ordered = new ArrayList<>(source);
        ordered.sort(Comparator.comparing(CatalogEntry::recipeId));
        if (ordered.stream().map(CatalogEntry::recipeId).distinct().count() != ordered.size()) {
            throw new IllegalArgumentException("duplicate recipe identifier");
        }
        entries = List.copyOf(ordered);
        soils = index(entries, CatalogEntry.Kind.SOIL);
        defaultSoils = index(entries, CatalogEntry.Kind.DEFAULT_SOIL);
        crops = index(entries, CatalogEntry.Kind.CROP);
    }

    public List<CatalogEntry> entries() {
        return entries;
    }

    public Optional<CatalogEntry> soil(ResourceId input) {
        return Optional.ofNullable(soils.get(input));
    }

    public Optional<CatalogEntry> crop(ResourceId input) {
        return Optional.ofNullable(crops.get(input));
    }

    public Optional<CatalogEntry> defaultSoil(ResourceId input) {
        return Optional.ofNullable(defaultSoils.get(input));
    }

    public NormalizedCatalog enabledProfiles(Set<String> enabled) {
        return new NormalizedCatalog(entries.stream()
                .filter(entry -> enabled.contains(entry.profile()))
                .toList());
    }

    public long count(Predicate<CatalogEntry> predicate) {
        return entries.stream().filter(predicate).count();
    }

    private static Map<ResourceId, CatalogEntry> index(
            List<CatalogEntry> entries,
            CatalogEntry.Kind kind
    ) {
        Map<ResourceId, CatalogEntry> index = new HashMap<>();
        for (CatalogEntry entry : entries) {
            if (entry.kind() != kind) {
                continue;
            }
            for (ResourceId input : entry.inputItems()) {
                if (index.putIfAbsent(input, entry) != null) {
                    throw new IllegalArgumentException(
                            "ambiguous " + kind.name().toLowerCase() + " input: " + input
                    );
                }
            }
        }
        return Map.copyOf(index);
    }
}
