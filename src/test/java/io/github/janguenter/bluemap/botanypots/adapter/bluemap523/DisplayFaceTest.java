/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import com.flowpowered.math.vector.Vector3f;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Rotation;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.Axis;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayFaceTest {

    @Test
    void nonUniformDisplayScaleUsesInverseTransposeForAngledFace() {
        DisplayNode node = node(new DisplayNode.Vector3(0.4D, 0.625D, 0.4D));
        Element angled = new Element(
                Vector3f.ZERO,
                new Vector3f(16F, 16F, 16F),
                new Rotation(new Vector3f(8F, 8F, 8F), Axis.Z, 22.5F, false),
                Map.of());
        DisplayFace.Normal normal = DisplayFace.transformedNormal(
                node, Direction.UP, angled, identity(), 0D, 1D);

        assertEquals(0.543D, Math.abs(normal.x()), 0.002D);
        assertEquals(0.840D, normal.y(), 0.002D);
        assertEquals(1, Math.abs(Math.round(normal.x())));
        assertEquals(1, Math.round(normal.y()));
    }

    @Test
    void soilOuterScaleParticipatesInNormalTransformBeforeFacingRotation() {
        DisplayNode node = node(new DisplayNode.Vector3(0.625D, 0.625D, 0.625D));
        Element angled = new Element(
                Vector3f.ZERO,
                new Vector3f(16F, 16F, 16F),
                new Rotation(new Vector3f(8F, 8F, 8F), Axis.Z, 22.5F, false),
                Map.of());
        DisplayFace.Normal normal = DisplayFace.transformedNormal(
                node, Direction.UP, angled, identity(), 90D, 0.6375D);

        assertTrue(normal.y() > 0.95F);
        assertTrue(Math.abs(normal.z()) > 0.25F);
        assertEquals(0, Math.round(normal.x()));
    }

    private static DisplayNode node(DisplayNode.Vector3 scale) {
        return new DisplayNode(
                "minecraft:spore_blossom", null, null, null,
                Set.of(DisplayNode.Face.UP), scale,
                new DisplayNode.Vector3(0D, 0D, 0D), List.of(),
                DisplayNode.Strategy.RESOURCE);
    }

    private static Variant identity() {
        return new Variant(new ResourcePath<Model>("minecraft:block/spore_blossom"));
    }
}
