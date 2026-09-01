/*
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Resource-model geometry and UV conventions are adapted from BlueMap 5.22's
 * MIT-licensed ResourceModelRenderer. This implementation is specialized for
 * Botany Pots' virtual, unculled, face-filtered display contract and never
 * applies ordinary world-block random offsets.
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Emits installed JSON models as fixed-seed virtual display quads. */
final class VirtualDisplayModelEmitter {

    private static final float BLOCK_SCALE = 1F / 16F;

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;
    private final BlockColorCalculator blockColors;
    private final Map<DisplayNode.Tint.Kind, BlockColorCalculator> tintCalculators;
    private final RenderSettings renderSettings;
    private final IsolatedQuadEmitter isolatedQuads;

    VirtualDisplayModelEmitter(
            ResourcePack resourcePack,
            TextureGallery textureGallery,
            RenderSettings renderSettings
    ) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;
        this.blockColors = resourcePack.createBlockColorCalculator();
        this.tintCalculators = Map.of(
                DisplayNode.Tint.Kind.GRASS,
                BlockColorCalculatorFactory
                        .colorMap(Key.minecraft("colormap/grass"),
                                new Color().set(0xff52952f, true))
                        .withBiomeOverlay(Biome::getOverlayGrassColor)
                        .create(resourcePack),
                DisplayNode.Tint.Kind.FOLIAGE,
                BlockColorCalculatorFactory
                        .colorMap(Key.minecraft("colormap/foliage"),
                                new Color().set(0xff48B518, true))
                        .withBiomeOverlay(Biome::getOverlayFoliageColor)
                        .create(resourcePack),
                DisplayNode.Tint.Kind.DRY_FOLIAGE,
                BlockColorCalculatorFactory
                        .colorMap(Key.minecraft("colormap/dry_foliage"),
                                new Color().set(0xff8f5f33, true))
                        .withBiomeOverlay(Biome::getOverlayDryFoliageColor)
                        .create(resourcePack),
                DisplayNode.Tint.Kind.WATER,
                BlockColorCalculatorFactory.biome(Biome::getWaterColor).create(resourcePack)
        );
        this.isolatedQuads = new IsolatedQuadEmitter(
                resourcePack, textureGallery, renderSettings
        );
    }

    boolean emit(
            DisplayNode node,
            BlockNeighborhood real,
            TileModelView target,
            OverlayColorAccumulator color,
            double outerYDegrees,
            double outerYScale
    ) {
        if (node.strategy() != DisplayNode.Strategy.RESOURCE) {
            return false;
        }
        BlockState displayState;
        try {
            displayState = BlockState.fromString(node.blockState());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        List<Variant> variants;
        if (node.modelOverride() != null) {
            variants = List.of(new Variant(new ResourcePath<Model>(node.modelOverride().value())));
        } else {
            variants = FixedVariantSelector.select(
                    resourcePack.getBlockStates().get(displayState.getId()),
                    displayState
            );
        }
        boolean emitted = false;
        EmissionEvidence evidence = new EmissionEvidence();
        if (node.renderFluid()) {
            if (!emitFluid(node, real, target, color, outerYDegrees, outerYScale)) {
                return false;
            }
            emitted = true;
        }
        for (Variant variant : variants) {
            if (variant.getRenderer() == BlockRendererType.LIQUID && node.renderFluid()) {
                continue;
            }
            if (variant.getRenderer() != BlockRendererType.DEFAULT) {
                return false;
            }
            int variantStart = target.getTileModel().size();
            Model model = variant.getModel().getResource(resourcePack.getModels()::get);
            if (model == null || model.getElements() == null) {
                return false;
            }
            for (Element element : model.getElements()) {
                if (element == null) {
                    continue;
                }
                int elementStart = target.getTileModel().size();
                if (!emitElement(
                        node, displayState, model, element, variant, real, target, color,
                        outerYDegrees, outerYScale, evidence)) {
                    return false;
                }
                TileModelView elementView = new TileModelView(target.getTileModel())
                        .initialize(target.getTileModel(), elementStart);
                elementView.transform(new MatrixM4f()
                        .copy(element.getRotation().getMatrix())
                        .scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE));
            }
            TileModelView variantView = new TileModelView(target.getTileModel())
                    .initialize(target.getTileModel(), variantStart);
            if (variant.isTransformed()) {
                variantView.transform(variant.getTransformMatrix());
            }
            emitted |= target.getTileModel().size() > variantStart;
        }
        return evidence.successful(emitted);
    }

    private boolean emitFluid(
            DisplayNode node,
            BlockNeighborhood real,
            TileModelView target,
            OverlayColorAccumulator color,
            double outerYDegrees,
            double outerYScale
    ) {
        DisplayNode.Fluid fluid = node.fluid();
        if (fluid == null) {
            return false;
        }
        boolean success = true;
        Integer fluidTint = resolveRule(fluid.tint(), real, displayState(node));
        if (fluidTint == null) {
            return false;
        }
        for (Direction face : Direction.values()) {
            success &= isolatedQuads.emitFluidFace(
                    node, real, target, color, face,
                    de.bluecolored.bluemap.core.util.Key.parse(fluid.stillTexture().value()),
                    fluidTint, outerYDegrees, outerYScale
            );
        }
        return success;
    }

    private static BlockState displayState(DisplayNode node) {
        return BlockState.fromString(node.blockState());
    }

    private boolean emitElement(
            DisplayNode node,
            BlockState state,
            Model model,
            Element element,
            Variant variant,
            BlockNeighborhood real,
            TileModelView target,
            OverlayColorAccumulator color,
            double outerYDegrees,
            double outerYScale,
            EmissionEvidence evidence
    ) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();
        float[][] corners = {
                {from.getX(), from.getY(), from.getZ()},
                {from.getX(), from.getY(), to.getZ()},
                {to.getX(), from.getY(), from.getZ()},
                {to.getX(), from.getY(), to.getZ()},
                {from.getX(), to.getY(), from.getZ()},
                {from.getX(), to.getY(), to.getZ()},
                {to.getX(), to.getY(), from.getZ()},
                {to.getX(), to.getY(), to.getZ()}
        };
        return emitFace(node, state, model, element, Direction.DOWN,
                corners[0], corners[2], corners[3], corners[1], variant, real, target, color,
                outerYDegrees, outerYScale, evidence)
                && emitFace(node, state, model, element, Direction.UP,
                corners[5], corners[7], corners[6], corners[4], variant, real, target, color,
                outerYDegrees, outerYScale, evidence)
                && emitFace(node, state, model, element, Direction.NORTH,
                corners[2], corners[0], corners[4], corners[6], variant, real, target, color,
                outerYDegrees, outerYScale, evidence)
                && emitFace(node, state, model, element, Direction.SOUTH,
                corners[1], corners[3], corners[7], corners[5], variant, real, target, color,
                outerYDegrees, outerYScale, evidence)
                && emitFace(node, state, model, element, Direction.WEST,
                corners[0], corners[1], corners[5], corners[4], variant, real, target, color,
                outerYDegrees, outerYScale, evidence)
                && emitFace(node, state, model, element, Direction.EAST,
                corners[3], corners[2], corners[6], corners[7], variant, real, target, color,
                outerYDegrees, outerYScale, evidence);
    }

    private boolean emitFace(
            DisplayNode node,
            BlockState state,
            Model model,
            Element element,
            Direction direction,
            float[] corner0,
            float[] corner1,
            float[] corner2,
            float[] corner3,
            Variant variant,
            BlockNeighborhood real,
            TileModelView target,
            OverlayColorAccumulator color,
            double outerYDegrees,
            double outerYScale,
            EmissionEvidence evidence
    ) {
        Face face = element.getFaces().get(direction);
        if (face == null || !included(node.faces(), face, variant)) {
            return true;
        }
        ResourcePath<Texture> texturePath = face.getTexture().getTexturePath(model.getTextures()::get);
        Texture texture = texturePath == null
                ? null : texturePath.getResource(resourcePack.getTextures()::get);
        if (texture == null) {
            return false;
        }
        Color tint = tint(node, face, state, real);
        if (tint == null) {
            return false;
        }
        // A face that is structurally valid but intentionally hidden by
        // BlueMap's cave/top-only policy still proves a valid display model.
        // Visibility must not turn semantic validation into an atomic failure.
        evidence.markValidFace();
        DisplayFace.Sample lighting = DisplayFace.sample(
                node, direction, element, variant, real, outerYDegrees, outerYScale
        );
        if (DisplayFace.hiddenByCave(
                real, renderSettings.isCaveDetectionUsesBlockLight(), lighting)) {
            return true;
        }
        if (renderSettings.isRenderTopOnly() && lighting.normalY() < 0.01F) {
            return true;
        }
        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        positions(mesh, start, corner0, corner1, corner2, corner3);
        uvs(mesh, start, face.getUv(), face.getRotation(), direction, variant);
        int material = textureGallery.get(texturePath);
        mesh.setMaterialIndex(start, material);
        mesh.setMaterialIndex(start + 1, material);

        mesh.setColor(start, tint.r, tint.g, tint.b);
        mesh.setColor(start + 1, tint.r, tint.g, tint.b);
        mesh.setAOs(start, 1F, 1F, 1F);
        mesh.setAOs(start + 1, 1F, 1F, 1F);
        int sunlight = lighting.sunlight();
        int blocklight = lighting.blocklight();
        mesh.setSunlight(start, sunlight);
        mesh.setSunlight(start + 1, sunlight);
        mesh.setBlocklight(start, blocklight);
        mesh.setBlocklight(start + 1, blocklight);
        if (lighting.normalY() > 0.01F) {
            color.add(texture, tint.getInt(), sunlight, blocklight);
        }
        return true;
    }

    private Color tint(DisplayNode node, Face face, BlockState state, BlockNeighborhood real) {
        DisplayNode.Tint rules = node.tint();
        if (rules != null && rules.allArgb() != null) {
            return new Color().set(rules.allArgb());
        }
        if (face.getTintindex() < 0) {
            return new Color().set(1F, 1F, 1F, 1F, false);
        }
        VirtualBlockAccess virtual = new VirtualBlockAccess(real, state);
        if (rules == null) {
            return null;
        }
        DisplayNode.Tint.Rule rule = rules.indices().get(face.getTintindex());
        if (rule == null) {
            return null;
        }
        if (rule.kind() == DisplayNode.Tint.Kind.FIXED) {
            return new Color().set(rule.argb());
        }
        Integer argb = resolveRule(rule, real, state);
        return argb == null ? null : new Color().set(argb);
    }

    private Integer resolveRule(
            DisplayNode.Tint.Rule rule,
            BlockNeighborhood real,
            BlockState state
    ) {
        if (rule.kind() == DisplayNode.Tint.Kind.FIXED) {
            return rule.argb();
        }
        BlockColorCalculator calculator = tintCalculators.get(rule.kind());
        if (calculator == null) {
            return null;
        }
        int color = ExactBiomeTint.resolve(calculator, rule.kind(), real, state);
        return multiplyOpaque(color, rule.argb());
    }

    private static int multiplyOpaque(int color, int multiplier) {
        int red = ((color >>> 16) & 0xFF) * ((multiplier >>> 16) & 0xFF) / 255;
        int green = ((color >>> 8) & 0xFF) * ((multiplier >>> 8) & 0xFF) / 255;
        int blue = (color & 0xFF) * (multiplier & 0xFF) / 255;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    static boolean included(
            Set<DisplayNode.Face> included,
            Face face,
            Variant variant
    ) {
        if (face.getCullface() == null) {
            return true;
        }
        Direction transformed = transformedDirection(face.getCullface(), variant);
        return included.contains(DisplayNode.Face.valueOf(transformed.name()));
    }

    private static void positions(
            TileModel mesh,
            int start,
            float[] c0,
            float[] c1,
            float[] c2,
            float[] c3
    ) {
        mesh.setPositions(start,
                c0[0], c0[1], c0[2], c1[0], c1[1], c1[2], c2[0], c2[1], c2[2]);
        mesh.setPositions(start + 1,
                c0[0], c0[1], c0[2], c2[0], c2[1], c2[2], c3[0], c3[1], c3[2]);
    }

    private static void uvs(
            TileModel mesh,
            int start,
            Vector4f raw,
            int rotationDegrees,
            Direction direction,
            Variant variant
    ) {
        float[][] base = {
                {raw.getX() / 16F, raw.getW() / 16F},
                {raw.getZ() / 16F, raw.getW() / 16F},
                {raw.getZ() / 16F, raw.getY() / 16F},
                {raw.getX() / 16F, raw.getY() / 16F}
        };
        int rotation = Math.floorMod(Math.floorDiv(rotationDegrees, 90), 4);
        List<float[]> uv = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            float[] point = base[(rotation + index) % 4].clone();
            if (variant.isUvlock() && variant.isTransformed()) {
                rotateUv(point, uvLockRotation(direction, variant));
            }
            uv.add(point);
        }
        mesh.setUvs(start,
                uv.get(0)[0], uv.get(0)[1], uv.get(1)[0], uv.get(1)[1],
                uv.get(2)[0], uv.get(2)[1]);
        mesh.setUvs(start + 1,
                uv.get(0)[0], uv.get(0)[1], uv.get(2)[0], uv.get(2)[1],
                uv.get(3)[0], uv.get(3)[1]);
    }

    private static void rotateUv(float[] uv, float radians) {
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        float x = uv[0] - 0.5F;
        float y = uv[1] - 0.5F;
        uv[0] = cosine * x - sine * y + 0.5F;
        uv[1] = sine * x + cosine * y + 0.5F;
    }

    /** BlueMap 5.22's exact UV-lock counter-rotation oracle. */
    static float uvLockRotation(Direction direction, Variant variant) {
        if (!variant.isTransformed()) {
            return 0F;
        }
        Vec normal = variantVector(direction, variant);
        Vec localUp = variantVector(direction.getLocalUp(), variant);
        Vec worldUp = new Vec(0F, 1F, 0F);
        float projection = dot(worldUp, normal);
        Vec projected = new Vec(
                -normal.x() * projection,
                1F - normal.y() * projection,
                -normal.z() * projection
        );
        if (lengthSquared(projected) < 0.01F) {
            projected = direction(normal.y() > 0F ? Direction.UP.getLocalUp()
                    : Direction.DOWN.getLocalUp());
        } else {
            projected = normalize(projected);
        }
        return (float) Math.atan2(dot(cross(localUp, projected), normal),
                dot(localUp, projected));
    }

    static Direction transformedDirection(Direction direction, Variant variant) {
        Vec vector = variantVector(direction, variant);
        float absoluteX = Math.abs(vector.x());
        float absoluteY = Math.abs(vector.y());
        float absoluteZ = Math.abs(vector.z());
        if (absoluteX > absoluteY && absoluteX > absoluteZ) {
            return vector.x() > 0F ? Direction.EAST : Direction.WEST;
        }
        if (absoluteY > absoluteZ) {
            return vector.y() > 0F ? Direction.UP : Direction.DOWN;
        }
        return vector.z() > 0F ? Direction.SOUTH : Direction.NORTH;
    }

    private static Vec variantVector(Direction direction, Variant variant) {
        Vec vector = direction(direction);
        MatrixM4f matrix = variant.getTransformMatrix();
        return new Vec(
                matrix.m00 * vector.x() + matrix.m01 * vector.y() + matrix.m02 * vector.z(),
                matrix.m10 * vector.x() + matrix.m11 * vector.y() + matrix.m12 * vector.z(),
                matrix.m20 * vector.x() + matrix.m21 * vector.y() + matrix.m22 * vector.z()
        );
    }

    private static Vec direction(Direction direction) {
        var vector = direction.toVector();
        return new Vec(vector.getX(), vector.getY(), vector.getZ());
    }

    private static float dot(Vec left, Vec right) {
        return left.x() * right.x() + left.y() * right.y() + left.z() * right.z();
    }

    private static Vec cross(Vec left, Vec right) {
        return new Vec(
                left.y() * right.z() - left.z() * right.y(),
                left.z() * right.x() - left.x() * right.z(),
                left.x() * right.y() - left.y() * right.x()
        );
    }

    private static float lengthSquared(Vec vector) {
        return dot(vector, vector);
    }

    private static Vec normalize(Vec vector) {
        float inverse = 1F / (float) Math.sqrt(lengthSquared(vector));
        return new Vec(vector.x() * inverse, vector.y() * inverse, vector.z() * inverse);
    }

    private record Vec(float x, float y, float z) {
    }

    static final class EmissionEvidence {

        private boolean validFace;

        void markValidFace() {
            validFace = true;
        }

        boolean successful(boolean emittedGeometry) {
            return emittedGeometry || validFace;
        }
    }
}
