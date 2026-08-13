/* SPDX-License-Identifier: LGPL-2.1-only */
package io.github.janguenter.bluemap.botanypots.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactModArtifactDetectorTest {

    @TempDir
    Path temporary;

    @Test
    void genuineAbsenceIsDistinctFromWrongOrDuplicateKnownArtifact() throws IOException {
        ExactModArtifactDetector.Scan absent = ExactModArtifactDetector.scan(List.of());
        assertTrue(absent.absent("botanypots"));
        assertFalse(absent.defective("botanypots"));

        Path wrong = fakeMod("wrong.jar", "botanypots");
        ExactModArtifactDetector.Scan mismatch = ExactModArtifactDetector.scan(List.of(wrong));
        assertFalse(mismatch.absent("botanypots"));
        assertTrue(mismatch.defective("botanypots"));

        Path duplicate = fakeMod("duplicate.jar", "botanypots");
        ExactModArtifactDetector.Scan repeated = ExactModArtifactDetector.scan(
                List.of(wrong, duplicate));
        assertFalse(repeated.absent("botanypots"));
        assertTrue(repeated.defective("botanypots"));
    }

    private Path fakeMod(String name, String modId) throws IOException {
        Path archive = temporary.resolve(name);
        try (ZipOutputStream output = new ZipOutputStream(
                java.nio.file.Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            output.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            output.write(("[[mods]]\nmodId=\"" + modId + "\"\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return archive;
    }
}
