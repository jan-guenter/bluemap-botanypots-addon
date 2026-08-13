/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Bounded exact-byte scanner for known NeoForge mod artifacts. */
public final class ExactModArtifactDetector {

    private static final int MAX_ROOTS = 4_096;
    private static final int MAX_DESCRIPTOR_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final String MOD_DESCRIPTOR = "META-INF/neoforge.mods.toml";
    private static final Pattern MOD_ID = Pattern.compile(
            "^(?:modId|\\\"modId\\\"|'modId')\\s*=\\s*"
                    + "(?:\\\"([a-z0-9_.-]+)\\\"|'([a-z0-9_.-]+)')$"
    );

    private ExactModArtifactDetector() {
    }

    public static Scan scan(Iterable<Path> roots) {
        Map<String, Path> candidates = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        Set<Path> inspected = new HashSet<>();
        int rootCount = 0;
        for (Path root : roots) {
            if (Thread.currentThread().isInterrupted() || ++rootCount > MAX_ROOTS) {
                return Scan.incomplete();
            }
            if (root == null || !Files.isRegularFile(root)
                    || !root.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                continue;
            }
            try {
                Path real = root.toRealPath();
                if (!inspected.add(real)) {
                    continue;
                }
                for (String modId : declaredKnownMods(real)) {
                    if (candidates.putIfAbsent(modId, real) != null) {
                        duplicates.add(modId);
                    }
                }
            } catch (IOException ignored) {
                // A broken unrelated/optional root must not globally disable an
                // independently exact core. An unreadable core remains absent.
            }
        }
        Set<String> matches = new HashSet<>();
        Set<String> mismatches = new HashSet<>();
        Map<String, Path> matchedPaths = new HashMap<>();
        for (Map.Entry<String, Path> candidate : candidates.entrySet()) {
            if (duplicates.contains(candidate.getKey())) {
                continue;
            }
            try {
                ExactArtifacts.Identity expected = ExactArtifacts.RUNTIME.get(candidate.getKey());
                if (Files.size(candidate.getValue()) == expected.size()
                        && expected.sha256().equals(digest(candidate.getValue()))) {
                    matches.add(candidate.getKey());
                    matchedPaths.put(candidate.getKey(), candidate.getValue());
                } else {
                    mismatches.add(candidate.getKey());
                }
            } catch (IOException exception) {
                mismatches.add(candidate.getKey());
            }
        }
        return new Scan(
                true,
                Set.copyOf(matches),
                Set.copyOf(mismatches),
                Set.copyOf(duplicates),
                Map.copyOf(matchedPaths)
        );
    }

    private static Set<String> declaredKnownMods(Path jar) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry descriptor = zip.getEntry(MOD_DESCRIPTOR);
            if (descriptor == null || descriptor.isDirectory()
                    || descriptor.getSize() > MAX_DESCRIPTOR_BYTES) {
                return Set.of();
            }
            byte[] content;
            try (InputStream input = zip.getInputStream(descriptor)) {
                content = input.readNBytes(MAX_DESCRIPTOR_BYTES + 1);
            }
            if (content.length > MAX_DESCRIPTOR_BYTES) {
                throw new IOException("mod descriptor exceeds bound");
            }
            Set<String> declared = new HashSet<>();
            boolean inModsTable = false;
            for (String line : new String(content, StandardCharsets.UTF_8).split("\\R", -1)) {
                int comment = line.indexOf('#');
                String statement = (comment < 0 ? line : line.substring(0, comment)).trim();
                if (statement.startsWith("[")) {
                    inModsTable = statement.equals("[[mods]]")
                            || statement.equals("[[\"mods\"]]")
                            || statement.equals("[['mods']]");
                } else if (inModsTable) {
                    Matcher matcher = MOD_ID.matcher(statement);
                    if (matcher.matches()) {
                        String modId = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
                        if (ExactArtifacts.RUNTIME.containsKey(modId)) {
                            declared.add(modId);
                        }
                    }
                }
            }
            return declared;
        }
    }

    private static String digest(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public record Scan(
            boolean valid,
            Set<String> matches,
            Set<String> mismatches,
            Set<String> duplicates,
            Map<String, Path> matchedPaths
    ) {

        private static Scan incomplete() {
            return new Scan(false, Set.of(), Set.of(), Set.of(), Map.of());
        }

        public boolean exact(String modId) {
            return valid && matches.contains(modId)
                    && !mismatches.contains(modId) && !duplicates.contains(modId);
        }

        public Path exactPath(String modId) {
            return exact(modId) ? matchedPaths.get(modId) : null;
        }

        public boolean absent(String modId) {
            return valid && ExactArtifacts.RUNTIME.containsKey(modId)
                    && !matches.contains(modId) && !mismatches.contains(modId)
                    && !duplicates.contains(modId);
        }

        public boolean defective(String modId) {
            return valid && (mismatches.contains(modId) || duplicates.contains(modId));
        }
    }
}
