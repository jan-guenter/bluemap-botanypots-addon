/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import io.github.janguenter.bluemap.botanypots.activation.BotanyPotsRuntime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/** BlueMap 5.22 internal ABI boundary. */
public final class BlueMap522Adapter {

    private static final BotanyPotsRuntime RUNTIME = BotanyPotsRuntime.INSTANCE;
    static final Key RENDERER_KEY = Key.parse("bluemap_botanypots:botany_pot_overlay");
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            RENDERER_KEY,
            (pack, gallery, settings) -> new BotanyPotsRenderer(pack, gallery, settings, RUNTIME)
    );
    private static final ResourcePack.Extension<BotanyPotsResourceExtension> EXTENSION =
            new BotanyPotsResourceExtensionType(RUNTIME);
    private static final BlockEntityType BLOCK_ENTITY = new BlockEntityType.Impl(
            Key.parse("botanypots:botany_pot"),
            BotanyPotBlockEntityData.class
    );

    private BlueMap522Adapter() {
    }

    public static synchronized boolean install() {
        if (!canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)
                || !canRegister(BlockEntityType.REGISTRY, BLOCK_ENTITY)) {
            RUNTIME.disableAll("registry-collision");
            return false;
        }
        if (!register(BlockRendererType.REGISTRY, RENDERER)
                || !register(ResourcePack.Extension.REGISTRY, EXTENSION)
                || !register(BlockEntityType.REGISTRY, BLOCK_ENTITY)) {
            RUNTIME.disableAll("registry-collision");
            return false;
        }
        return true;
    }

    static boolean isExpectedDispatch(Variant variant) {
        return variant != null
                && variant.getRenderer() == RENDERER
                && ResourcePack.MISSING_BLOCK_MODEL.equals(variant.getModel())
                && !variant.isTransformed()
                && !variant.isUvlock()
                && Double.compare(variant.getWeight(), 1D) == 0;
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

    private static <T extends Keyed> boolean canRegister(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        return existing == null || existing == candidate;
    }

    private static <T extends Keyed> boolean register(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        if (existing == null) {
            registry.register(candidate);
            existing = registry.get(candidate.getKey());
        }
        return existing == candidate;
    }
}
