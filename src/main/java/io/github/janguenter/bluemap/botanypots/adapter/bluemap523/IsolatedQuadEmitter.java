/*
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Face geometry and UV conventions are adapted from BlueMap 5.22's
 * MIT-licensed ResourceModelRenderer for the add-on's isolated display path.
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;

/** Emits an unculled face-local quad using an operator-installed texture. */
final class IsolatedQuadEmitter {

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;
    private final RenderSettings renderSettings;

    IsolatedQuadEmitter(
            ResourcePack resourcePack,
            TextureGallery textureGallery,
            RenderSettings renderSettings
    ) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;
    }

    boolean emit(
            DisplayNode node,
            BlockNeighborhood block,
            TileModelView target,
            OverlayColorAccumulator color,
            Direction face,
            float left,
            float bottom,
            float right,
            float top,
            Key textureKey,
            int tintArgb,
            double outerYDegrees,
            double outerYScale
    ) {
        Texture texture = resourcePack.getTextures().get(textureKey);
        if (texture == null || left < 0F || bottom < 0F || right > 1F || top > 1F
                || left >= right || bottom >= top) {
            return false;
        }
        DisplayFace.Sample lighting = DisplayFace.axisAligned(
                node, face, block, outerYDegrees, outerYScale
        );
        if (DisplayFace.hiddenByCave(
                block, renderSettings.isCaveDetectionUsesBlockLight(), lighting)) {
            return true;
        }
        if (renderSettings.isRenderTopOnly() && lighting.normalY() < 0.01F) {
            return true;
        }

        Point bottomLeft = point(face, left, bottom);
        Point bottomRight = point(face, right, bottom);
        Point topRight = point(face, right, top);
        Point topLeft = point(face, left, top);
        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        mesh.setPositions(start,
                bottomLeft.x(), bottomLeft.y(), bottomLeft.z(),
                bottomRight.x(), bottomRight.y(), bottomRight.z(),
                topRight.x(), topRight.y(), topRight.z());
        mesh.setPositions(start + 1,
                bottomLeft.x(), bottomLeft.y(), bottomLeft.z(),
                topRight.x(), topRight.y(), topRight.z(),
                topLeft.x(), topLeft.y(), topLeft.z());
        mesh.setUvs(start, left, 1F - bottom, right, 1F - bottom, right, 1F - top);
        mesh.setUvs(start + 1, left, 1F - bottom, right, 1F - top, left, 1F - top);
        int material = textureGallery.get(textureKey);
        mesh.setMaterialIndex(start, material);
        mesh.setMaterialIndex(start + 1, material);
        float red = ((tintArgb >>> 16) & 0xFF) / 255F;
        float green = ((tintArgb >>> 8) & 0xFF) / 255F;
        float blue = (tintArgb & 0xFF) / 255F;
        mesh.setColor(start, red, green, blue);
        mesh.setColor(start + 1, red, green, blue);
        mesh.setAOs(start, 1F, 1F, 1F);
        mesh.setAOs(start + 1, 1F, 1F, 1F);
        mesh.setSunlight(start, lighting.sunlight());
        mesh.setSunlight(start + 1, lighting.sunlight());
        mesh.setBlocklight(start, lighting.blocklight());
        mesh.setBlocklight(start + 1, lighting.blocklight());
        if (lighting.normalY() > 0.01F) {
            color.add(texture, tintArgb, lighting.sunlight(), lighting.blocklight());
        }
        return true;
    }

    /** Emits one exact Bookshelf IRenderHelper full-fluid-box face. */
    boolean emitFluidFace(
            DisplayNode node,
            BlockNeighborhood block,
            TileModelView target,
            OverlayColorAccumulator color,
            Direction face,
            Key textureKey,
            int tintArgb,
            double outerYDegrees,
            double outerYScale
    ) {
        Texture texture = resourcePack.getTextures().get(textureKey);
        if (texture == null) {
            return false;
        }
        DisplayFace.Sample lighting = DisplayFace.axisAligned(
                node, face, block, outerYDegrees, outerYScale);
        if (DisplayFace.hiddenByCave(
                block, renderSettings.isCaveDetectionUsesBlockLight(), lighting)) {
            return true;
        }
        if (renderSettings.isRenderTopOnly() && lighting.normalY() < 0.01F) {
            return true;
        }
        FluidVertex[] vertices = fluidVertices(face);
        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        setTriangle(mesh, start, vertices[0], vertices[1], vertices[2]);
        setTriangle(mesh, start + 1, vertices[0], vertices[2], vertices[3]);
        int material = textureGallery.get(textureKey);
        mesh.setMaterialIndex(start, material);
        mesh.setMaterialIndex(start + 1, material);
        float red = ((tintArgb >>> 16) & 0xFF) / 255F;
        float green = ((tintArgb >>> 8) & 0xFF) / 255F;
        float blue = (tintArgb & 0xFF) / 255F;
        mesh.setColor(start, red, green, blue);
        mesh.setColor(start + 1, red, green, blue);
        mesh.setAOs(start, 1F, 1F, 1F);
        mesh.setAOs(start + 1, 1F, 1F, 1F);
        mesh.setSunlight(start, lighting.sunlight());
        mesh.setSunlight(start + 1, lighting.sunlight());
        mesh.setBlocklight(start, lighting.blocklight());
        mesh.setBlocklight(start + 1, lighting.blocklight());
        if (lighting.normalY() > 0.01F) {
            color.add(texture, tintArgb, lighting.sunlight(), lighting.blocklight());
        }
        return true;
    }

    private static void setTriangle(
            TileModel mesh,
            int index,
            FluidVertex first,
            FluidVertex second,
            FluidVertex third
    ) {
        mesh.setPositions(index,
                first.x(), first.y(), first.z(),
                second.x(), second.y(), second.z(),
                third.x(), third.y(), third.z());
        mesh.setUvs(index,
                first.u(), first.v(), second.u(), second.v(), third.u(), third.v());
    }

    static FluidVertex[] fluidVertices(Direction face) {
        return switch (face) {
            case DOWN -> new FluidVertex[]{
                    v(0F, 0F, 1F, 0F, 1F), v(0F, 0F, 0F, 0F, 0F),
                    v(1F, 0F, 0F, 1F, 0F), v(1F, 0F, 1F, 1F, 1F)};
            case UP -> new FluidVertex[]{
                    v(0F, 1F, 1F, 0F, 1F), v(1F, 1F, 1F, 1F, 1F),
                    v(1F, 1F, 0F, 1F, 0F), v(0F, 1F, 0F, 0F, 0F)};
            case NORTH -> new FluidVertex[]{
                    v(0F, 0F, 0F, 0F, 0F), v(0F, 1F, 0F, 0F, 1F),
                    v(1F, 1F, 0F, 1F, 1F), v(1F, 0F, 0F, 1F, 0F)};
            case SOUTH -> new FluidVertex[]{
                    v(1F, 0F, 1F, 1F, 0F), v(1F, 1F, 1F, 1F, 1F),
                    v(0F, 1F, 1F, 0F, 1F), v(0F, 0F, 1F, 0F, 0F)};
            case WEST -> new FluidVertex[]{
                    v(0F, 0F, 1F, 0F, 1F), v(0F, 1F, 1F, 1F, 1F),
                    v(0F, 1F, 0F, 1F, 0F), v(0F, 0F, 0F, 0F, 0F)};
            case EAST -> new FluidVertex[]{
                    v(1F, 0F, 0F, 0F, 0F), v(1F, 1F, 0F, 1F, 0F),
                    v(1F, 1F, 1F, 1F, 1F), v(1F, 0F, 1F, 0F, 1F)};
        };
    }

    private static FluidVertex v(float x, float y, float z, float u, float value) {
        return new FluidVertex(x, y, z, u, value);
    }

    private static Point point(Direction face, float horizontal, float vertical) {
        return switch (face) {
            case DOWN -> new Point(horizontal, 0F, vertical);
            case UP -> new Point(horizontal, 1F, 1F - vertical);
            case NORTH -> new Point(1F - horizontal, vertical, 0F);
            case SOUTH -> new Point(horizontal, vertical, 1F);
            case WEST -> new Point(0F, vertical, horizontal);
            case EAST -> new Point(1F, vertical, 1F - horizontal);
        };
    }

    private record Point(float x, float y, float z) {
    }

    record FluidVertex(float x, float y, float z, float u, float v) {
    }
}
