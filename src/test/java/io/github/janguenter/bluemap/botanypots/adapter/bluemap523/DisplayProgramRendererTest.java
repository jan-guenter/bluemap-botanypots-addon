/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayProgram;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayProgramRendererTest {

    @Test
    void structurallyValidAllHiddenFaceCountsAsSuccessfulNode() {
        VirtualDisplayModelEmitter.EmissionEvidence evidence =
                new VirtualDisplayModelEmitter.EmissionEvidence();
        evidence.markValidFace();

        assertTrue(evidence.successful(false));
    }

    @Test
    void hiddenLaterMemberDoesNotRollbackVisibleEarlierMember() {
        DisplayNode visible = node("test:visible");
        DisplayNode hidden = node("test:hidden");
        DisplayProgramRenderer renderer = new DisplayProgramRenderer(
                (node, block, view, color, outerYDegrees, outerYScale) -> {
                    if (node.blockId().value().equals("test:visible")) {
                        view.add(2);
                    }
                    // The second node models a structurally valid face filtered
                    // by top-only/cave policy: success with no emitted geometry.
                    return true;
                });
        ArrayTileModel tile = new ArrayTileModel(4);

        assertTrue(renderer.renderCrop(
                new DisplayProgram(DisplayProgram.Kind.SIMPLE, List.of(visible, hidden)),
                null, tile, null));
        assertEquals(2, tile.size());
    }

    @Test
    void nonUniformScaleOffsetAndOrderedRotationsMatchPoseStackReplay() {
        DisplayNode transformed = new DisplayNode(
                "test:transform", null, null, null,
                Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.5D, 0.75D, 0.25D),
                new DisplayNode.Vector3(0.2D, -0.1D, 0.4D),
                List.of(
                        new DisplayNode.Rotation(DisplayNode.Axis.X, 90D),
                        new DisplayNode.Rotation(DisplayNode.Axis.Y, 270D)
                ),
                DisplayNode.Strategy.RESOURCE
        );
        ArrayTileModel tile = new ArrayTileModel(2);
        int face = tile.add(1);
        tile.setPositions(face,
                0.1F, 0.2F, 0.3F,
                0.4F, 0.5F, 0.6F,
                0.7F, 0.8F, 0.9F);
        DisplayProgramRenderer.applyDisplayTransform(
                transformed, 0.3984375D,
                new de.bluecolored.bluemap.core.map.hires.TileModelView(tile)
                        .initialize(tile, 0)
        );

        float[] actual = positions(tile);
        double[][] source = {
                {0.1D, 0.2D, 0.3D},
                {0.4D, 0.5D, 0.6D},
                {0.7D, 0.8D, 0.9D},
        };
        for (int vertex = 0; vertex < source.length; vertex++) {
            double[] expected = upstream(
                    source[vertex], transformed, 0.3984375D, 1D, 0D
            );
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(expected[axis], actual[vertex * 3 + axis], 0.00001D);
            }
        }
    }

    @Test
    void soilWrapperMatchesEveryFacingAroundExactYScale() {
        DisplayNode transformed = new DisplayNode(
                "test:soil", null, null, null,
                Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.5D, 0.75D, 0.25D),
                new DisplayNode.Vector3(0.2D, -0.1D, 0.4D),
                List.of(new DisplayNode.Rotation(DisplayNode.Axis.X, 90D)),
                DisplayNode.Strategy.RESOURCE
        );
        for (double facing : List.of(0D, 90D, 180D, 270D)) {
            ArrayTileModel tile = new ArrayTileModel(2);
            int face = tile.add(1);
            tile.setPositions(face,
                    0.1F, 0.2F, 0.3F,
                    0.4F, 0.5F, 0.6F,
                    0.7F, 0.8F, 0.9F);
            var view = new de.bluecolored.bluemap.core.map.hires.TileModelView(tile)
                    .initialize(tile, 0);
            DisplayProgramRenderer.applyDisplayTransform(transformed, 0D, view);
            view.scale(1F, 0.6375F, 1F);
            DisplayProgramRenderer.applyAxisRotation(
                    view, DisplayNode.Axis.Y, facing);
            float[] actual = positions(tile);
            double[][] source = {
                    {0.1D, 0.2D, 0.3D},
                    {0.4D, 0.5D, 0.6D},
                    {0.7D, 0.8D, 0.9D},
            };
            for (int vertex = 0; vertex < source.length; vertex++) {
                double[] expected = upstream(
                        source[vertex], transformed, 0D, 0.6375D, facing
                );
                for (int axis = 0; axis < 3; axis++) {
                    assertEquals(
                            expected[axis], actual[vertex * 3 + axis], 0.00001D,
                            "facing=" + facing + " vertex=" + vertex + " axis=" + axis
                    );
                }
            }
        }
    }

    private static double[] upstream(
            double[] point,
            DisplayNode node,
            double height,
            double soilYScale,
            double facing
    ) {
        double[] value = point.clone();
        for (int index = node.rotations().size() - 1; index >= 0; index--) {
            DisplayNode.Rotation rotation = node.rotations().get(index);
            value = rotateAroundOrigin(value, rotation.axis(), rotation.degrees());
        }
        value[0] *= node.scale().x();
        value[1] *= node.scale().y();
        value[2] *= node.scale().z();
        value[0] += 0.5D - node.scale().x() / 2D
                + node.offset().x() * node.scale().x();
        value[1] += height + node.offset().y() * node.scale().y();
        value[2] += 0.5D - node.scale().z() / 2D
                + node.offset().z() * node.scale().z();
        value[1] *= soilYScale;
        return rotateAroundOrigin(value, DisplayNode.Axis.Y, facing);
    }

    private static double[] rotateAroundOrigin(
            double[] point, DisplayNode.Axis axis, double degrees
    ) {
        int turns = Math.floorMod((int) Math.round(degrees / 90D), 4);
        double[] value = point.clone();
        double[][] offsets = switch (axis) {
            case X -> new double[][]{
                    {0D, 0D, 0D}, {0D, 0D, -1D},
                    {0D, -1D, -1D}, {0D, -1D, 0D}
            };
            case Y -> new double[][]{
                    {0D, 0D, 0D}, {-1D, 0D, 0D},
                    {-1D, 0D, -1D}, {0D, 0D, -1D}
            };
            case Z -> new double[][]{
                    {0D, 0D, 0D}, {0D, -1D, 0D},
                    {-1D, -1D, 0D}, {-1D, 0D, 0D}
            };
        };
        value[0] += offsets[turns][0];
        value[1] += offsets[turns][1];
        value[2] += offsets[turns][2];
        for (int turn = 0; turn < turns; turn++) {
            value = switch (axis) {
                case X -> new double[]{value[0], -value[2], value[1]};
                case Y -> new double[]{value[2], value[1], -value[0]};
                case Z -> new double[]{-value[1], value[0], value[2]};
            };
        }
        return value;
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

    private static DisplayNode node(String block) {
        return new DisplayNode(
                block, null, null, null,
                Set.of(DisplayNode.Face.UP),
                new DisplayNode.Vector3(0.625D, 0.625D, 0.625D),
                new DisplayNode.Vector3(0D, 0D, 0D),
                List.of(), DisplayNode.Strategy.RESOURCE);
    }
}
