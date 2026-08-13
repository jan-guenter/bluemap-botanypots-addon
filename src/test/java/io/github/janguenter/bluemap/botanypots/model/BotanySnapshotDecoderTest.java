/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotanySnapshotDecoderTest {

    private final BotanySnapshotDecoder decoder = new BotanySnapshotDecoder();

    @Test
    void decodesOnlyStableVisualSlots() {
        ItemProjection soil = item("minecraft:dirt", false);
        ItemProjection seed = item("minecraft:wheat_seeds", false);
        BotanySnapshot result = decoder.decode(new BotanyInventoryProjection(
                true, Map.of(0, soil, 1, seed, 14, item("minecraft:diamond", true))));

        assertTrue(result.valid());
        assertEquals(soil, result.soil());
        assertEquals(seed, result.seed());
    }

    @Test
    void componentBearingVisualInputsFailClosed() {
        assertFalse(decoder.decode(new BotanyInventoryProjection(true, Map.of(
                0, item("minecraft:dirt", true)))).valid());
        assertFalse(decoder.decode(new BotanyInventoryProjection(true, Map.of(
                0, item("minecraft:dirt", false),
                1, item("minecraft:wheat_seeds", true)))).valid());
    }

    @Test
    void missingSoilAndInvalidDtoFailClosed() {
        assertFalse(decoder.decode(BotanyInventoryProjection.invalid()).valid());
        assertFalse(decoder.decode(new BotanyInventoryProjection(true, Map.of(
                1, item("minecraft:wheat_seeds", false)))).valid());
    }

    private static ItemProjection item(String id, boolean components) {
        return new ItemProjection(ResourceId.parse(id), 1, components);
    }
}
