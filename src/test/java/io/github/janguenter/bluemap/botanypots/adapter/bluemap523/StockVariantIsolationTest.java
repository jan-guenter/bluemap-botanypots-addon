/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.util.math.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class StockVariantIsolationTest {

    @Test
    void prefixAndEarlierVariantCannotBeTransformedByLaterVariant() {
        ArrayTileModel tile = new ArrayTileModel(4);
        addTriangle(tile, -2F);
        TileModelView target = new TileModelView(tile).initialize();
        Variant first = new Variant(new ResourcePath<Model>("test:first"));
        Variant second = new Variant(new ResourcePath<Model>("test:second"));
        var emitter = (BotanyPotsRenderer.StockEmitter) (block, variant, view, color) -> {
            addTriangle(view, variant == first ? 1F : 4F);
            if (variant == second) {
                view.translate(10F, 0F, 0F);
            }
        };

        BotanyPotsRenderer.renderStockVariant(
                null, first, target, new Color(), emitter);
        BotanyPotsRenderer.renderStockVariant(
                null, second, target, new Color(), emitter);

        float[] positions = positions(tile);
        assertArrayEquals(triangle(-2F), slice(positions, 0), 0F);
        assertArrayEquals(triangle(1F), slice(positions, 9), 0F);
        assertArrayEquals(triangle(14F), slice(positions, 18), 0F);
    }

    private static void addTriangle(ArrayTileModel tile, float x) {
        int face = tile.add(1);
        tile.setPositions(face, x, 0F, 0F, x + 1F, 0F, 0F, x, 1F, 0F);
    }

    private static void addTriangle(TileModelView view, float x) {
        int face = view.add(1);
        view.getTileModel().setPositions(
                face, x, 0F, 0F, x + 1F, 0F, 0F, x, 1F, 0F);
    }

    private static float[] triangle(float x) {
        return new float[]{x, 0F, 0F, x + 1F, 0F, 0F, x, 1F, 0F};
    }

    private static float[] slice(float[] values, int start) {
        return java.util.Arrays.copyOfRange(values, start, start + 9);
    }

    private static float[] positions(ArrayTileModel tile) {
        try {
            var field = ArrayTileModel.class.getDeclaredField("position");
            field.setAccessible(true);
            return ((float[]) field.get(tile)).clone();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
