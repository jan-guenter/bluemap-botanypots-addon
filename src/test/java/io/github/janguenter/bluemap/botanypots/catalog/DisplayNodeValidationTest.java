/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisplayNodeValidationTest {

    @Test
    void onlyQuarterTurnRotationsAreAccepted() {
        assertDoesNotThrow(() -> node(List.of(
                new DisplayNode.Rotation(DisplayNode.Axis.X, 180),
                new DisplayNode.Rotation(DisplayNode.Axis.Y, -270))));
        assertThrows(IllegalArgumentException.class,
                () -> new DisplayNode.Rotation(DisplayNode.Axis.Z, 45));
    }

    @Test
    void calculatedTintCanCarryAnOpaqueMultiplier() {
        assertDoesNotThrow(() -> new DisplayNode.Tint(null, Map.of(
                1, new DisplayNode.Tint.Rule(DisplayNode.Tint.Kind.GRASS, 0xFF999999))));
        assertThrows(IllegalArgumentException.class, () -> new DisplayNode.Tint.Rule(
                DisplayNode.Tint.Kind.GRASS, 0x80999999));
    }

    private static DisplayNode node(List<DisplayNode.Rotation> rotations) {
        return new DisplayNode("minecraft:dirt", null, null, null,
                Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.625, 0.5, 0.75),
                new DisplayNode.Vector3(0.125, -0.25, 0.5), rotations,
                DisplayNode.Strategy.RESOURCE);
    }
}
