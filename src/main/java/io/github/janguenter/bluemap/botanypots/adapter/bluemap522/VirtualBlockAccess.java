/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import org.jetbrains.annotations.Nullable;

/** Air-surrounded display state retaining only position, biome, and light from the real block. */
final class VirtualBlockAccess implements BlockAccess {

    private final BlockAccess delegate;
    private final BlockState displayState;
    private final int originX;
    private final int originY;
    private final int originZ;
    private int x;
    private int y;
    private int z;

    VirtualBlockAccess(BlockAccess delegate, BlockState displayState) {
        this(
                delegate.copy(),
                displayState,
                delegate.getX(),
                delegate.getY(),
                delegate.getZ()
        );
    }

    private VirtualBlockAccess(
            BlockAccess delegate,
            BlockState displayState,
            int originX,
            int originY,
            int originZ
    ) {
        this.delegate = delegate;
        this.displayState = displayState;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        set(originX, originY, originZ);
    }

    @Override
    public void set(int blockX, int blockY, int blockZ) {
        x = blockX;
        y = blockY;
        z = blockZ;
        delegate.set(blockX, blockY, blockZ);
    }

    @Override
    public BlockAccess copy() {
        return new VirtualBlockAccess(delegate.copy(), displayState, originX, originY, originZ);
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
    public BlockState getBlockState() {
        return x == originX && y == originY && z == originZ ? displayState : BlockState.AIR;
    }

    @Override
    public LightData getLightData() {
        return delegate.getLightData();
    }

    @Override
    public Biome getBiome() {
        return delegate.getBiome();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity() {
        return null;
    }

    @Override
    public boolean hasOceanFloorY() {
        return delegate.hasOceanFloorY();
    }

    @Override
    public int getOceanFloorY() {
        return delegate.getOceanFloorY();
    }
}
