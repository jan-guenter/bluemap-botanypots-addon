/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.NBTDeserializer;
import de.bluecolored.bluenbt.NBTName;
import io.github.janguenter.bluemap.botanypots.model.BotanyInventoryProjection;

/** BlueNBT DTO retaining only the bounded standard inventory projection. */
public final class BotanyPotBlockEntityData extends MCABlockEntity {

    @NBTName("Items")
    @NBTDeserializer(BotanyItemsDeserializer.class)
    private BotanyInventoryProjection items;

    public BotanyPotBlockEntityData() {
    }

    public BotanyInventoryProjection inventory() {
        return items;
    }
}
