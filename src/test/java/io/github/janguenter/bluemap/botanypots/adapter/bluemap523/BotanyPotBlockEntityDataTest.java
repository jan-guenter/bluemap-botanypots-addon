/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import io.github.janguenter.bluemap.botanypots.model.BotanyInventoryProjection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotanyPotBlockEntityDataTest {

    @Test
    void realBlueNbtDtoRetainsSlotsZeroAndOne() throws Exception {
        BotanyPotBlockEntityData data = read(blockEntity(writer -> {
            writer.name("Items").beginList(3, TagType.COMPOUND);
            stack(writer, 0, "minecraft:dirt", 1, false, false);
            stack(writer, 1, "minecraft:wheat_seeds", 1, false, false);
            stack(writer, 14, "not even a resource id", 50_000, true, false);
            writer.endList();
        }));

        BotanyInventoryProjection inventory = data.inventory();
        assertTrue(inventory.valid());
        assertEquals("minecraft:dirt", inventory.slot(0).orElseThrow().itemId().value());
        assertEquals("minecraft:wheat_seeds",
                inventory.slot(1).orElseThrow().itemId().value());
        assertTrue(inventory.slot(14).isEmpty());
    }

    @Test
    void componentBearingVisualStackIsRetainedAsOpaquePresence() throws Exception {
        BotanyPotBlockEntityData data = read(blockEntity(writer -> {
            writer.name("Items").beginList(1, TagType.COMPOUND);
            stack(writer, 0, "minecraft:dirt", 1, false, true);
            writer.endList();
        }));

        assertTrue(data.inventory().slot(0).orElseThrow().hasComponents());
    }

    @Test
    void duplicateVisualSlotAndOversizedListFailClosed() throws Exception {
        byte[] duplicate = blockEntity(writer -> {
            writer.name("Items").beginList(2, TagType.COMPOUND);
            stack(writer, 0, "minecraft:dirt", 1, false, false);
            stack(writer, 0, "minecraft:stone", 1, false, false);
            writer.endList();
        });
        assertThrows(IOException.class, () -> read(duplicate));

        byte[] oversized = blockEntity(writer -> {
            writer.name("Items").beginList(16, TagType.COMPOUND);
            for (int slot = 0; slot < 16; slot++) {
                stack(writer, slot % 15, "minecraft:stone", 1, false, false);
            }
            writer.endList();
        });
        assertThrows(IOException.class, () -> read(oversized));
    }

    @Test
    void sharedBlueNbtRetainsRegisteredBotanyDtoOnColdInstall() {
        assertTrue(BlueMap523Adapter.install());
        assertTrue(BlueMap523Adapter.probeBlockEntityRetention());
    }

    private static BotanyPotBlockEntityData read(byte[] nbt) throws IOException {
        return MCAUtil.addCommonNbtSettings(new BlueNBT()).read(
                new ByteArrayInputStream(nbt), BotanyPotBlockEntityData.class);
    }

    private static byte[] blockEntity(WriterAction body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(bytes)) {
            writer.beginCompound();
            writer.name("id").value("botanypots:botany_pot");
            writer.name("x").value(3);
            writer.name("y").value(64);
            writer.name("z").value(5);
            body.write(writer);
            writer.endCompound();
        }
        return bytes.toByteArray();
    }

    private static void stack(
            NBTWriter writer,
            int slot,
            String id,
            int count,
            boolean unknown,
            boolean components
    ) throws IOException {
        writer.beginCompound();
        writer.name("Slot").value((byte) slot);
        writer.name("id").value(id);
        writer.name("count").value(count);
        if (unknown) {
            writer.name("opaque_extension").beginCompound();
            writer.name("arbitrary").value("ignored for output slots");
            writer.endCompound();
        }
        if (components) {
            writer.name("components").beginCompound();
            writer.name("minecraft:custom_name").value("opaque");
            writer.endCompound();
        }
        writer.endCompound();
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(NBTWriter writer) throws IOException;
    }
}
