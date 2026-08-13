/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.MaxCapacityReachedException;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.botanypots.activation.BotanyPotsRuntime;
import io.github.janguenter.bluemap.botanypots.catalog.CatalogResolver;
import io.github.janguenter.bluemap.botanypots.catalog.NormalizedCatalog;
import io.github.janguenter.bluemap.botanypots.catalog.RenderSelection;
import io.github.janguenter.bluemap.botanypots.model.BotanySnapshot;
import io.github.janguenter.bluemap.botanypots.model.BotanySnapshotDecoder;
import io.github.janguenter.bluemap.botanypots.profile.RouteCatalog;

/** Stock-shell-first stable-optics renderer with transactional soil/crop overlays. */
final class BotanyPotsRenderer implements BlockRenderer {

    private final ResourcePack resourcePack;
    private final BotanyPotsRuntime runtime;
    private final ResourceModelRenderer stock;
    private final DisplayProgramRenderer displays;
    private final BotanySnapshotDecoder snapshotDecoder = new BotanySnapshotDecoder();
    private final CatalogResolver catalogResolver = new CatalogResolver();

    BotanyPotsRenderer(
            ResourcePack resourcePack,
            TextureGallery textureGallery,
            RenderSettings renderSettings,
            BotanyPotsRuntime runtime
    ) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
        this.stock = new ResourceModelRenderer(resourcePack, textureGallery, renderSettings);
        this.displays = new DisplayProgramRenderer(
                new VirtualDisplayModelEmitter(resourcePack, textureGallery, renderSettings));
    }

    @Override
    public void render(
            BlockNeighborhood block,
            Variant ignoredDispatch,
            TileModelView target,
            Color mapColor
    ) {
        renderStock(block, target, mapColor);
        NormalizedCatalog catalog = runtime.catalog();
        RouteCatalog owned = runtime.ownedBlocks();
        if (!runtime.route(BotanyPotsRuntime.CORE).isActive()
                || catalog == null || owned == null
                || !owned.contains(block.getBlockState().getId().getFormatted())) {
            return;
        }

        TileModel tile = target.getTileModel();
        int overlayStart = tile.size();
        Color originalColor = new Color().set(mapColor);
        try {
            BotanyPotBlockEntityData data = block.getBlockEntity()
                    instanceof BotanyPotBlockEntityData found ? found : null;
            BotanySnapshot snapshot = snapshotDecoder.decode(
                    data == null ? null : data.inventory()
            );
            RenderSelection selection = catalogResolver.resolve(
                    snapshot,
                    catalog,
                    entry -> runtime.route(entry.profile()).isActive()
            );
            if (!selection.hasSoil()) {
                return;
            }
            renderOverlaysAtomically(
                    selection, block, tile, mapColor, block.getRenderSettings(), displays
            );
        } catch (MaxCapacityReachedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            tile.reset(overlayStart);
            mapColor.set(originalColor);
        }
    }

    static boolean renderOverlaysAtomically(
            RenderSelection selection,
            BlockNeighborhood block,
            TileModel tile,
            Color mapColor,
            RenderSettings renderSettings,
            DisplayProgramRenderer displays
    ) {
        int overlayStart = tile.size();
        Color originalColor = new Color().set(mapColor);
        OverlayColorAccumulator overlayColor = new OverlayColorAccumulator(
                mapColor, renderSettings
        );
        try {
            if (selection.hasCrop()
                    && !displays.renderCrop(selection.crop(), block, tile, overlayColor)) {
                tile.reset(overlayStart);
                mapColor.set(originalColor);
                return false;
            }
            if (!displays.renderSoil(selection.soil(), block, tile, overlayColor)) {
                tile.reset(overlayStart);
                mapColor.set(originalColor);
                return false;
            }
            overlayColor.commit();
            return true;
        } catch (MaxCapacityReachedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            tile.reset(overlayStart);
            mapColor.set(originalColor);
            return false;
        }
    }

    private void renderStock(
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState original =
                resourcePack.getBlockStates().get(block.getBlockState().getId());
        if (original == null) {
            return;
        }
        mapColor.set(0F, 0F, 0F, 0F, true);
        Color variantColor = new Color();
        float[] maximumOpacity = {0F};
        original.forEach(
                block.getBlockState(),
                block.getX(),
                block.getY(),
                block.getZ(),
                variant -> {
                    variantColor.set(0F, 0F, 0F, 0F, true);
                    renderStockVariant(
                            block, variant, target, variantColor, stock::render);
                    maximumOpacity[0] = Math.max(maximumOpacity[0], variantColor.a);
                    mapColor.add(variantColor.premultiplied());
                    target.initialize();
                }
        );
        if (mapColor.a > 0F) {
            mapColor.flatten().straight();
            mapColor.a = maximumOpacity[0];
        }
    }

    static void renderStockVariant(
            BlockNeighborhood block,
            Variant variant,
            TileModelView target,
            Color variantColor,
            StockEmitter emitter
    ) {
        TileModelView isolated = new TileModelView(target.getTileModel()).initialize();
        emitter.emit(block, variant, isolated, variantColor);
    }

    @FunctionalInterface
    interface StockEmitter {

        void emit(
                BlockNeighborhood block,
                Variant variant,
                TileModelView target,
                Color variantColor
        );
    }

}
