/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePackExtension;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.botanypots.activation.BotanyPotsRuntime;
import io.github.janguenter.bluemap.botanypots.catalog.NormalizedCatalog;
import io.github.janguenter.bluemap.botanypots.catalog.TsvCatalogSource;
import io.github.janguenter.bluemap.botanypots.profile.ExactModArtifactDetector;
import io.github.janguenter.bluemap.botanypots.profile.ProfileDisablement;
import io.github.janguenter.bluemap.botanypots.profile.RouteCatalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Exact artifact/catalog activation and 183-ID allowlist routing. */
final class BotanyPotsResourceExtension implements ResourcePackExtension {

    private static final Key SYNTHETIC = Key.parse("bluemap_botanypots:botany_pot_overlay");
    private static final String CATALOG_RESOURCE =
            "/bluemap-botanypots/profiles/atmons-1.2.0/catalog.tsv";

    private final ResourcePack resourcePack;
    private final BotanyPotsRuntime runtime;
    private RouteCatalog routes;

    BotanyPotsResourceExtension(ResourcePack resourcePack, BotanyPotsRuntime runtime) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
    }

    @Override
    public void loadResources(Iterable<Path> roots) throws IOException, InterruptedException {
        runtime.clearCatalog();
        routes = null;
        List<Path> rootList = copyRoots(roots);
        ProfileDisablement disabled = ProfileDisablement.current();
        ExactModArtifactDetector.Scan artifacts = ExactModArtifactDetector.scan(rootList);
        if (!artifacts.valid() || !artifacts.exact("botanypots")) {
            inactiveAll("exact-core-missing");
            return;
        }
        if (disabled.isDisabled(BotanyPotsRuntime.CORE)) {
            inactiveAll("operator-disabled");
            return;
        }
        if (!BlueMap522Adapter.probeBlockEntityRetention()) {
            runtime.disableAll("bluenbt-retention-probe-failed");
            return;
        }

        NormalizedCatalog complete = readCatalog();
        RouteCatalog loadedRoutes = RouteCatalog.load();
        routes = loadedRoutes;
        if (complete.entries().isEmpty()
                || !validDispatch(resourcePack.getBlockStates().get(SYNTHETIC))) {
            inactiveAll("catalog-or-dispatch-invalid");
            return;
        }
        runtime.route(BotanyPotsRuntime.CORE).activate();
        runtime.installCatalog(complete, loadedRoutes);
    }

    @Override
    public Set<Key> collectUsedTextureKeys() {
        return Set.of();
    }

    @Override
    public void bake() {
        NormalizedCatalog catalog = runtime.catalog();
        if (catalog == null || routes == null) {
            return;
        }
        CatalogResourcePreflight.Result result = CatalogResourcePreflight.validate(
                resourcePack, routes, catalog);
        if (!result.stockShellsValid()) {
            inactiveAll("stock-shell-resource-invalid");
            return;
        }
        if (result.invalidProfiles().contains(BotanyPotsRuntime.CORE)) {
            inactiveAll("required-display-resource-invalid");
        }
    }

    @Override
    public Key getBlockStateKey(Key key) {
        RouteCatalog routes = runtime.ownedBlocks();
        return runtime.route(BotanyPotsRuntime.CORE).isActive()
                && routes != null
                && routes.contains(key.getFormatted()) ? SYNTHETIC : key;
    }

    @Override
    public void getBlockProperties(BlockState state, BlockProperties.Builder builder) {
        RouteCatalog routes = runtime.ownedBlocks();
        if (runtime.route(BotanyPotsRuntime.CORE).isActive()
                && routes != null && routes.contains(state.getId().getFormatted())) {
            builder.culling(false)
                    .occluding(false)
                    .cullingIdentical(false)
                    .randomOffset(false);
        }
    }

    private NormalizedCatalog readCatalog() throws IOException {
        try (InputStream input = BotanyPotsResourceExtension.class.getResourceAsStream(
                CATALOG_RESOURCE)) {
            if (input == null) {
                throw new IOException("normalized catalog resource missing");
            }
            byte[] bytes = input.readNBytes(32 * 1024 * 1024 + 1);
            if (bytes.length > 32 * 1024 * 1024) {
                throw new IOException("normalized catalog outside supported bounds");
            }
            return new TsvCatalogSource().read(new ByteArrayInputStream(bytes));
        }
    }

    private static List<Path> copyRoots(Iterable<Path> roots) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path root : roots) {
            if (result.size() >= 4_096) {
                throw new IOException("resource root count outside supported bounds");
            }
            result.add(root);
        }
        return List.copyOf(result);
    }

    private void inactiveAll(String reason) {
        runtime.clearCatalog();
        BotanyPotsRuntime.PROFILE_IDS.forEach(profile -> runtime.route(profile).inactive(reason));
    }

    private static boolean validDispatch(
            de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState state
    ) {
        if (state == null || state.getMultipart() != null) {
            return false;
        }
        Variants variants = state.getVariants();
        if (variants == null || variants.getDefaultVariant() == null) {
            return false;
        }
        VariantSet set = variants.getDefaultVariant();
        return set.getVariants().length == 1
                && BlueMap522Adapter.isExpectedDispatch(set.getVariants()[0]);
    }

}
