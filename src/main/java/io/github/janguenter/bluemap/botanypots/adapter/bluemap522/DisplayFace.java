/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;

/** Final display-face orientation, exact BER light, and independent cave sample. */
final class DisplayFace {

    private DisplayFace() {
    }

    static Sample sample(
            DisplayNode node,
            Direction direction,
            Element element,
            Variant variant,
            BlockNeighborhood block,
            double outerYDegrees,
            double outerYScale
    ) {
        Normal normal = transformedNormal(
                node, direction, element, variant, outerYDegrees, outerYScale);
        return sample(block, normal);
    }

    static Normal transformedNormal(
            DisplayNode node,
            Direction direction,
            Element element,
            Variant variant,
            double outerYDegrees,
            double outerYScale
    ) {
        Vec normal = direction(direction);
        normal = normalMatrix(normal, element.getRotation().getMatrix());
        if (variant.isTransformed()) {
            normal = normalMatrix(normal, variant.getTransformMatrix());
        }
        for (int index = node.rotations().size() - 1; index >= 0; index--) {
            DisplayNode.Rotation rotation = node.rotations().get(index);
            normal = rotate(normal, rotation.axis(), rotation.degrees());
        }
        normal = inverseScale(normal, node.scale().x(), node.scale().y(), node.scale().z());
        normal = inverseScale(normal, 1D, outerYScale, 1D);
        normal = rotate(normal, DisplayNode.Axis.Y, outerYDegrees);
        normal = normalize(normal);
        return new Normal(normal.x(), normal.y(), normal.z());
    }

    private static Sample sample(BlockNeighborhood block, Normal normal) {
        int dx = Math.round(normal.x());
        int dy = Math.round(normal.y());
        int dz = Math.round(normal.z());
        int sunlight = block.getSunLightLevel();
        int blocklight = block.getBlockLightLevel();
        int caveSunlight = sunlight;
        int caveBlocklight = blocklight;
        if (dx != 0 || dy != 0 || dz != 0) {
            var neighbor = block.getNeighborBlock(dx, dy, dz);
            caveSunlight = Math.max(caveSunlight, neighbor.getLightData().getSkyLight());
            caveBlocklight = Math.max(caveBlocklight, neighbor.getLightData().getBlockLight());
        }
        return new Sample(normal.y(), sunlight, blocklight, caveSunlight, caveBlocklight);
    }

    static Sample axisAligned(
            DisplayNode node,
            Direction direction,
            BlockNeighborhood block,
            double outerYDegrees,
            double outerYScale
    ) {
        Vec normal = direction(direction);
        for (int index = node.rotations().size() - 1; index >= 0; index--) {
            DisplayNode.Rotation rotation = node.rotations().get(index);
            normal = rotate(normal, rotation.axis(), rotation.degrees());
        }
        normal = inverseScale(normal, node.scale().x(), node.scale().y(), node.scale().z());
        normal = inverseScale(normal, 1D, outerYScale, 1D);
        normal = rotate(normal, DisplayNode.Axis.Y, outerYDegrees);
        normal = normalize(normal);
        return sample(block, new Normal(normal.x(), normal.y(), normal.z()));
    }

    static boolean hiddenByCave(
            BlockNeighborhood block,
            boolean caveUsesBlockLight,
            Sample sample
    ) {
        int visible = caveUsesBlockLight
                ? Math.max(sample.caveSunlight(), sample.caveBlocklight())
                : sample.caveSunlight();
        return block.isRemoveIfCave() && visible == 0;
    }

    private static Vec direction(Direction direction) {
        return switch (direction) {
            case DOWN -> new Vec(0F, -1F, 0F);
            case UP -> new Vec(0F, 1F, 0F);
            case NORTH -> new Vec(0F, 0F, -1F);
            case SOUTH -> new Vec(0F, 0F, 1F);
            case WEST -> new Vec(-1F, 0F, 0F);
            case EAST -> new Vec(1F, 0F, 0F);
        };
    }

    private static Vec normalMatrix(Vec vector, MatrixM4f matrix) {
        float c00 = matrix.m11 * matrix.m22 - matrix.m12 * matrix.m21;
        float c01 = matrix.m12 * matrix.m20 - matrix.m10 * matrix.m22;
        float c02 = matrix.m10 * matrix.m21 - matrix.m11 * matrix.m20;
        float c10 = matrix.m02 * matrix.m21 - matrix.m01 * matrix.m22;
        float c11 = matrix.m00 * matrix.m22 - matrix.m02 * matrix.m20;
        float c12 = matrix.m01 * matrix.m20 - matrix.m00 * matrix.m21;
        float c20 = matrix.m01 * matrix.m12 - matrix.m02 * matrix.m11;
        float c21 = matrix.m02 * matrix.m10 - matrix.m00 * matrix.m12;
        float c22 = matrix.m00 * matrix.m11 - matrix.m01 * matrix.m10;
        float determinant = matrix.m00 * c00 + matrix.m01 * c01 + matrix.m02 * c02;
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1E-8F) {
            throw new IllegalArgumentException("singular display face transform");
        }
        float inverseDeterminant = 1F / determinant;
        return new Vec(
                (c00 * vector.x() + c01 * vector.y() + c02 * vector.z())
                        * inverseDeterminant,
                (c10 * vector.x() + c11 * vector.y() + c12 * vector.z())
                        * inverseDeterminant,
                (c20 * vector.x() + c21 * vector.y() + c22 * vector.z())
                        * inverseDeterminant
        );
    }

    private static Vec inverseScale(Vec vector, double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || x == 0D || y == 0D || z == 0D) {
            throw new IllegalArgumentException("singular display face scale");
        }
        return new Vec(
                (float) (vector.x() / x),
                (float) (vector.y() / y),
                (float) (vector.z() / z)
        );
    }

    private static Vec rotate(Vec vector, DisplayNode.Axis axis, double degrees) {
        double radians = Math.toRadians(degrees);
        float sine = (float) Math.sin(radians);
        float cosine = (float) Math.cos(radians);
        return switch (axis) {
            case X -> new Vec(
                    vector.x(),
                    cosine * vector.y() - sine * vector.z(),
                    sine * vector.y() + cosine * vector.z()
            );
            case Y -> new Vec(
                    cosine * vector.x() + sine * vector.z(),
                    vector.y(),
                    -sine * vector.x() + cosine * vector.z()
            );
            case Z -> new Vec(
                    cosine * vector.x() - sine * vector.y(),
                    sine * vector.x() + cosine * vector.y(),
                    vector.z()
            );
        };
    }

    private static Vec normalize(Vec vector) {
        float length = (float) Math.sqrt(
                vector.x() * vector.x() + vector.y() * vector.y() + vector.z() * vector.z()
        );
        if (length == 0F || !Float.isFinite(length)) {
            throw new IllegalArgumentException("invalid display face normal");
        }
        return new Vec(vector.x() / length, vector.y() / length, vector.z() / length);
    }

    record Sample(
            float normalY,
            int sunlight,
            int blocklight,
            int caveSunlight,
            int caveBlocklight
    ) {
    }

    record Normal(float x, float y, float z) {
    }

    private record Vec(float x, float y, float z) {
    }
}
