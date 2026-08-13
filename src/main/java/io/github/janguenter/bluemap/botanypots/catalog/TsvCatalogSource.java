/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode.Axis;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode.Face;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode.Rotation;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode.Strategy;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode.Vector3;
import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

/** Strict parser for the canonical bundled asset-free display table. */
public final class TsvCatalogSource implements CatalogSource {

    public static final String HEADER = "kind\trecipe_id\tprofile\tsource\tderivation\tinput_items"
            + "\taccepted_soils\tprogram_type\tdisplays";

    @Override
    public NormalizedCatalog read(InputStream input) throws IOException {
        List<CatalogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IOException("catalog header mismatch");
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.length() > 1_000_000 || entries.size() >= 2_048) {
                    throw new IOException("invalid catalog line " + lineNumber);
                }
                try {
                    entries.add(parseEntry(line));
                } catch (IllegalArgumentException exception) {
                    throw new IOException("invalid catalog line " + lineNumber, exception);
                }
            }
        }
        try {
            return new NormalizedCatalog(entries);
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid catalog indexes", exception);
        }
    }

    private static CatalogEntry parseEntry(String line) {
        String[] fields = line.split("\\t", -1);
        if (fields.length != 9) {
            throw new IllegalArgumentException("wrong field count");
        }
        return new CatalogEntry(
                CatalogEntry.Kind.valueOf(fields[0].toUpperCase(Locale.ROOT)),
                ResourceId.parse(fields[1]),
                fields[2],
                fields[3],
                fields[4],
                ids(fields[5], false),
                ids(fields[6], true),
                new DisplayProgram(
                        DisplayProgram.Kind.valueOf(fields[7].toUpperCase(Locale.ROOT)),
                        displays(fields[8])
                )
        );
    }

    private static Set<ResourceId> ids(String encoded, boolean emptyAllowed) {
        if (encoded.isEmpty()) {
            if (emptyAllowed) {
                return Set.of();
            }
            throw new IllegalArgumentException("required item set is empty");
        }
        LinkedHashSet<ResourceId> result = new LinkedHashSet<>();
        for (String token : encoded.split(",", -1)) {
            if (!result.add(ResourceId.parse(token))) {
                throw new IllegalArgumentException("duplicate item identifier");
            }
        }
        return Set.copyOf(result);
    }

    private static List<DisplayNode> displays(String encoded) {
        if (encoded.isEmpty()) {
            throw new IllegalArgumentException("display sequence is empty");
        }
        return Arrays.stream(encoded.split("\\|\\|", -1))
                .map(TsvCatalogSource::display)
                .toList();
    }

    private static DisplayNode display(String encoded) {
        String[] fields = encoded.split("~", -1);
        if (fields.length != 9) {
            throw new IllegalArgumentException("wrong display field count");
        }
        return new DisplayNode(
                fields[0],
                "-".equals(fields[1]) ? null : ResourceId.parse(fields[1]),
                fluid(fields[2]),
                tint(fields[3]),
                faces(fields[4]),
                vector(fields[5]),
                vector(fields[6]),
                rotations(fields[7]),
                Strategy.valueOf(fields[8].toUpperCase(Locale.ROOT))
        );
    }

    private static DisplayNode.Fluid fluid(String encoded) {
        if ("-".equals(encoded)) {
            return null;
        }
        String[] fields = encoded.split("@", -1);
        if (fields.length != 3) {
            throw new IllegalArgumentException("invalid fluid material");
        }
        return new DisplayNode.Fluid(
                ResourceId.parse(fields[0]),
                ResourceId.parse(fields[1]),
                tintRule(fields[2])
        );
    }

    private static DisplayNode.Tint.Rule tintRule(String encoded) {
        String[] fields = encoded.split(":", -1);
        DisplayNode.Tint.Kind kind = DisplayNode.Tint.Kind.valueOf(
                fields[0].toUpperCase(Locale.ROOT));
        if (kind == DisplayNode.Tint.Kind.FIXED) {
            if (fields.length != 2 || fields[1].length() != 8) {
                throw new IllegalArgumentException("invalid fixed material tint");
            }
            return new DisplayNode.Tint.Rule(
                    kind, (int) Long.parseUnsignedLong(fields[1], 16));
        }
        if (fields.length > 2 || fields.length == 2 && fields[1].length() != 8) {
            throw new IllegalArgumentException("invalid calculated material tint");
        }
        return new DisplayNode.Tint.Rule(
                kind,
                fields.length == 2
                        ? (int) Long.parseUnsignedLong(fields[1], 16)
                        : 0xFFFFFFFF
        );
    }

    private static DisplayNode.Tint tint(String encoded) {
        if ("-".equals(encoded)) {
            return null;
        }
        if (encoded.startsWith("all=")) {
            String value = encoded.substring(4);
            if (value.length() != 8) {
                throw new IllegalArgumentException("invalid all-face tint");
            }
            return new DisplayNode.Tint(
                    (int) Long.parseUnsignedLong(value, 16), Map.of());
        }
        Map<Integer, DisplayNode.Tint.Rule> rules = new TreeMap<>();
        for (String token : encoded.split(",", -1)) {
            String[] assignment = token.split("=", -1);
            if (assignment.length != 2) {
                throw new IllegalArgumentException("invalid indexed tint");
            }
            int index = Integer.parseInt(assignment[0]);
            String[] rule = assignment[1].split(":", -1);
            if (rules.put(index, tintRule(assignment[1])) != null) {
                throw new IllegalArgumentException("duplicate tint index");
            }
        }
        return new DisplayNode.Tint(null, rules);
    }

    private static Set<Face> faces(String encoded) {
        if ("all".equals(encoded)) {
            return EnumSet.allOf(Face.class);
        }
        EnumSet<Face> result = EnumSet.noneOf(Face.class);
        for (String token : encoded.split("/", -1)) {
            if (!result.add(Face.valueOf(token.toUpperCase(Locale.ROOT)))) {
                throw new IllegalArgumentException("duplicate display face");
            }
        }
        return result;
    }

    private static Vector3 vector(String encoded) {
        String[] values = encoded.split(",", -1);
        if (values.length != 3) {
            throw new IllegalArgumentException("invalid vector");
        }
        return new Vector3(
                Double.parseDouble(values[0]),
                Double.parseDouble(values[1]),
                Double.parseDouble(values[2])
        );
    }

    private static List<Rotation> rotations(String encoded) {
        if ("-".equals(encoded)) {
            return List.of();
        }
        return Arrays.stream(encoded.split("/", -1)).map(token -> {
            String[] values = token.split(":", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException("invalid rotation");
            }
            return new Rotation(
                    Axis.valueOf(values[0].toUpperCase(Locale.ROOT)),
                    Double.parseDouble(values[1])
            );
        }).toList();
    }
}
