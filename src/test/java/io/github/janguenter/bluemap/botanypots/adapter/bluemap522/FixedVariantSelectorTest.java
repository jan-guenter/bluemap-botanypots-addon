/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockStateCondition;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Multipart;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedVariantSelectorTest {

    @Test
    void directWeightedModelUsesLegacyRandomSeedFortyTwo() {
        Variant first = variant("test:first", 1);
        Variant second = variant("test:second", 2);
        Variant third = variant("test:third", 3);
        BlockState resource = new BlockState(new Variants(
                new VariantSet[0], new VariantSet(first, second, third)));

        List<Variant> selected = FixedVariantSelector.select(
                resource, de.bluecolored.bluemap.core.world.BlockState.fromString("test:block"));
        assertEquals(List.of(second), selected);
    }

    @Test
    void multipartReseedsEachMatchedPartFromOneOuterNextLong() {
        Variant first = variant("test:first", 1);
        Variant second = variant("test:second", 2);
        Variant third = variant("test:third", 3);
        VariantSet left = new VariantSet(BlockStateCondition.all(), first, second, third);
        VariantSet right = new VariantSet(BlockStateCondition.all(), first, second, third);
        BlockState resource = new BlockState(new Multipart(new VariantSet[]{left, right}));

        List<Variant> selected = FixedVariantSelector.select(
                resource, de.bluecolored.bluemap.core.world.BlockState.fromString("test:block"));
        assertEquals(List.of(first, first), selected);
    }

    @Test
    void rejectsFractionalOrEmptyWeights() {
        Variant fractional = new Variant(
                new ResourcePath<Model>("test:fractional"), 0, 0, 0, false, 1.5);
        BlockState fractionalResource = new BlockState(new Variants(
                new VariantSet[0], new VariantSet(fractional)));
        BlockState emptyResource = new BlockState(new Variants(
                new VariantSet[0], new VariantSet()));
        var state = de.bluecolored.bluemap.core.world.BlockState.fromString("test:block");

        assertThrows(IllegalArgumentException.class,
                () -> FixedVariantSelector.select(fractionalResource, state));
        assertThrows(IllegalArgumentException.class,
                () -> FixedVariantSelector.select(emptyResource, state));
    }

    private static Variant variant(String model, int weight) {
        return new Variant(new ResourcePath<Model>(model), 0, 0, 0, false, weight);
    }
}
