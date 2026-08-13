/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.profile;

import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/** Exact 183-block ownership allowlist. */
public final class RouteCatalog {

    public static final int EXPECTED_ROUTES = 183;
    public static final int EXPECTED_LEGAL_STATES = 23_424;
    private static final String RESOURCE =
            "/bluemap-botanypots/profiles/atmons-1.2.0/routes.tsv";

    private final Set<ResourceId> routes;

    private RouteCatalog(Set<ResourceId> routes) {
        this.routes = Set.copyOf(routes);
    }

    public static RouteCatalog load() throws IOException {
        try (InputStream input = RouteCatalog.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("route catalog resource missing");
            }
            return read(input);
        }
    }

    static RouteCatalog read(InputStream input) throws IOException {
        LinkedHashSet<ResourceId> routes = new LinkedHashSet<>();
        int legalStates = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            if (!"block_id\tkind\tlegal_states".equals(reader.readLine())) {
                throw new IOException("route catalog header mismatch");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3
                        || !Set.of("basic", "hopper", "waxed").contains(fields[1])) {
                    throw new IOException("invalid route row");
                }
                ResourceId route;
                int stateCount;
                try {
                    route = ResourceId.parse(fields[0]);
                    stateCount = Integer.parseInt(fields[2]);
                } catch (IllegalArgumentException exception) {
                    throw new IOException("invalid route row", exception);
                }
                if (!route.value().startsWith("botanypots:")
                        || !route.value().endsWith("_botany_pot")
                        || stateCount != 128
                        || !routes.add(route)) {
                    throw new IOException("route ownership mismatch");
                }
                legalStates += stateCount;
            }
        }
        if (routes.size() != EXPECTED_ROUTES || legalStates != EXPECTED_LEGAL_STATES) {
            throw new IOException("route census mismatch");
        }
        return new RouteCatalog(routes);
    }

    public boolean contains(String blockId) {
        try {
            return routes.contains(ResourceId.parse(blockId));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public Set<ResourceId> routes() {
        return routes;
    }
}
