/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.activation;

import io.github.janguenter.bluemap.botanypots.catalog.NormalizedCatalog;
import io.github.janguenter.bluemap.botanypots.profile.RouteCatalog;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Process-scoped activation and immutable catalog state for exact internal profiles. */
public final class BotanyPotsRuntime {

    public static final String CORE = "core";
    public static final Set<String> PROFILE_IDS = Set.of(CORE);
    public static final BotanyPotsRuntime INSTANCE = new BotanyPotsRuntime();

    private final Map<String, RouteActivation> routes;
    private volatile NormalizedCatalog catalog;
    private volatile RouteCatalog ownedBlocks;

    private BotanyPotsRuntime() {
        TreeMap<String, RouteActivation> configured = new TreeMap<>();
        PROFILE_IDS.forEach(id -> configured.put(id, new RouteActivation(id)));
        routes = Map.copyOf(configured);
    }

    public RouteActivation route(String profile) {
        RouteActivation result = routes.get(profile);
        if (result == null) {
            throw new IllegalArgumentException("unknown internal profile");
        }
        return result;
    }

    public NormalizedCatalog catalog() {
        return catalog;
    }

    public RouteCatalog ownedBlocks() {
        return ownedBlocks;
    }

    public synchronized void installCatalog(
            NormalizedCatalog installed,
            RouteCatalog routesCatalog
    ) {
        catalog = Objects.requireNonNull(installed, "installed");
        ownedBlocks = Objects.requireNonNull(routesCatalog, "routesCatalog");
    }

    public synchronized void clearCatalog() {
        catalog = null;
        ownedBlocks = null;
    }

    public void disableAll(String detail) {
        clearCatalog();
        routes.values().forEach(route -> route.fail(detail));
    }
}
