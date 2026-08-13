/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.profile;

import java.util.Map;
import java.util.Set;

/** Evidence-locked runtime artifact identity for the representative tranche. */
public final class ExactArtifacts {

    public static final Map<String, Identity> RUNTIME = Map.of(
            "botanypots",
            new Identity(1_068_816L,
                    "45b23ac195511f724f62ab5f0c2d7a1c2c2403ff324a7403a1142e28a7d65edd")
    );

    public static final Map<String, String> FILE_NAMES = Map.of(
            "botanypots", "botanypots-neoforge-1.21.1-21.1.44.jar"
    );

    public static final Set<String> CORE_PROFILE = Set.of("botanypots");

    private ExactArtifacts() {
    }

    public record Identity(long size, String sha256) {
    }
}
