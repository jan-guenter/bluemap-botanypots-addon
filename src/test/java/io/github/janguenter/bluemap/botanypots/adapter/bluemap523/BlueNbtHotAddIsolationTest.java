/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluenbt.NBTWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs in its own Gradle test JVM; see the dedicated hotAddBlueNbtTest task. */
class BlueNbtHotAddIsolationTest {

    @Test
    void cacheWarmedBeforeRegistrationRejectsHotAddedDto() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(bytes)) {
            writer.beginCompound();
            writer.name("id").value("minecraft:chest");
            writer.name("x").value(0);
            writer.name("y").value(64);
            writer.name("z").value(0);
            writer.endCompound();
        }
        // Snapshot the BlockEntity resolver before the Botany type exists.
        MCAUtil.BLUENBT.read(new ByteArrayInputStream(bytes.toByteArray()), BlockEntity.class);

        assertTrue(BlueMap523Adapter.install());
        assertFalse(BlueMap523Adapter.probeBlockEntityRetention());
    }
}
