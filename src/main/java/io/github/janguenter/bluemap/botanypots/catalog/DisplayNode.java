/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One mature, normalized display member emitted from installed resources. */
public record DisplayNode(
        String blockState,
        ResourceId modelOverride,
        Fluid fluid,
        Tint tint,
        Set<Face> faces,
        Vector3 scale,
        Vector3 offset,
        List<Rotation> rotations,
        Strategy strategy
) {

    public DisplayNode {
        Objects.requireNonNull(blockState, "blockState");
        if (blockState.length() > 512
                || !blockState.matches("[a-z0-9_.-]+:[a-z0-9/._-]+(?:\\[[a-z0-9_=,.-]+])?")) {
            throw new IllegalArgumentException("invalid normalized block state");
        }
        Objects.requireNonNull(faces, "faces");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(rotations, "rotations");
        Objects.requireNonNull(strategy, "strategy");
        faces = Set.copyOf(faces);
        rotations = List.copyOf(rotations);
        if (faces.isEmpty() || rotations.size() > 8) {
            throw new IllegalArgumentException("display member outside supported bounds");
        }
    }

    public ResourceId blockId() {
        int properties = blockState.indexOf('[');
        return ResourceId.parse(properties < 0 ? blockState : blockState.substring(0, properties));
    }

    public boolean renderFluid() {
        return fluid != null;
    }

    public Integer tintArgb() {
        return tint == null ? null : tint.allArgb();
    }

    /** Exact per-quad tint semantics exported from the installed client callbacks. */
    public record Tint(Integer allArgb, Map<Integer, Rule> indices) {

        public Tint {
            Objects.requireNonNull(indices, "indices");
            indices = Map.copyOf(indices);
            if (allArgb == null && indices.isEmpty()
                    || allArgb != null && !indices.isEmpty()
                    || allArgb != null && (allArgb >>> 24) != 0xFF
                    || indices.keySet().stream().anyMatch(index -> index < 0 || index > 255)) {
                throw new IllegalArgumentException("invalid display tint rules");
            }
        }

        public record Rule(Kind kind, int argb) {

            public Rule {
                Objects.requireNonNull(kind, "kind");
                if ((argb >>> 24) != 0xFF) {
                    throw new IllegalArgumentException("tint multiplier must be opaque");
                }
            }
        }

        public enum Kind {
            FIXED,
            GRASS,
            FOLIAGE,
            DRY_FOLIAGE,
            WATER
        }
    }

    /** Exact static fluid-box material exported from the client fluid extension. */
    public record Fluid(ResourceId fluidId, ResourceId stillTexture, Tint.Rule tint) {

        public Fluid {
            Objects.requireNonNull(fluidId, "fluidId");
            Objects.requireNonNull(stillTexture, "stillTexture");
            Objects.requireNonNull(tint, "tint");
            if (tint.kind() == Tint.Kind.FIXED && (tint.argb() >>> 24) != 0xFF) {
                throw new IllegalArgumentException(
                        "translucent fluid tint cannot be represented exactly");
            }
        }
    }

    public enum Face {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    public enum Strategy {
        RESOURCE
    }

    public record Vector3(double x, double y, double z) {

        public Vector3 {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 16D || Math.abs(y) > 16D || Math.abs(z) > 16D) {
                throw new IllegalArgumentException("display vector outside supported bounds");
            }
        }
    }

    public record Rotation(Axis axis, double degrees) {

        public Rotation {
            Objects.requireNonNull(axis, "axis");
            if (!Double.isFinite(degrees) || degrees < -360D || degrees > 360D
                    || Math.rint(degrees / 90D) * 90D != degrees) {
                throw new IllegalArgumentException("display rotation outside supported bounds");
            }
        }
    }

    public enum Axis {
        X,
        Y,
        Z
    }
}
