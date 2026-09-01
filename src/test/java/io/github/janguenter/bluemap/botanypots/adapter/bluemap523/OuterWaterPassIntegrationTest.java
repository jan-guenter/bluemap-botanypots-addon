/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import com.flowpowered.math.vector.Vector3f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.map.hires.block.BlockStateModelRenderer;
import de.bluecolored.bluemap.core.map.mask.Mask;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.PackVersion;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OuterWaterPassIntegrationTest {

    private static final Key POT_RENDERER = Key.parse("test:counted_botany_pot");
    private static final Key WATER_RENDERER = Key.parse("test:counted_water");
    private static final Key POT_STATE = Key.parse("botanypots:terracotta_botany_pot");
    private static final Key SYNTHETIC_STATE =
            Key.parse("bluemap_botanypots:botany_pot_overlay");
    private static final Key WATER_STATE = Key.parse("minecraft:water");
    private static final Key POT_MODEL = Key.parse("test:block/pot");
    private static final Key WATER_MODEL = Key.parse("test:block/water");
    private static final Key POT_TEXTURE = Key.parse("test:block/pot");
    private static final Key WATER_TEXTURE = Key.parse("test:block/water");

    @Test
    void outerBlockStateRendererAddsExactlyOneRealWaterPass() throws IOException {
        assertTrue(BlueMap523Adapter.install());
        BlockRendererType actual = BlockRendererType.REGISTRY.get(
                BlueMap523Adapter.RENDERER_KEY);
        assertNotNull(actual);

        AtomicInteger potCalls = new AtomicInteger();
        AtomicInteger waterCalls = new AtomicInteger();
        BlockRendererType countedPot = new BlockRendererType.Impl(
                POT_RENDERER,
                (pack, gallery, settings) -> counting(
                        actual.create(pack, gallery, settings), potCalls)
        );
        BlockRendererType countedWater = new BlockRendererType.Impl(
                WATER_RENDERER,
                (pack, gallery, settings) -> counting(
                        BlockRendererType.LIQUID.create(pack, gallery, settings),
                        waterCalls)
        );
        assertNull(BlockRendererType.REGISTRY.get(POT_RENDERER));
        assertNull(BlockRendererType.REGISTRY.get(WATER_RENDERER));
        BlockRendererType.REGISTRY.register(countedPot);
        BlockRendererType.REGISTRY.register(countedWater);

        ResourcePack pack = new ResourcePack(new PackVersion(34, 0));
        Texture potTexture = texture(POT_TEXTURE, 0x80FF0000);
        Texture waterTexture = texture(WATER_TEXTURE, 0x400000FF);
        pack.getTextures().put(POT_TEXTURE, potTexture);
        pack.getTextures().put(WATER_TEXTURE, waterTexture);
        pack.getModels().put(POT_MODEL, potModel());
        pack.getModels().put(WATER_MODEL, waterModel());

        Variant potVariant = new Variant(new ResourcePath<Model>(POT_MODEL));
        potVariant.setRenderer(countedPot);
        BlockState potState = defaultState(potVariant);
        pack.getBlockStates().put(POT_STATE, potState);
        pack.getBlockStates().put(SYNTHETIC_STATE, potState);

        Variant waterVariant = new Variant(new ResourcePath<Model>(WATER_MODEL));
        waterVariant.setRenderer(countedWater);
        pack.getBlockStates().put(WATER_STATE, defaultState(waterVariant));

        TextureGallery gallery = new TextureGallery();
        gallery.put(pack.getTextures());
        RenderSettings settings = renderSettings();
        BlockNeighborhood block = new BlockNeighborhood(
                new FixedBlockAccess(), pack, settings, DimensionType.OVERWORLD);
        ArrayTileModel tile = new ArrayTileModel(32);
        Color color = new Color();

        new BlockStateModelRenderer(pack, gallery, settings).render(
                block, new TileModelView(tile), color);

        assertEquals(1, potCalls.get());
        assertEquals(1, waterCalls.get());
        assertEquals(14, tile.size());
        assertTrue(color.premultiplied);
        float potAlpha = potTexture.getColorStraight().a;
        float waterAlpha = waterTexture.getColorStraight().a;
        assertEquals(potAlpha, color.r, 0.000_001F);
        assertEquals(0F, color.g, 0.000_001F);
        assertEquals((1F - potAlpha) * waterAlpha, color.b, 0.000_001F);
        assertEquals(
                potAlpha + (1F - potAlpha) * waterAlpha,
                color.a,
                0.000_001F);
    }

    private static BlockRenderer counting(
            BlockRenderer delegate,
            AtomicInteger calls
    ) {
        return (block, variant, target, color) -> {
            calls.incrementAndGet();
            delegate.render(block, variant, target, color);
        };
    }

    private static Texture texture(Key key, int argb) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        return Texture.from(key, image);
    }

    private static Model potModel() {
        Face top = new Face(new TextureVariable(new ResourcePath<>(POT_TEXTURE)));
        Element element = new Element(
                Vector3f.ZERO,
                new Vector3f(16F, 16F, 16F),
                Map.of(Direction.UP, top)
        );
        return new Model(Map.of(), new Element[]{element}, false);
    }

    private static Model waterModel() {
        TextureVariable texture = new TextureVariable(
                new ResourcePath<>(WATER_TEXTURE));
        return new Model(Map.of("still", texture, "flow", texture.copy()));
    }

    private static BlockState defaultState(Variant variant) {
        return new BlockState(new Variants(
                new VariantSet[0], new VariantSet(variant)));
    }

    private static RenderSettings renderSettings() {
        return new RenderSettings() {
            @Override
            public int getRemoveCavesBelowY() {
                return Integer.MIN_VALUE;
            }

            @Override
            public int getCaveDetectionOceanFloor() {
                return 0;
            }

            @Override
            public boolean isCaveDetectionUsesBlockLight() {
                return false;
            }

            @Override
            public float getAmbientLight() {
                return 1F;
            }

            @Override
            public Mask getRenderMask() {
                return Mask.ALL;
            }

            @Override
            public boolean isSaveHiresLayer() {
                return false;
            }

            @Override
            public boolean isRenderTopOnly() {
                return false;
            }
        };
    }

    private static final class FixedBlockAccess implements BlockAccess {

        private int x;
        private int y;
        private int z;

        @Override
        public void set(int newX, int newY, int newZ) {
            x = newX;
            y = newY;
            z = newZ;
        }

        @Override
        public BlockAccess copy() {
            FixedBlockAccess copy = new FixedBlockAccess();
            copy.set(x, y, z);
            return copy;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public de.bluecolored.bluemap.core.world.BlockState getBlockState() {
            if (x == 0 && y == 0 && z == 0) {
                return de.bluecolored.bluemap.core.world.BlockState.fromString(
                        "botanypots:terracotta_botany_pot"
                                + "[facing=south,level=0,waterlogged=true]");
            }
            return de.bluecolored.bluemap.core.world.BlockState.AIR;
        }

        @Override
        public LightData getLightData() {
            return new LightData(15, 0);
        }

        @Override
        public Biome getBiome() {
            return Biome.DEFAULT;
        }

        @Override
        public BlockEntity getBlockEntity() {
            return null;
        }

        @Override
        public boolean hasOceanFloorY() {
            return false;
        }

        @Override
        public int getOceanFloorY() {
            return 0;
        }
    }
}
