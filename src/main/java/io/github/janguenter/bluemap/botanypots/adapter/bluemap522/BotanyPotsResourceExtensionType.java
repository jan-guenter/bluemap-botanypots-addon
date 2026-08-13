/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.botanypots.activation.BotanyPotsRuntime;

/** Resource-pack extension factory registered before resource loading begins. */
final class BotanyPotsResourceExtensionType
        implements ResourcePack.Extension<BotanyPotsResourceExtension> {

    static final Key KEY = Key.parse("bluemap_botanypots:exact_profile");
    private final BotanyPotsRuntime runtime;

    BotanyPotsResourceExtensionType(BotanyPotsRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public BotanyPotsResourceExtension create(ResourcePack pack) {
        return new BotanyPotsResourceExtension(pack, runtime);
    }
}
