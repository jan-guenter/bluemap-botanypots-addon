/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.RegistryGuard;
import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.ResourceExtensionType;
import io.github.janguenter.bluemap.botanypots.activation.BotanyPotsRuntime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/** Exact BlueMap 5.23 feature-backport internal ABI boundary. */
public final class BlueMap523Adapter {

    private static final BotanyPotsRuntime RUNTIME = BotanyPotsRuntime.INSTANCE;
    static final Key RENDERER_KEY = Key.parse("bluemap_botanypots:botany_pot_overlay");
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            RENDERER_KEY,
            (pack, gallery, settings) -> new BotanyPotsRenderer(pack, gallery, settings, RUNTIME)
    );
    private static final ResourcePack.Extension<BotanyPotsResourceExtension> EXTENSION =
            new ResourceExtensionType<>(
                    Key.parse("bluemap_botanypots:exact_profile"),
                    pack -> new BotanyPotsResourceExtension(pack, RENDERER, RUNTIME)
            );
    private static final BlockEntityType BLOCK_ENTITY = new BlockEntityType.Impl(
            Key.parse("botanypots:botany_pot"),
            BotanyPotBlockEntityData.class
    );

    private BlueMap523Adapter() {
    }

    public static synchronized boolean install() {
        if (!RegistryGuard.canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !RegistryGuard.canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)
                || !RegistryGuard.canRegister(BlockEntityType.REGISTRY, BLOCK_ENTITY)) {
            RUNTIME.disableAll("registry-collision");
            return false;
        }
        if (!RegistryGuard.register(BlockRendererType.REGISTRY, RENDERER)
                || !RegistryGuard.register(ResourcePack.Extension.REGISTRY, EXTENSION)
                || !RegistryGuard.register(BlockEntityType.REGISTRY, BLOCK_ENTITY)) {
            RUNTIME.disableAll("registry-collision");
            return false;
        }
        return true;
    }

    /**
     * Probes BlueMap's process-wide BlueNBT instance after every add-on has
     * registered. BlueNBT snapshots resolving delegates on first use, so a
     * hot-added add-on after an earlier chunk read cannot be repaired by a
     * BlueMap-only reload and must remain fail-closed until a JVM restart.
     */
    static boolean probeBlockEntityRetention() {
        try {
            BlockEntity parsed = MCAUtil.BLUENBT.read(
                    new ByteArrayInputStream(createProbeNbt()), BlockEntity.class);
            if (!(parsed instanceof BotanyPotBlockEntityData data)
                    || !BLOCK_ENTITY.getKey().equals(data.getId())
                    || data.getX() != 17 || data.getY() != -23 || data.getZ() != 41
                    || data.inventory() == null || !data.inventory().valid()) {
                return false;
            }
            return data.inventory().slot(0)
                    .filter(item -> "minecraft:dirt".equals(item.itemId().value())
                            && item.count() == 1 && !item.hasComponents())
                    .isPresent()
                    && data.inventory().slot(1)
                    .filter(item -> "minecraft:wheat_seeds".equals(item.itemId().value())
                            && item.count() == 1 && !item.hasComponents())
                    .isPresent();
        } catch (IOException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    static ResourcePack.Extension<BotanyPotsResourceExtension> extensionType() {
        return EXTENSION;
    }

    private static byte[] createProbeNbt() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(bytes)) {
            writer.beginCompound();
            writer.name("id").value("botanypots:botany_pot");
            writer.name("x").value(17);
            writer.name("y").value(-23);
            writer.name("z").value(41);
            writer.name("Items").beginList(2, TagType.COMPOUND);
            writeProbeStack(writer, 0, "minecraft:dirt");
            writeProbeStack(writer, 1, "minecraft:wheat_seeds");
            writer.endList();
            writer.endCompound();
        }
        return bytes.toByteArray();
    }

    private static void writeProbeStack(NBTWriter writer, int slot, String item)
            throws IOException {
        writer.beginCompound();
        writer.name("Slot").value((byte) slot);
        writer.name("id").value(item);
        writer.name("count").value(1);
        writer.endCompound();
    }

}
