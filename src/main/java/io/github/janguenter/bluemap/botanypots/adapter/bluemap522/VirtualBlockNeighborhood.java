/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

/** Virtual, unculled neighborhood with ordinary random block offsets disabled. */
final class VirtualBlockNeighborhood extends BlockNeighborhood {

    private final BlockProperties properties;

    VirtualBlockNeighborhood(
            BlockAccess real,
            BlockState displayState,
            ResourcePack resourcePack,
            RenderSettings renderSettings,
            DimensionType dimensionType
    ) {
        super(new VirtualBlockAccess(real, displayState), resourcePack, renderSettings, dimensionType);
        properties = resourcePack.getBlockProperties(displayState).toBuilder()
                .culling(false)
                .occluding(false)
                .cullingIdentical(false)
                .randomOffset(false)
                .build();
        set(real.getX(), real.getY(), real.getZ());
    }

    @Override
    public BlockProperties getProperties() {
        return properties;
    }
}
