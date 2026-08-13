/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import java.util.List;
import java.util.Objects;

/** Static-mature projection retaining the exact upstream display-program family. */
public record DisplayProgram(Kind kind, List<DisplayNode> members) {

    public DisplayProgram {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(members, "members");
        members = List.copyOf(members);
        if (members.isEmpty() || members.size() > 8) {
            throw new IllegalArgumentException("display program outside supported bounds");
        }
    }

    public enum Kind {
        SIMPLE,
        TRANSITIONAL,
        AGING,
        DERIVED
    }
}
