/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.util.Direction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FluidBoxUvTest {

    @Test
    void everyFaceUsesExactBookshelfCoordinateToUvMapping() {
        Map<Direction, float[][]> expected = Map.of(
                Direction.DOWN, new float[][]{
                        {0, 0, 1, 0, 1}, {0, 0, 0, 0, 0},
                        {1, 0, 0, 1, 0}, {1, 0, 1, 1, 1}},
                Direction.UP, new float[][]{
                        {0, 1, 1, 0, 1}, {1, 1, 1, 1, 1},
                        {1, 1, 0, 1, 0}, {0, 1, 0, 0, 0}},
                Direction.NORTH, new float[][]{
                        {0, 0, 0, 0, 0}, {0, 1, 0, 0, 1},
                        {1, 1, 0, 1, 1}, {1, 0, 0, 1, 0}},
                Direction.SOUTH, new float[][]{
                        {1, 0, 1, 1, 0}, {1, 1, 1, 1, 1},
                        {0, 1, 1, 0, 1}, {0, 0, 1, 0, 0}},
                Direction.WEST, new float[][]{
                        {0, 0, 1, 0, 1}, {0, 1, 1, 1, 1},
                        {0, 1, 0, 1, 0}, {0, 0, 0, 0, 0}},
                Direction.EAST, new float[][]{
                        {1, 0, 0, 0, 0}, {1, 1, 0, 1, 0},
                        {1, 1, 1, 1, 1}, {1, 0, 1, 0, 1}}
        );
        for (Direction face : Direction.values()) {
            IsolatedQuadEmitter.FluidVertex[] actual =
                    IsolatedQuadEmitter.fluidVertices(face);
            float[][] oracle = expected.get(face);
            for (int index = 0; index < 4; index++) {
                assertArrayEquals(oracle[index], values(actual[index]), 0F,
                        face + " vertex " + index);
            }
        }
    }

    private static float[] values(IsolatedQuadEmitter.FluidVertex vertex) {
        return new float[]{
                vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v()
        };
    }
}
