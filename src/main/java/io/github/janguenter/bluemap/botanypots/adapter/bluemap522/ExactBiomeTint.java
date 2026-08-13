/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.biome.GrassColorModifier;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;

/** Exact client radius-two horizontal integer tint blend and positional swamp branch. */
final class ExactBiomeTint {

    private static final Simplex2D SWAMP = new Simplex2D(2_345L);

    private ExactBiomeTint() {
    }

    static int resolve(
            BlockColorCalculator unblended,
            DisplayNode.Tint.Kind kind,
            BlockAccess real,
            BlockState state
    ) {
        VirtualBlockAccess sample = new VirtualBlockAccess(real, state);
        int originX = real.getX();
        int originY = real.getY();
        int originZ = real.getZ();
        int red = 0;
        int green = 0;
        int blue = 0;
        Color color = new Color();
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                sample.set(originX + xOffset, originY, originZ + zOffset);
                int value = unblended.getBlockColor(sample, state, color).straight().getInt();
                if (kind == DisplayNode.Tint.Kind.GRASS) {
                    value = grassModifier(sample, value);
                }
                red += value >>> 16 & 0xFF;
                green += value >>> 8 & 0xFF;
                blue += value & 0xFF;
            }
        }
        return 0xFF000000 | red / 25 << 16 | green / 25 << 8 | blue / 25;
    }

    private static int grassModifier(BlockAccess block, int color) {
        GrassColorModifier modifier = block.getBiome().getGrassColorModifier();
        if (modifier == GrassColorModifier.DARK_FOREST) {
            return ((color & 0xFEFEFE) + 0x28340A >> 1) | 0xFF000000;
        }
        if (modifier == GrassColorModifier.SWAMP) {
            return SWAMP.value(block.getX() * 0.0225D, block.getZ() * 0.0225D) < -0.1D
                    ? 0xFF4C763C : 0xFF6A7039;
        }
        return color | 0xFF000000;
    }

    /** Narrow deterministic legacy-seeded two-dimensional simplex oracle. */
    private static final class Simplex2D {

        private static final int[][] GRADIENT = {
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
                {0, 1}, {0, -1}, {0, 1}, {0, -1}
        };
        private static final double SQRT_THREE = Math.sqrt(3D);
        private static final double SKEW = 0.5D * (SQRT_THREE - 1D);
        private static final double UNSKEW = (3D - SQRT_THREE) / 6D;
        private final int[] permutation = new int[256];

        private Simplex2D(long seed) {
            LegacyRandom random = new LegacyRandom(seed);
            random.nextDouble();
            random.nextDouble();
            random.nextDouble();
            for (int index = 0; index < permutation.length; index++) {
                permutation[index] = index;
            }
            for (int index = 0; index < permutation.length; index++) {
                int selected = index + random.nextInt(256 - index);
                int value = permutation[index];
                permutation[index] = permutation[selected];
                permutation[selected] = value;
            }
        }

        private double value(double x, double y) {
            double skew = (x + y) * SKEW;
            int cellX = floor(x + skew);
            int cellY = floor(y + skew);
            double unskew = (cellX + cellY) * UNSKEW;
            double localX = x - (cellX - unskew);
            double localY = y - (cellY - unskew);
            int stepX = localX > localY ? 1 : 0;
            int stepY = localX > localY ? 0 : 1;
            double secondX = localX - stepX + UNSKEW;
            double secondY = localY - stepY + UNSKEW;
            double thirdX = localX - 1D + 2D * UNSKEW;
            double thirdY = localY - 1D + 2D * UNSKEW;
            int xIndex = cellX & 0xFF;
            int yIndex = cellY & 0xFF;
            int firstGradient = p(xIndex + p(yIndex)) % 12;
            int secondGradient = p(xIndex + stepX + p(yIndex + stepY)) % 12;
            int thirdGradient = p(xIndex + 1 + p(yIndex + 1)) % 12;
            return 70D * (corner(firstGradient, localX, localY)
                    + corner(secondGradient, secondX, secondY)
                    + corner(thirdGradient, thirdX, thirdY));
        }

        private int p(int index) {
            return permutation[index & 0xFF];
        }

        private static double corner(int gradient, double x, double y) {
            double attenuation = 0.5D - x * x - y * y;
            if (attenuation < 0D) {
                return 0D;
            }
            attenuation *= attenuation;
            int[] vector = GRADIENT[gradient];
            return attenuation * attenuation * (vector[0] * x + vector[1] * y);
        }

        private static int floor(double value) {
            int truncated = (int) value;
            return value < truncated ? truncated - 1 : truncated;
        }
    }

    private static final class LegacyRandom {

        private static final long MASK = (1L << 48) - 1L;
        private long seed;

        private LegacyRandom(long value) {
            seed = (value ^ 0x5DEECE66DL) & MASK;
        }

        private int next(int bits) {
            seed = seed * 0x5DEECE66DL + 0xBL & MASK;
            return (int) (seed >>> 48 - bits);
        }

        private double nextDouble() {
            return (((long) next(26) << 27) + next(27)) * 0x1.0p-53;
        }

        private int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be positive");
            }
            if ((bound & -bound) == bound) {
                return (int) (bound * (long) next(31) >> 31);
            }
            int bits;
            int value;
            do {
                bits = next(31);
                value = bits % bound;
            } while (bits - value + bound - 1 < 0);
            return value;
        }
    }
}
