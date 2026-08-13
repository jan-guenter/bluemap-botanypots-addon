/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import de.bluecolored.bluemap.core.util.math.Color;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayProgram;
import io.github.janguenter.bluemap.botanypots.catalog.RenderSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BotanyPotsRendererAtomicityTest {

    @Test
    void cropFailureRollsBackPairWithoutAttemptingSoil() {
        ArrayTileModel tile = new ArrayTileModel(4);
        tile.add(1);
        int stockSize = tile.size();
        Color mapColor = new Color().set(0.2F, 0.3F, 0.4F, 0.5F, false);
        Color stockColor = new Color().set(mapColor);
        AtomicInteger soilCalls = new AtomicInteger();
        DisplayProgramRenderer displays = new DisplayProgramRenderer(
                (node, block, view, color, outerYDegrees, outerYScale) -> {
                    view.add(1);
                    if (node.blockId().value().equals("test:crop")) {
                        return false;
                    }
                    soilCalls.incrementAndGet();
                    return true;
                });

        boolean rendered = BotanyPotsRenderer.renderOverlaysAtomically(
                selection(), null, tile, mapColor, null, displays
        );

        assertFalse(rendered);
        assertEquals(stockSize, tile.size());
        assertEquals(0, soilCalls.get());
        assertColor(stockColor, mapColor);
    }

    @Test
    void soilFailureRollsBackPreviouslySuccessfulCrop() {
        ArrayTileModel tile = new ArrayTileModel(4);
        tile.add(1);
        int stockSize = tile.size();
        Color mapColor = new Color().set(0.2F, 0.3F, 0.4F, 0.5F, false);
        Color stockColor = new Color().set(mapColor);
        DisplayProgramRenderer displays = new DisplayProgramRenderer(
                (node, block, view, color, outerYDegrees, outerYScale) -> {
                    view.add(1);
                    return !node.blockId().value().equals("test:soil");
                });

        boolean rendered = BotanyPotsRenderer.renderOverlaysAtomically(
                selection(), null, tile, mapColor, null, displays
        );

        assertFalse(rendered);
        assertEquals(stockSize, tile.size());
        assertColor(stockColor, mapColor);
    }

    private static RenderSelection selection() {
        return new RenderSelection(program("test:soil"), program("test:crop"));
    }

    private static DisplayProgram program(String block) {
        DisplayNode node = new DisplayNode(
                block, null, null, null, Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.625D, 0.625D, 0.625D),
                new DisplayNode.Vector3(0D, 0D, 0D),
                List.of(), DisplayNode.Strategy.RESOURCE
        );
        return new DisplayProgram(DisplayProgram.Kind.SIMPLE, List.of(node));
    }

    private static void assertColor(Color expected, Color actual) {
        assertEquals(expected.r, actual.r, 0F);
        assertEquals(expected.g, actual.g, 0F);
        assertEquals(expected.b, actual.b, 0F);
        assertEquals(expected.a, actual.a, 0F);
        assertEquals(expected.premultiplied, actual.premultiplied);
    }
}
