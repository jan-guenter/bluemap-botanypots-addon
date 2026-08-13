/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.util.Direction;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UvLockAndCullfaceTest {

    @Test
    void exactSculkDownVariantRotatesAndUvLocksBothModelPlanes() {
        Variant sculkDown = new Variant(
                new ResourcePath<Model>("minecraft:block/sculk_vein"),
                90F, 0F, 0F, true, 1D);

        assertEquals(Direction.DOWN,
                VirtualDisplayModelEmitter.transformedDirection(
                        Direction.NORTH, sculkDown));
        assertEquals(Direction.UP,
                VirtualDisplayModelEmitter.transformedDirection(
                        Direction.SOUTH, sculkDown));
        assertEquals(Math.PI,
                Math.abs(VirtualDisplayModelEmitter.uvLockRotation(
                        Direction.NORTH, sculkDown)), 0.00001D);
        assertEquals(0D,
                VirtualDisplayModelEmitter.uvLockRotation(
                        Direction.SOUTH, sculkDown), 0.00001D);
    }

    @Test
    void exactRotatedNetherrackCullGroupIsFilteredAfterVariantTransform() {
        Variant netherrackSeed42 = new Variant(
                new ResourcePath<Model>("minecraft:block/netherrack"),
                270F, 90F, 0F, false, 1D);
        List<Direction> rawUpGroup = Arrays.stream(Direction.values())
                .filter(direction -> VirtualDisplayModelEmitter.transformedDirection(
                        direction, netherrackSeed42) == Direction.UP)
                .toList();

        assertEquals(1, rawUpGroup.size());
        Face included = face(rawUpGroup.getFirst());
        assertTrue(VirtualDisplayModelEmitter.included(
                Set.of(DisplayNode.Face.UP), included, netherrackSeed42));
        Direction another = Arrays.stream(Direction.values())
                .filter(direction -> direction != rawUpGroup.getFirst())
                .findFirst().orElseThrow();
        assertFalse(VirtualDisplayModelEmitter.included(
                Set.of(DisplayNode.Face.UP), face(another), netherrackSeed42));
    }

    private static Face face(Direction cullface) {
        return new Face(
                new Vector4f(0F, 0F, 16F, 16F),
                new TextureVariable(new ResourcePath<>("minecraft:block/netherrack")),
                cullface
        );
    }
}
