/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Validated, normalized Minecraft resource identifier without game linkage. */
public record ResourceId(String value) implements Comparable<ResourceId> {

    private static final Pattern VALID = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9/._-]+"
    );

    public ResourceId {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (value.length() > 256 || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid resource identifier");
        }
    }

    public static ResourceId parse(String value) {
        return new ResourceId(value);
    }

    @Override
    public int compareTo(ResourceId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
