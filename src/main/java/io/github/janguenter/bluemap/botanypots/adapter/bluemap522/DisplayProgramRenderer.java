/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayProgram;

/** Applies the exact mature display stack, transform order, and pot-facing soil transform. */
final class DisplayProgramRenderer {

    private static final double CROP_BASELINE_Y = 0.3984375D;
    private static final double SOIL_Y_SCALE = 0.6375D;

    private final VirtualDisplayModelEmitter resourceEmitter;
    private final NodeEmitter testEmitter;

    DisplayProgramRenderer(VirtualDisplayModelEmitter resourceEmitter) {
        this.resourceEmitter = resourceEmitter;
        this.testEmitter = null;
    }

    DisplayProgramRenderer(NodeEmitter testEmitter) {
        this.resourceEmitter = null;
        this.testEmitter = testEmitter;
    }

    boolean renderSoil(
            DisplayProgram program,
            BlockNeighborhood block,
            TileModel tile,
            OverlayColorAccumulator color
    ) {
        int start = tile.size();
        double height = 0D;
        double outerYDegrees = facingDegrees(block);
        for (DisplayNode node : program.members()) {
            int nodeStart = tile.size();
            if (!emit(node, block, new TileModelView(tile), color,
                    outerYDegrees, SOIL_Y_SCALE)) {
                tile.reset(start);
                return false;
            }
            TileModelView view = new TileModelView(tile).initialize(tile, nodeStart);
            applyDisplayTransform(node, height, view);
            view.scale(1F, (float) SOIL_Y_SCALE, 1F);
            applyAxisRotation(view, DisplayNode.Axis.Y, outerYDegrees);
            height += node.offset().y() * node.scale().y() + node.scale().y();
        }
        return true;
    }

    boolean renderCrop(
            DisplayProgram program,
            BlockNeighborhood block,
            TileModel tile,
            OverlayColorAccumulator color
    ) {
        int start = tile.size();
        double height = CROP_BASELINE_Y;
        for (DisplayNode node : program.members()) {
            int nodeStart = tile.size();
            if (!emit(node, block, new TileModelView(tile), color, 0D, 1D)) {
                tile.reset(start);
                return false;
            }
            TileModelView view = new TileModelView(tile).initialize(tile, nodeStart);
            applyDisplayTransform(node, height, view);
            height += node.offset().y() * node.scale().y() + node.scale().y();
        }
        return true;
    }

    private boolean emit(
            DisplayNode node,
            BlockNeighborhood block,
            TileModelView view,
            OverlayColorAccumulator color,
            double outerYDegrees,
            double outerYScale
    ) {
        if (testEmitter != null) {
            return testEmitter.emit(
                    node, block, view, color, outerYDegrees, outerYScale);
        }
        return resourceEmitter.emit(
                node, block, view, color, outerYDegrees, outerYScale);
    }

    static void applyDisplayTransform(
            DisplayNode node,
            double height,
            TileModelView view
    ) {
        // TileModelView mutates vertices immediately, whereas PoseStack post-multiplies
        // its current matrix. Apply the exact upstream pose operations in reverse so
        // the resulting vertex matrix remains T(center) * T(offset) * S * R...T.
        for (int index = node.rotations().size() - 1; index >= 0; index--) {
            DisplayNode.Rotation rotation = node.rotations().get(index);
            applyAxisRotation(view, rotation.axis(), rotation.degrees());
        }
        float scaleX = (float) node.scale().x();
        float scaleY = (float) node.scale().y();
        float scaleZ = (float) node.scale().z();
        view.scale(scaleX, scaleY, scaleZ);
        view.translate(
                (float) (0.5D - scaleX / 2D + node.offset().x() * scaleX),
                (float) (height + node.offset().y() * scaleY),
                (float) (0.5D - scaleZ / 2D + node.offset().z() * scaleZ)
        );
    }

    static void applyAxisRotation(
            TileModelView view,
            DisplayNode.Axis axis,
            double degrees
    ) {
        int quarterTurns = Math.floorMod((int) Math.round(degrees / 90D), 4);
        if (quarterTurns == 0) {
            return;
        }
        float x = axis == DisplayNode.Axis.X ? 1F : 0F;
        float y = axis == DisplayNode.Axis.Y ? 1F : 0F;
        float z = axis == DisplayNode.Axis.Z ? 1F : 0F;
        float[][] offsets = switch (axis) {
            case X -> new float[][]{
                    {0F, 0F, 0F}, {0F, 0F, -1F}, {0F, -1F, -1F}, {0F, -1F, 0F}
            };
            case Y -> new float[][]{
                    {0F, 0F, 0F}, {-1F, 0F, 0F}, {-1F, 0F, -1F}, {0F, 0F, -1F}
            };
            case Z -> new float[][]{
                    {0F, 0F, 0F}, {0F, -1F, 0F}, {-1F, -1F, 0F}, {-1F, 0F, 0F}
            };
        };
        float[] offset = offsets[quarterTurns];
        view.translate(offset[0], offset[1], offset[2]);
        view.rotate(quarterTurns * 90F, x, y, z);
    }

    static double facingDegrees(BlockNeighborhood block) {
        return switch (block.getBlockState().getProperties().getOrDefault(
                "facing", "south")) {
            case "south" -> 0D;
            case "east" -> 90D;
            case "north" -> 180D;
            case "west" -> 270D;
            default -> throw new IllegalArgumentException("invalid facing");
        };
    }

    @FunctionalInterface
    interface NodeEmitter {

        boolean emit(
                DisplayNode node,
                BlockNeighborhood block,
                TileModelView view,
                OverlayColorAccumulator color,
                double outerYDegrees,
                double outerYScale
        );
    }
}
