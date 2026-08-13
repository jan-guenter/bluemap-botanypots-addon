/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.profile;

import io.github.janguenter.bluemap.botanypots.catalog.CatalogEntry;
import io.github.janguenter.bluemap.botanypots.catalog.NormalizedCatalog;
import io.github.janguenter.bluemap.botanypots.catalog.TsvCatalogSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactProfileCensusTest {

    @Test
    void routeAllowlistIsExactAndExhaustive() throws Exception {
        RouteCatalog routes = RouteCatalog.load();

        assertEquals(183, routes.routes().size());
        assertTrue(routes.contains("botanypots:black_concrete_botany_pot"));
        assertTrue(routes.contains("botanypots:yellow_terracotta_waxed_botany_pot"));
        assertFalse(routes.contains("minecraft:flower_pot"));
        assertFalse(routes.contains("botanypots:not_a_real_botany_pot"));
        assertEquals(23_424, routes.routes().size() * 128);

        int states = 0;
        int waterloggedStates = 0;
        for (var route : routes.routes()) {
            for (boolean waterlogged : java.util.List.of(false, true)) {
                for (int level = 0; level < 16; level++) {
                    for (String facing : java.util.List.of(
                            "south", "east", "north", "west")) {
                        var state = de.bluecolored.bluemap.core.world.BlockState.fromString(
                                route.value() + "[facing=" + facing + ",level=" + level
                                        + ",waterlogged=" + waterlogged + "]");
                        states++;
                        if (state.isWaterlogged()) {
                            waterloggedStates++;
                        }
                    }
                }
            }
        }
        assertEquals(23_424, states);
        assertEquals(11_712, waterloggedStates);
    }

    @Test
    void checkedInRepresentativeCatalogContainsOnlyTwoExactPairs() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/bluemap-botanypots/profiles/atmons-1.2.0/catalog.tsv")) {
            NormalizedCatalog catalog = new TsvCatalogSource().read(input);

            assertEquals(4, catalog.entries().size());
            assertEquals(2, catalog.count(entry -> entry.kind() == CatalogEntry.Kind.CROP));
            assertEquals(2, catalog.count(entry -> entry.kind() == CatalogEntry.Kind.SOIL));
            assertEquals(0,
                    catalog.count(entry -> entry.kind() == CatalogEntry.Kind.DEFAULT_SOIL));
            assertEquals(4, catalog.entries().stream()
                    .mapToInt(entry -> entry.display().members().size()).sum());
            assertEquals(4, catalog.entries().stream()
                    .flatMap(entry -> entry.display().members().stream())
                    .map(node -> node.blockId().value()).distinct().count());
        }
    }

    @Test
    void onlyTheExactCoreArtifactIsAnActivationAuthority() {
        assertEquals(java.util.Set.of("botanypots"), ExactArtifacts.RUNTIME.keySet());
        assertEquals(java.util.Set.of("botanypots"), ExactArtifacts.FILE_NAMES.keySet());
        assertEquals(java.util.Set.of("botanypots"), ExactArtifacts.CORE_PROFILE);
    }
}
