/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Multipart;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Exact LegacyRandomSource/WeightedBakedModel selection for renderer seed 42. */
final class FixedVariantSelector {

    static final long SEED = 42L;

    private FixedVariantSelector() {
    }

    static List<Variant> select(
            de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState resource,
            BlockState state
    ) {
        if (resource == null) {
            return List.of();
        }
        List<Variant> selected = new ArrayList<>();
        Variants variants = resource.getVariants();
        if (variants != null) {
            boolean matched = false;
            for (VariantSet candidate : variants.getVariants()) {
                if (candidate.getCondition().matches(state)) {
                    selected.add(select(candidate, SEED));
                    matched = true;
                    break;
                }
            }
            if (!matched && variants.getDefaultVariant() != null) {
                selected.add(select(variants.getDefaultVariant(), SEED));
            }
        }
        Multipart multipart = resource.getMultipart();
        if (multipart != null) {
            long multipartSeed = nextLong(SEED);
            for (VariantSet part : multipart.getParts()) {
                if (part.getCondition().matches(state)) {
                    selected.add(select(part, multipartSeed));
                }
            }
        }
        return List.copyOf(selected);
    }

    private static Variant select(VariantSet set, long seed) {
        Variant[] variants = set.getVariants();
        if (variants.length == 0) {
            throw new IllegalArgumentException("empty weighted variant set");
        }
        int total = 0;
        int[] weights = new int[variants.length];
        for (int index = 0; index < variants.length; index++) {
            double weight = variants[index].getWeight();
            if (!Double.isFinite(weight) || weight <= 0D || weight != Math.rint(weight)
                    || weight > Integer.MAX_VALUE - total) {
                throw new IllegalArgumentException("invalid weighted variant");
            }
            weights[index] = (int) weight;
            total += weights[index];
        }
        int random = (int) nextLong(seed);
        int selected = Math.floorMod(random == Integer.MIN_VALUE ? 0 : Math.abs(random), total);
        for (int index = 0; index < variants.length; index++) {
            selected -= weights[index];
            if (selected < 0) {
                return variants[index];
            }
        }
        return variants[variants.length - 1];
    }

    static long nextLong(long externalSeed) {
        long scrambled = (externalSeed ^ 0x5DEECE66DL) & ((1L << 48) - 1L);
        long first = (scrambled * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1L);
        long second = (first * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1L);
        return ((long) ((int) (first >>> 16)) << 32) + (int) (second >>> 16);
    }
}
