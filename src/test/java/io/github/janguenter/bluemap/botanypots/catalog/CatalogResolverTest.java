/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.model.BotanySnapshot;
import io.github.janguenter.bluemap.botanypots.model.ItemProjection;
import io.github.janguenter.bluemap.botanypots.model.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogResolverTest {

    private static final ResourceId DIRT = ResourceId.parse("minecraft:dirt");
    private static final ResourceId SAND = ResourceId.parse("minecraft:sand");
    private static final ResourceId WHEAT = ResourceId.parse("minecraft:wheat_seeds");
    private static final ResourceId CACTUS = ResourceId.parse("minecraft:cactus");

    @Test
    void onlyTheTwoExactRepresentativePairsResolve() {
        NormalizedCatalog catalog = representativeCatalog();

        RenderSelection wheat = new CatalogResolver().resolve(snapshot(DIRT, WHEAT), catalog);
        assertTrue(wheat.hasSoil());
        assertTrue(wheat.hasCrop());

        RenderSelection cactus = new CatalogResolver().resolve(snapshot(SAND, CACTUS), catalog);
        assertTrue(cactus.hasSoil());
        assertTrue(cactus.hasCrop());
    }

    @Test
    void unknownEmptyAndCrossedContentsRemainShellOnly() {
        NormalizedCatalog catalog = representativeCatalog();

        assertShellOnly(new CatalogResolver().resolve(
                snapshot(DIRT, ResourceId.parse("test:unknown_seed")), catalog));
        assertShellOnly(new CatalogResolver().resolve(snapshot(DIRT, CACTUS), catalog));
        assertShellOnly(new CatalogResolver().resolve(snapshot(SAND, WHEAT), catalog));
        assertShellOnly(new CatalogResolver().resolve(
                new BotanySnapshot(true, new ItemProjection(DIRT, 1, false), null), catalog));
    }

    @Test
    void duplicateInputMappingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NormalizedCatalog(List.of(
                entry(CatalogEntry.Kind.SOIL, "test:first", Set.of(DIRT), Set.of()),
                entry(CatalogEntry.Kind.SOIL, "test:second", Set.of(DIRT), Set.of()))));
    }

    private static NormalizedCatalog representativeCatalog() {
        return new NormalizedCatalog(List.of(
                entry(CatalogEntry.Kind.SOIL, "test:dirt", Set.of(DIRT), Set.of()),
                entry(CatalogEntry.Kind.SOIL, "test:sand", Set.of(SAND), Set.of()),
                entry(CatalogEntry.Kind.CROP, "test:wheat", Set.of(WHEAT), Set.of(DIRT)),
                entry(CatalogEntry.Kind.CROP, "test:cactus", Set.of(CACTUS), Set.of(SAND))));
    }

    private static void assertShellOnly(RenderSelection selection) {
        assertFalse(selection.hasSoil());
        assertFalse(selection.hasCrop());
    }

    private static BotanySnapshot snapshot(ResourceId soil, ResourceId seed) {
        return new BotanySnapshot(true,
                new ItemProjection(soil, 1, false), new ItemProjection(seed, 1, false));
    }

    private static CatalogEntry entry(
            CatalogEntry.Kind kind,
            String id,
            Set<ResourceId> inputs,
            Set<ResourceId> accepted
    ) {
        DisplayNode node = new DisplayNode(
                "minecraft:dirt", null, null, null,
                Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.625, 0.625, 0.625),
                new DisplayNode.Vector3(0, 0, 0), List.of(),
                DisplayNode.Strategy.RESOURCE);
        return new CatalogEntry(kind, ResourceId.parse(id), "core", "test", "test",
                inputs, accepted, new DisplayProgram(DisplayProgram.Kind.SIMPLE, List.of(node)));
    }
}
